from hicp import HICP, newLogger, EventType, Message, Panel, Window, Label, Button, TextField, Selection, SelectionItem
from hicp import App, AppInfo
from apps.test.test import TestApp
from apps.testml.testml import TestAppML

class ButtonSwitchAppHandler:
    def __init__(self, app_name=None):
        self.logger = newLogger(type(self).__name__)
        self.__app_name = app_name

    def set_app_name(self, app_name):
        self.__app_name = app_name

    def update(self, hicp, event_message, component):
        if self.__app_name is not None:
            hicp.switch_app(self.__app_name)

class AppSelectionHandler:
    def __init__(self, buttonHandler, start_button, app_desc_label, app_text_ids):
        self.__button_handler = buttonHandler
        self.__start_button = start_button
        self.__app_desc_label = app_desc_label
        self.__app_text_ids = app_text_ids

    def update(self, hicp, event_message, selection):
        items = selection.get_selected_item_list()

        # Should be one selected item, but check to be sure.
        if 0 == len(items):
            return

        item = items[0]
        app = item.item
        self.__button_handler.set_app_name(app.app_name)

        (app_name_id, app_desc_id) = self.__app_text_ids[app]

        self.__start_button.set_text_id(app_name_id)
        self.__start_button.update()

        self.__app_desc_label.set_text_id(app_desc_id)
        self.__app_desc_label.update()

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

        app_text_ids = {}
        # Sort them first
        unsorted_app_info = []
        for app_info in hicp.get_all_app_info().values():
            app_name = app_info.display_name.get_text(group, subgroup)
            app_name_id = hicp.add_text_selector_get_id(app_info.display_name)

            unsorted_app_info.append((app_name_id, app_info))

            app_desc = app_info.description.get_text(group, subgroup)
            app_desc_id = hicp.add_text_selector_get_id(app_info.description)

            app_text_ids[app_info] = (app_name_id, app_desc_id)
        sorted_app_info = hicp.sort(unsorted_app_info)

        # Make app description label to add later.
        app_desc = Label()

        # Make start button to add later
        # Selection handler updates this:
        start_button_handler = ButtonSwitchAppHandler()
        start_button = Button()

        # Add app selection list.
        app_selection = Selection()
        app_selection.set_presentation(Selection.SCROLL)
        app_selection.set_selection_mode(Selection.SINGLE)
        # Add app names to selection list, using sorted app info index as id.
        app_items = []
        item_id = 0
        for (app_name_id, app_info) in sorted_app_info:
            # Skip adding button for this app.
            if app_info.app_name != self.get_app_name():
                app_item = SelectionItem(item_id, app_name_id, item=app_info)
                app_items.append(app_item)
                item_id += 1
        app_selection.add_items(app_items)
        app_selection.set_selected_list([0])
        app_selection.set_handler(
            EventType.CHANGED,
            AppSelectionHandler(start_button_handler, start_button, app_desc, app_text_ids)
        )
        app_panel.add(app_selection, 0, 0)

        items = app_selection.get_selected_item_list()
        # Should be one selected item, but check to be sure.
        if 0 < len(items):
            initial_item = items[0]
            initial_app = initial_item.item
            start_button_handler.set_app_name(initial_app.app_name)
            (initial_name_id, initial_desc_id) = app_text_ids[initial_app]
        else:
            initial_item = None
            initial_name_id = \
                initial_desc_id = hicp.add_text_get_id('None')

        # Add app description label here.
        app_desc.set_text_id(initial_desc_id)
        app_panel.add(app_desc, 1, 0)

        # Add start button here.
        start_button.set_text_id(initial_name_id)
        start_button.set_handler(
            EventType.CLICK, start_button_handler
        )
        start_button.set_size(1, 1)
        app_panel.add(start_button, 0, 1)
