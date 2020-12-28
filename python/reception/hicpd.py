# Test framework to open server port and start recep[tion app.
import socket
import threading

import reception

class HICPd(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

        self.socket = None
        self.port = None
        self.is_stopped = False

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
                (cs, address) = self.socket.accept()
            except ConnectionAbortedError as cae:
                if self.is_stopped:
                    # Normal, no error.
                    break
                else:
                    raise cae

            # start actual reception app.
            f = cs.makefile(mode='rw', encoding='utf-8', newline='\n')

            reception_app = reception.Reception(f, f)
            reception_app.start()

            cs.close()
        self.socket.close()  # No longer neded?

    def stop(self):
        # If waiting for socket connection, interrupt.
        self.is_stopped = True
        self.socket.close()

    def get_port(self):
        return self.port

hicpd = HICPd()
hicpd.start()

while True:
    # TODO: How to wait for socket number to be available?
    socket = hicpd.get_port()
    c = input(f"{socket} x a ?: ")
    if c == "x":
        hicpd.stop()
        print("stopped")
        break
    elif c == "a":
        # Make hicpd rescan apps
        pass
    elif c == "":
        # Just re-print prompt.
        pass
    else:
        print("x: Exit after all apps exit")
        print("a: Rescan apps")
        print("?: help")

hicpd.join()
