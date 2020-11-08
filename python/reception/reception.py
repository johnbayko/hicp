import logging
import os
import os.path
import sys
import threading

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


class ButtonHandler:
    def __init__(self, label, next_text_id):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_text_id = next_text_id

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonHandler In update handler")
        self.__label.set_text_id(self.__next_text_id)
        self.__label.update()


class TextFieldHandler:
    def __init__(self, label, next_text_id):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_text_id = next_text_id

    def update(self, hicp, event_message, text_field):
        self.logger.debug("TextFieldHandler In update handler")
        self.logger.debug("content: " + text_field.get_content())  # debug
        self.logger.debug("attributes: " + text_field.get_attribute_string())  # debug
        self.__label.set_text_id(self.__next_text_id)
        self.__label.update()

        text_field.set_content("Woo-hoo!")
        text_field.update()


class TestApp:
    WINDOW_TITLE_ID = 1
    AMAZING_ID = 2
    BUTTON_ID = 3
    LABEL_CLICK_ID = 4
    LABEL_THANKS_ID = 5
    LABEL_CHANGED_ID = 6
#    EXTRA_ID = 7  # debug

    def __init__(self):
        self.__logger = newLogger(type(self).__name__)

    def connected(self, hicp):
        self.__logger.debug("TestApp connected")
        hicp.text_direction(hicp.RIGHT, hicp.UP) # debug
        hicp.add_all_text({
            self.WINDOW_TITLE_ID : "Button window",
            self.AMAZING_ID : "Amazing!",
            self.BUTTON_ID : "Button",
            self.LABEL_CLICK_ID : "Please click the button.",
            self.LABEL_THANKS_ID : "Thank you. Don't click the button again.",
            self.LABEL_CHANGED_ID : "Text has been changed.",
#            self.EXTRA_ID : "Extra"  # debug
        })
        self.__logger.debug("TestApp done add text")

        window = Window()
        window.set_text_id(self.WINDOW_TITLE_ID)
        window.set_handle_close(ButtonWindowCloser())
        hicp.add(window)
        self.__logger.debug("TestApp done add window")

        # TODO: Make amazing panel, add amazing label.
        amazing_panel = Panel()
        window.add(amazing_panel, 0, 0)

        amazing_label = Label()
        amazing_label.set_text_id(self.AMAZING_ID)
        amazing_panel.add(amazing_label, 0, 0)

        click_label = Label()
        click_label.set_text_id(self.LABEL_CLICK_ID)
        click_label.set_size(1, 1)  # debug
        window.add(click_label, 1, 0)

        button = Button()
        button.set_text_id(self.BUTTON_ID)
        button.set_size(1, 1)  # debug
        button.set_handle_click(
            ButtonHandler(click_label, self.LABEL_THANKS_ID)
        )
        window.add(button, 1, 1)

        text_field = TextField()
        text_field.set_content("This is text.")
        # debug - test binary attribute - underline "is"
        # Should be: 5 2 6
        text_field.set_attribute(TextField.UNDERLINE, 5, 2)
        # debug - test value attribute - size of "text"
        # Should be: 8 2=4 1
        text_field.set_attribute(TextField.SIZE, 8, 4, "2")
        text_field.set_handle_changed(
            TextFieldHandler(click_label, self.LABEL_CHANGED_ID)
        )
        window.add(text_field, 1, 2)

#        extra_label = Label()  # debug
#        extra_label.set_text_id(self.EXTRA_ID)  # debug
#        window.add(extra_label, 4, 0)  # debug

        window.set_visible(True)
        self.__logger.debug("About to window.update") # debug
        window.update()
        self.__logger.debug("Done window.update") # debug


# Will eventually do authentication (really?).
#    def authenticate(self):
        # Does nothing yet.
#        pass


class ButtonHandlerML:
    def __init__(self, label, next_group_text, hicp):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_group_text = next_group_text
        self.__hicp = hicp

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonHandler In update handler")
        self.__label.set_groups_text(self.__next_group_text, self.__hicp)
        self.__label.update()


