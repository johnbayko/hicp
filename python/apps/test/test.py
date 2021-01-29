import os

from hicp import HICP, newLogger, Message, Panel, Window, Label, Button, TextField
from hicp import App, AppInfo

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


class TestApp(App):
    WINDOW_TITLE_ID = 1
    AMAZING_ID = 2
    BUTTON_ID = 3
    LABEL_CLICK_ID = 4
    LABEL_THANKS_ID = 5
    LABEL_CHANGED_ID = 6
    LABEL_PATH_ID = 7

    def __init__(self):
        self.__logger = newLogger(type(self).__name__)

    @classmethod
    def get_app_name(cls):
        return 'test'

    @classmethod
    def get_app_info(cls):
        app_name = cls.get_app_name()
        display_name = [('Test', 'en')]
        desc = [('Test some components.', 'en')]

        return AppInfo(app_name, display_name, desc)

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
            self.LABEL_PATH_ID : "Current path.",
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

        path_label = Label()
        path_label.set_text_id(self.LABEL_PATH_ID)
        window.add(path_label, 0, 3)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        window.add(path_field, 1, 3)

        window.set_visible(True)
        self.__logger.debug("About to window.update") # debug
        window.update()
        self.__logger.debug("Done window.update") # debug

