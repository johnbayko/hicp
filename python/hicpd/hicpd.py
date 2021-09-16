# Test framework to open server port and start recep[tion app.
import importlib
import inspect
import os
import os.path
import pathlib
import pkgutil
import socket
import sys
import threading

from hicp import HICP, newLogger, Message
from hicp import AppSpec, App

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
        self.default_app = None
        self.app_list = {}
        self.is_stopped = False

    def run(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.bind(('', 0))
        addr = self.socket.getsockname()
        self.port = addr[1]
        print(f"Server socket: {addr[1]}") # debug

        self.find_apps()

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
                app_list=self.app_list,
                default_app=self.default_app,
                authenticator=authenticator)

            print("about to start HICP")
            hicp.start()
            print("done HICP")

            cs.close()
        # There is a possibility that the loop will exit before the socket
        # is closed. Not important since this will exit anyway, but clean up
        # properly anyway.
        self.socket.close()

    def stop(self):
        # If waiting for socket connection, interrupt.
        self.is_stopped = True
        self.socket.close()

    def __get_app_path(self):
        hicp_path = os.getenv('HICPPATH', default='.')
        app_path = os.path.join(hicp_path, 'apps')
        return app_path

    def __get_default_app_path(self):
        hicp_path = os.getenv('HICPPATH', default='.')
        app_path = os.path.join(hicp_path, 'default_app')
        return app_path

    def find_apps(self):
        """Find and load apps in the app path.

        If source files are changed, restart the server, it's not worth
        reloading modules and tracking down and killing active apps.
        """
        new_app_list = {}
        new_default_app = None

        app_path = self.__get_app_path()
        app_dirs_list = \
            [os.path.join(app_path, f) for f
                in os.listdir(app_path)
                if os.path.isdir(os.path.join(app_path, f)) ]

        # Add default app dir to beginning of list, if it exists.
        default_app_path = self.__get_default_app_path()
        if os.path.isdir(default_app_path):
            app_dirs_list.insert(0, default_app_path)

        for importer, package_name, _ in pkgutil.iter_modules(app_dirs_list):
            full_package_name = '%s.%s' % (app_path, package_name)
            module_spec = importer.find_spec(package_name)
            module = importlib.util.module_from_spec(module_spec)
            sys.modules[module.__name__] = module
            module_spec.loader.exec_module(module)
            for cls_name, cls in inspect.getmembers(module, inspect.isclass):
                # Filter out imported classes
                if inspect.getmodule(cls) == module:
                    app = None
                    if issubclass(cls, App):
                        module_dirname = os.path.dirname(module_spec.origin)
                        module_path = pathlib.Path(module_dirname)

                        app_name = cls.get_app_name()
                        if module_dirname == default_app_path:
                            new_default_app = app_name

                        new_app_list[app_name] = AppSpec(cls, module_path)

            # Not practical to unload a module with no apps found, just leave
            # it around as garbage.

        self.app_list = new_app_list

        if new_default_app is not None:
            self.default_app = new_default_app
        else:
            # None found, default to first app in list - might not be the same
            # each time, so probably won't work normally.
            # Take one iteration of new_app_list (will iterate keys, which are
            # app names).
            self.default_app = next(iter(new_app_list))

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