class ButtonLangHandler:
    def __init__(self, lang, hicp):
        self.logger = newLogger(type(self).__name__)
        self.__lang = lang
        self.__hicp = hicp

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonLangHandler In update handler")
        self.__hicp.set_text_group(self.__lang)


class TextFieldHandlerML:
    def __init__(self, label, next_group_text, hicp):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_group_text = next_group_text
        self.__hicp = hicp

    def update(self, hicp, event_message, text_field):
        self.logger.debug("TextFieldHandler In update handler")
        self.logger.debug("content: " + text_field.get_content())  # debug
        self.logger.debug("attributes: " + text_field.get_attribute_string())  # debug
        self.__label.set_groups_text(self.__next_group_text, self.__hicp)
        self.__label.update()

        text_field.set_content("Woo-hoo!")
        text_field.update()


# Test multilingual features.
class TestAppML:
    LANG_EN_CA = "en-ca"
    LANG_FR_CA = "fr-ca"

    def __init__(self):
        self.__logger = newLogger(type(self).__name__)

    def connected(self, hicp):
        self.__logger.debug("TestAppML connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug
        hicp.set_text_group(self.LANG_EN_CA)

        window = Window()
        window.set_groups_text({
                self.LANG_EN_CA: "Window",
                self.LANG_FR_CA: "Fenȇtre"
            }, hicp)
        window.set_handle_close(ButtonWindowCloser())
        hicp.add(window)
        self.__logger.debug("TestAppML done add window")

        # TODO: Make amazing panel, add amazing label.
        amazing_panel = Panel()
        window.add(amazing_panel, 0, 0)

        amazing_label = Label()
        amazing_label.set_groups_text({
                self.LANG_EN_CA : "Amazing!",
                self.LANG_FR_CA : "Sensationnel!"
            }, hicp)
        amazing_panel.add(amazing_label, 0, 0)

        button_en = Button()
        button_en.set_groups_text({
                self.LANG_EN_CA : "English",
                self.LANG_FR_CA : "English"
            }, hicp)
        button_en.set_handle_click(
            ButtonLangHandler(self.LANG_EN_CA, hicp)
        )
        amazing_panel.add(button_en, 0, 1)

        button_fr = Button()
        button_fr.set_groups_text({
                self.LANG_EN_CA : "Français",
                self.LANG_FR_CA : "Français"
            }, hicp)
        button_fr.set_handle_click(
            ButtonLangHandler(self.LANG_FR_CA, hicp)
        )
        amazing_panel.add(button_fr, 0, 2)

        click_label = Label()
        click_label.set_groups_text({
                self.LANG_EN_CA : "Please click the button.",
                self.LANG_FR_CA : "Veuillez cliquer sur le bouton."
            }, hicp)
        click_label.set_size(1, 1)  # debug
        window.add(click_label, 1, 0)

        button = Button()
        button.set_groups_text({
                self.LANG_EN_CA : "Button",
                self.LANG_FR_CA : "Bouton"
            }, hicp)
        button.set_size(1, 1)  # debug
        button.set_handle_click(
            ButtonHandlerML(click_label, {
                self.LANG_EN_CA : "Thank you. Don't click the button again.",
                self.LANG_FR_CA : "Merci. Ne cliquez plus sur le bouton."
            }, hicp)
        )
        window.add(button, 1, 1)

        text_field = TextField()
        text_field.set_content("This is text.")
        # debug - test binary attribute - underline "is"
        # Should be: 5 2 6
        text_field.set_attribute(TextField.UNDERLINE, 5, 2)
        # debug - test value attribute - size of "text"
        # Should be: 8 2=4 1
        text_field.set_attribute(TextField.SIZE, 8, 4, "2")
        text_field.set_handle_changed(
            TextFieldHandlerML(click_label, {
                self.LANG_EN_CA : "Text has been changed.",
                self.LANG_FR_CA : "Le texte a été modifié."
            }, hicp)
        )
        window.add(text_field, 1, 2)

        window.set_visible(True)
        window.update()


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

