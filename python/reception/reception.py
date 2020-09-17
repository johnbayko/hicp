import logging
import os
import os.path
import sys
import threading

#from logger import Logger
#from hicp.hicp import HICP, Message, Panel, Window, Label, Button, TextField
from hicp import HICP, Message, Panel, Window, Label, Button, TextField

def newLogger(name):
    lf = logging.Formatter('%(name)s:%(funcName)s %(lineno)d: %(message)s')

    lh = logging.FileHandler('reception.log')
    lh.setFormatter(lf)

    logger = logging.getLogger(__name__ + '.' + name)
    logger.addHandler(lh)
    logger.setLevel(logging.DEBUG)

    return logger

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

        if check_password == self.__user_password[check_user]:
            return True
        else:
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


class ButtonHandler:
    def __init__(self, label, next_text_id):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_text_id = next_text_id

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonHandler In update handler")
        self.__label.set_text(self.__next_text_id)
        self.__label.update()


class TextFieldHandler:
    def __init__(self, label, next_text_id):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_text_id = next_text_id

    def update(self, hicp, event_message, component):
        self.logger.debug("TextFieldHandler In update handler")
        self.__label.set_text(self.__next_text_id)
        self.__label.update()

        component.set_content("Woo-hoo!")
        component.update()


class Reception:
    WINDOW_TITLE_ID = 1
    AMAZING_ID = 2
    BUTTON_ID = 3
    LABEL_CLICK_ID = 4
    LABEL_THANKS_ID = 5
    LABEL_CHANGED_ID = 6
    EXTRA_ID = 7  # debug

    def __init__(self, in_stream, out_stream):
        self.in_stream = in_stream
        self.out_stream = out_stream

        self.__logger = newLogger(type(self).__name__)

    def connected(self, hicp):
        self.__logger.debug("Reception connected")
#        hicp.text_direction(hicp.LEFT, hicp.UP) # debug
        hicp.text_direction(hicp.RIGHT, hicp.UP) # debug
        hicp.add_all_text({
            self.WINDOW_TITLE_ID : "Button window",
            self.AMAZING_ID : "Amazing!",
            self.BUTTON_ID : "Button",
            self.LABEL_CLICK_ID : "Please click the button.",
            self.LABEL_THANKS_ID : "Thank you. Don't click the button again.",
            self.LABEL_CHANGED_ID : "Text has been changed.",
            self.EXTRA_ID : "Extra"  # debug
        })
        self.__logger.debug("Reception done add text")

        window = Window()
        window.set_text(self.WINDOW_TITLE_ID)
        window.set_handle_close(ButtonWindowCloser())
        hicp.add(window)
        self.__logger.debug("Reception done add window")

        # TODO: Make amazing panel, add amazing label.
        amazing_panel = Panel()
        window.add(amazing_panel, 0, 0)

        amazing_label = Label()
        amazing_label.set_text(self.AMAZING_ID)
        amazing_panel.add(amazing_label, 0, 0)

# debug
#        amazing_label = Label()
#        amazing_label.set_text(self.AMAZING_ID)
#        amazing_panel.add(amazing_label, 0, 1)

# debug
#        amazing_label = Label()
#        amazing_label.set_text(self.AMAZING_ID)
#        amazing_panel.add(amazing_label, 0, 2)

        click_label = Label()
        click_label.set_text(self.LABEL_CLICK_ID)
        click_label.set_size(1, 1)  # debug
        window.add(click_label, 1, 0)

        button = Button()
        button.set_text(self.BUTTON_ID)
        button.set_size(1, 1)  # debug
        button.set_handle_click(
            ButtonHandler(click_label, self.LABEL_THANKS_ID)
        )
        window.add(button, 1, 1)

#        text_field = TextField()
#        text_field.set_content("This is text")
        # debug - test binary attribute
#        text_field.set_attribute(TextField.UNDERLINE, 5, 2)
        # debug - test value attribute
#        text_field.set_attribute(TextField.SIZE, 8, 1, "2")
#        text_field.set_handle_changed(
#            TextFieldHandler(click_label, self.LABEL_CHANGED_ID)
#        )
#        window.add(text_field, 1, 2)

#        extra_label = Label()  # debug
#        extra_label.set_text(self.EXTRA_ID)  # debug
#        window.add(extra_label, 4, 0)  # debug

        window.set_visible(True)
        self.__logger.debug("About to window.update") # debug
        window.update()
        self.__logger.debug("Done window.update") # debug


# Will eventually do authentication (really?).
#    def authenticate(self):
        # Does nothing yet.
#        pass

    def run(self):
        self.__logger.debug('run()')
        # Make an authenticator
        authenticator = Authenticator(os.path.join(sys.path[0], "users"))

        # Make an HICP object
        self.__logger.debug("about to make HICP")
        hicp = HICP(
            in_stream=self.in_stream,
            out_stream=self.out_stream,
            default_app=self,
            app_list=None,
            authenticator=authenticator)

        self.__logger.debug("about to run HICP")
        hicp.run()
        self.__logger.debug("done run HICP")


# Open a log file and put debug info into it.
#log = Logger(os.path.join(sys.path[0], "reception.log"))
#log.removeLog()
#log.log("Started")

# Make a default app object.
#reception_app = Reception(log)

# Make an HICP object
#log.log("about to make HICP")
#hicp = HICP(
#    in_stream=sys.stdin,
#    out_stream=sys.stdout,
#    logger=log,
#    default_app=reception_app,
#    app_list=None,
#    authenticator=authenticator)

