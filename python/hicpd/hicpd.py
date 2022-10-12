# Test framework to open server port and start recep[tion app.
import os
import os.path
import socket
import sys
import threading

from hicp import HICP, newLogger, Message

class Authenticator:
    "A simple authenticator, uses plain text file with 'user, password' lines"

    def __init__(self, user_password_path):
        self.__user_password = {}
        self.__logger = newLogger(type(self).__name__)

        user_password_file = open(user_password_path, "r")
        for user_password_line in user_password_file:
            # split on comma
            user_password_split = user_password_line.find(",")
            # Ignore lines in wrong format.
            if 0 < user_password_split:
                user = user_password_line[0:user_password_split].strip()
                password = user_password_line[user_password_split + 1:].strip()
                self.__user_password[user] = password
            else:
                self.__logger.error("Wrong format in file: " + user_password_line)

    def authenticate(self, message):
        method = message.get_header(Message.METHOD)
        if method is None:
            # No authentication method, fails.
            return False

        if method != Message.PLAIN:
            # This only handles plain passwords.
            return False

        check_user = message.get_header(Message.USER)
        check_password = message.get_header(Message.PASSWORD)

        try:
            if check_password == self.__user_password[check_user]:
                return True
            else:
                return False
        except KeyError:
            # No such user
            return False

    def get_methods(self):
        return ["plain"]


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

            # Make an authenticator.
            authenticator = Authenticator(os.path.join(sys.path[0], "users"))

            # Make an HICP object.
            hicp = HICP(
                in_stream=f,
                out_stream=f,
                authenticator=authenticator)

            print("about to start HICP")
            hicp.start()
            print("done HICP")

            cs.close()
        # There is a possibility that the loop will exit before the socket
        # is closed. Not important since this will exit anyway, but clean up # properly anyway.
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
