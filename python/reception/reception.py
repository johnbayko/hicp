import os
import sys

from hicp import HICP, newLogger, Message, Panel, Window, Label, Button, TextField
from test import TestApp
from testml import TestAppML

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


class ButtonWindowCloser:
    def __init__(self):
        self.logger = newLogger(type(self).__name__)

    def feedback(self, hicp, event_message, component):
        self.logger.debug("ButtonWindowCloser In feedback handler")

    def process(self, event_message, component):
        self.logger.debug("ButtonWindowCloser In process handler")

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonWindowCloser In update handler")

        hicp.remove(component)
        self.logger.debug("ButtonWindowCloser Done remove component")

        # Next, disconnect.
        hicp.disconnect()
        self.logger.debug("ButtonWindowCloser Done disconnect")


# Will eventually do authentication (really?).
#    def authenticate(self):
        # Does nothing yet.
#        pass

class ButtonAppHandler:
    def __init__(self, reception_window, app, hicp):
        self.logger = newLogger(type(self).__name__)
        self.__reception_window = reception_window
        self.__app = app
        self.__hicp = hicp

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonLangHandler In update handler")
        self.__hicp.remove(self.__reception_window)
        self.__app.connected(self.__hicp)

class Reception:
    WINDOW_TITLE_ID = 1
    SELECT_APP_ID = 2
    TEST_APP_ID = 3
    TEST_APP_ML_ID = 4

    APP_NAME_SELF = "self"
    APP_NAME_TEST = "test"
    APP_NAME_TEST_ML = "testml"

    def __init__(self, in_stream, out_stream):
        self.in_stream = in_stream
        self.out_stream = out_stream

        self.__logger = newLogger(type(self).__name__)

        # Make app list.
        self.app_list = {
            self.APP_NAME_SELF: self,
            self.APP_NAME_TEST: TestApp(),
            self.APP_NAME_TEST_ML: TestAppML()
        }
        self.default_app = self.APP_NAME_SELF

    def connected(self, hicp):
        self.__logger.debug("TestAppML connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug
        hicp.add_all_text({
            self.WINDOW_TITLE_ID : "App list",
            self.SELECT_APP_ID : "Select app:",
            self.TEST_APP_ID : "Test",
            self.TEST_APP_ML_ID : "Test Multi-language",
        })

        window = Window()
        window.set_text_id(self.WINDOW_TITLE_ID)
        window.set_handle_close(ButtonWindowCloser())
        hicp.add(window)
        self.__logger.debug("TestAppML done add window")

        select_app_label = Label()
        select_app_label.set_text_id(self.SELECT_APP_ID)
        window.add(select_app_label, 0, 0)

        button_test = Button()
        button_test.set_text_id(self.TEST_APP_ID)
        button_test.set_handle_click(
            ButtonAppHandler(window, self.app_list[self.APP_NAME_TEST], hicp)
        )
        window.add(button_test, 0, 1)

        button_test_ml = Button()
        button_test_ml.set_text_id(self.TEST_APP_ML_ID)
        button_test_ml.set_handle_click(
            ButtonAppHandler(window, self.app_list[self.APP_NAME_TEST_ML], hicp)
        )
        window.add(button_test_ml, 0, 2)

    def start(self):
        self.__logger.debug('start()')

        # Make an authenticator.
        authenticator = Authenticator(os.path.join(sys.path[0], "users"))

        # Make an HICP object.
        self.__logger.debug("about to make HICP")
        hicp = HICP(
            in_stream=self.in_stream,
            out_stream=self.out_stream,
            app_list=self.app_list,
            default_app=self.default_app,
            authenticator=authenticator)

        self.__logger.debug("about to start HICP")
        hicp.start()
        self.__logger.debug("done HICP")

