import os
import os.path
import socket
import queue
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
        done_queue,
        text_group=None,
        text_subgroup=None):

        # These must be specified for this to work.
        if io_socket is None:
            raise UnboundLocalError("io_socket required, not defined")

        self.io_socket = io_socket
        self._done_queue = done_queue
        self.text_group = text_group
        self.text_subgroup = text_subgroup

        threading.Thread.__init__(self)

    def run(self):
        hicp = HICP(io_socket=self.io_socket)
        hicp.start()

        # Tell joiner to wait for this thread. It will try to wait in order,
        # even if a thread started later exits sooner, but that's okay, it just
        # exists to make sure things are cleaned up eventually. It's done this
        # way to ensure the server waits for all processes before it exists,
        # otherwise a server exit will cause the join thread to exit with
        # unjoined threads.
        # Note there is a minor race condition here that's too unlikely for me
        # to fix.
        self._done_queue.put(self)

        # Close socket here, process will keep it open.
        self.io_socket.close()

        hicp.join()
        # TODO: Log any anomolies (process exit not 0).
        


class HICPd_starter(threading.Thread):
    """Open server port, and start HICP instance per connection.
    """
    def __init__(self, done_queue):
        self._done_queue = done_queue

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
            hicp = HICP_thread(io_socket, self._done_queue)
            hicp.start()

        # There is a possibility that the loop will exit before the socket
        # is closed. Not important since this will exit anyway, but clean up
        # properly anyway.
        self.socket.close()

    def stop(self):
        # If waiting for socket connection, interrupt.
        self.is_stopped = True
        self.socket.close()

        # Indicate this thread is stopped to join thread. That thread does not
        # try to join this, but will exit when it sees this has stopped.
        self._done_queue.put(self)

    def get_port(self):
        return self.port


class HICPd_joiner(threading.Thread):
    "Join thread to clean up."
    def __init__(self, done_queue):
        self._done_queue = done_queue

        threading.Thread.__init__(self)

    def run(self):
        while True:
            t = self._done_queue.get()
            if type(t) is HICPd_starter:
                # End of threads to join.
                return
            t.join()


if __name__ == "__main__":
    done_queue = queue.Queue()

    hicpd_starter = HICPd_starter(done_queue)
    hicpd_starter.start()

    hicpd_joiner = HICPd_joiner(done_queue)
    hicpd_joiner.start()

    while True:
        # TODO: How to wait for socket number to be available?
        socket = hicpd_starter.get_port()
        c = input(f"{socket} x ?: ")
        if c == "x":
            hicpd_starter.stop()
            print("stopped")
            break
        elif c == "":
            # Just re-print prompt.
            pass
        else:
            print("x: Exit after all apps exit")
            print("?: help")

    hicpd_starter.join()
    hicpd_joiner.join()
