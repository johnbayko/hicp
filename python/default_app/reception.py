from hicp import HICP, newLogger, EventType, Message, Panel, Window, Label, Button, TextField
from hicp import App, AppInfo
from apps.test.test import TestApp
from apps.testml.testml import TestAppML

class ButtonSwitchAppHandler:
    def __init__(self, app_name):
        self.logger = newLogger(type(self).__name__)
        self.__app_name = app_name

    def update(self, hicp, event_message, component):
        hicp.switch_app(self.__app_name)

class Reception(App):

    def __init__(self):

        self.__logger = newLogger(type(self).__name__)

    @classmethod
    def get_app_name(cls):
        return 'reception'

    @classmethod
    def get_app_info(cls):
        app_name = cls.get_app_name()
        display_name = [('Reception', 'en')]
        desc = [('List apps for user to choose.', 'en')]

        return AppInfo(app_name, display_name, desc)

    def connected(self, hicp):
        self.__logger.debug("reception connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug

        WINDOW_TITLE_ID = hicp.add_text_get_id("App list")
        SELECT_APP_ID = hicp.add_text_get_id("Select app:")
        TEST_APP_ID = hicp.add_text_get_id("Test")
        TEST_APP_ML_ID = hicp.add_text_get_id("Test Multi-language")

        window = self.new_app_window()
        window.set_text_id(WINDOW_TITLE_ID)
        hicp.add(window)

        select_app_label = Label()
        select_app_label.set_text_id(SELECT_APP_ID)
        window.add(select_app_label, 0, 0)

        # Show found apps
        app_panel = Panel()
        window.add(app_panel, 0, 1)

        (group, subgroup) = hicp.get_text_group()

        # Sort them first
        unsorted_app_info = []
        for app_info in hicp.get_all_app_info().values():
            app_name = app_info.display_name.get_text(group, subgroup)
            app_name_id = hicp.add_text_get_id(app_name)
            unsorted_app_info.append((app_name_id, app_info))
        sorted_app_info = hicp.sort(unsorted_app_info)

        app_pos_y = 0
        for (app_name_id, app_info) in sorted_app_info:
            # Skip adding button for this app.
            if app_info.app_name != self.get_app_name():
                app_button = Button()
                app_button.set_text_id(app_name_id)
                app_button.set_handler(
                    EventType.CLICK,
                    ButtonSwitchAppHandler(app_info.app_name)
                )
                app_panel.add(app_button, 0, app_pos_y)

                app_desc = app_info.description.get_text(group, subgroup)
                app_desc_id = hicp.add_text_get_id(app_desc)

                app_label = Label()
                app_label.set_text_id(app_desc_id)
                app_panel.add(app_label, 1, app_pos_y)

                app_pos_y += 1

