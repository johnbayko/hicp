import os
import os.path
import socket
import sys
import threading

from hicp import HICP, newLogger, Message

class HICP_thread(threading.Thread):
    """Actual app will be run as a process to manage resources, but there's no
    portable way to signal child termination, so start a thread to join() the
    process, then put itself on a queue for a final join() of the thread.

    Why not have the process signal it's done through a Queue or something? A
    process can terminate unexpectedly, a thread (ignoring bugs) should always
    be able to put itself away cleanly if everything else is still running.
    """
    def __init__(
        self,
        io_socket,
        text_group=None,
        text_subgroup=None):

        # These must be specified for this to work.
        if io_socket is None:
            raise UnboundLocalError("io_socket required, not defined")

        self.io_socket = io_socket
        self.text_group = text_group
        self.text_subgroup = text_subgroup

        threading.Thread.__init__(self)

    def run(self):
        hicp = HICP(io_socket=self.io_socket)

        print("before HICP start")
        hicp.start()

        # Close socket here, process will keep it open.
        self.io_socket.close()

        hicp.join()
        print("after HICP join")
        
        # TODO: Add to queue for final cleanup.


class HICPd(threading.Thread):
    """Open server port, and start HICP instance per connection.
    """
    def __init__(self):
        self.socket = None
        self.port = None
        self.is_stopped = False

        threading.Thread.__init__(self)

    def run(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.bind(('', 0))
        addr = self.socket.getsockname()
        self.port = addr[1]
        print(f"Server socket: {addr[1]}") # debug

        while not self.is_stopped:
            # Wait for socket connect
            try:
                self.socket.listen()
                (io_socket, address) = self.socket.accept()
            except ConnectionAbortedError as cae:
                if self.is_stopped:
                    # Normal, no error.
                    break
                else:
                    raise cae

            # start actual reception app.

            # Make an HICP object.
            hicp = HICP_thread(io_socket)

            print("before HICP thread start")
            hicp.start()
            # TODO: Going to need a separate thread to monitor a queue, and
            # join() any threads it receives to release them.
            # Meanwhile, just join() here.
            hicp.join()
            print("after HICP thread join")

        # There is a possibility that the loop will exit before the socket
        # is closed. Not important since this will exit anyway, but clean up
        # properly anyway.
        self.socket.close()

    def stop(self):
        # If waiting for socket connection, interrupt.
        self.is_stopped = True
        self.socket.close()

    def get_port(self):
        return self.port

if __name__ == "__main__":
    hicpd = HICPd()
    hicpd.start()

    while True:
        # TODO: How to wait for socket number to be available?
        socket = hicpd.get_port()
        c = input(f"{socket} x ?: ")
        if c == "x":
            hicpd.stop()
            print("stopped")
            break
        elif c == "":
            # Just re-print prompt.
            pass
        else:
            print("x: Exit after all apps exit")
            print("?: help")

    hicpd.join()
