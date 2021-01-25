from hicp import HICP, newLogger, Message, Panel, Window, Label, Button, TextField
from hicp import App, AppInfo
from apps.test.test import TestApp
from apps.testml.testml import TestAppML

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

class Reception(App):
    APP_NAME_SELF = "self"
    APP_NAME_TEST = "test"
    APP_NAME_TEST_ML = "testml"

    def __init__(self):

        self.__logger = newLogger(type(self).__name__)

        # Make app list.
        self.app_list = {
            self.APP_NAME_SELF: self,
            self.APP_NAME_TEST: TestApp(),
            self.APP_NAME_TEST_ML: TestAppML()
        }
        self.default_app = self.APP_NAME_SELF

    @classmethod
    def get_app_name(cls):
        return 'reception'

    @classmethod
    def get_app_info(cls):
        name = cls.get_app_name()
        desc = [('List apps for user to choose.', 'en')]

        return AppInfo(name, desc)

    def connected(self, hicp):
        self.__logger.debug("reception connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug

        WINDOW_TITLE_ID = hicp.add_text_get_id("App list")
        SELECT_APP_ID = hicp.add_text_get_id("Select app:")
        TEST_APP_ID = hicp.add_text_get_id("Test")
        TEST_APP_ML_ID = hicp.add_text_get_id("Test Multi-language")

        window = Window()
        window.set_text_id(WINDOW_TITLE_ID)
        window.set_handle_close(ButtonWindowCloser())
        hicp.add(window)

        select_app_label = Label()
        select_app_label.set_text_id(SELECT_APP_ID)
        window.add(select_app_label, 0, 0)

        select_panel = Panel()
        window.add(select_panel, 0, 1)

        button_test = Button()
        button_test.set_text_id(TEST_APP_ID)
        button_test.set_handle_click(
            ButtonAppHandler(window, self.app_list[self.APP_NAME_TEST], hicp)
        )
        select_panel.add(button_test, 0, 0)

        button_test_ml = Button()
        button_test_ml.set_text_id(TEST_APP_ML_ID)
        button_test_ml.set_handle_click(
            ButtonAppHandler(window, self.app_list[self.APP_NAME_TEST_ML], hicp)
        )
        select_panel.add(button_test_ml, 0, 1)

        # Show found apps
        app_panel = Panel()
        window.add(app_panel, 1, 1)

        all_app_info = hicp.get_all_app_info()

        app_pos_y = 0
        for app_info in all_app_info.values():
            app_name_id = hicp.add_text_get_id(app_info.name)

            app_button = Button()
            app_button.set_text_id(app_name_id)
            app_panel.add(app_button, 0, app_pos_y)

            (group, subgroup) = hicp.get_text_group()
            app_desc = app_info.description.get_text(group, subgroup)
            app_desc_id = hicp.add_text_get_id(app_desc)

            app_label = Label()
            app_label.set_text_id(app_desc_id)
            app_panel.add(app_label, 1, app_pos_y)

            app_pos_y += 1


