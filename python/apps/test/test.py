import os

from datetime import datetime

from hicp import HICP, newLogger, EventType, TimeHandler, TimeHandlerInfo, Message, Panel, Window, Label, Button, TextField, Selection, SelectionItem
from hicp import App, AppInfo

class DisconnectHandler:
    def process(self, event_message):
        print("DisconnectHandler.process()")  # debug

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
        self.__label.set_text_id(self.__next_text_id)
        self.__label.update()

        text_field.set_content("Woo-hoo!")
        text_field.update()


class AbleButtonHandler:
    def __init__(self, other_button, text_field, enabled_text_id, disabled_text_id):
        self.__other_button = other_button
        self.__text_field = text_field
        self.__enabled_text_id = enabled_text_id
        self.__disabled_text_id = disabled_text_id

        self.__events = Button.ENABLED

    def update(self, hicp, event_message, button):
        if Button.ENABLED == self.__events:
            self.__events = Button.DISABLED
            new_text_id = self.__enabled_text_id
        else:
            self.__events = Button.ENABLED
            new_text_id = self.__disabled_text_id

        self.__other_button.set_events(self.__events)
        self.__other_button.update()

        self.__text_field.set_events(self.__events)
        self.__text_field.update()

        button.set_text_id(new_text_id)
        button.update()


class ClockHandler(TimeHandler):
    def __init__(self, clock_text):
        self.clock_text = clock_text

        # Display now as initial time, instead of blank for a second.
        self.update_clock_text(datetime.now())

        self.time_info = TimeHandlerInfo(1, is_repeating=True)

    def get_info(self):
        return self.time_info

    def process(self, event):
        # Update clock_text from event time.
        self.update_clock_text(event.event_time)

    def update_clock_text(self, new_time):
        self.clock_text.set_content(
            new_time.isoformat(sep=' ', timespec='seconds') )
        self.clock_text.update()


class TestApp(App):
    WINDOW_TITLE_ID = 1
    AMAZING_ID = 2
    BUTTON_ID = 3
    LABEL_CLICK_ID = 4
    LABEL_THANKS_ID = 5
    LABEL_CHANGED_ID = 6
    LABEL_PATH_ID = 7
    LABEL_CLOCK_ID = 8
    DISABLE_BUTTON_ID = 9
    ENABLE_BUTTON_ID = 10
    SELECTION_LABEL_ID = 11

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
        hicp.set_disconnect_handler(DisconnectHandler())

        hicp.text_direction(hicp.RIGHT, hicp.UP) # debug
        hicp.add_all_text({
            self.WINDOW_TITLE_ID : "Button window",
            self.AMAZING_ID : "Amazing!",
            self.BUTTON_ID : "Button",
            self.LABEL_CLICK_ID : "Please click the button.",
            self.LABEL_THANKS_ID : "Thank you. Don't click the button again.",
            self.LABEL_CHANGED_ID : "Text has been changed.",
            self.LABEL_PATH_ID : "Current path:",
            self.LABEL_CLOCK_ID : "Current time:",
            self.DISABLE_BUTTON_ID : "Disable",
            self.ENABLE_BUTTON_ID : "Enable",
            self.SELECTION_LABEL_ID : "Selection",
        })
        self.__logger.debug("TestApp done add text")

        window = self.new_app_window()
        window.set_text_id(self.WINDOW_TITLE_ID)
        hicp.add(window)

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
        button.set_handler(
            EventType.CLICK,
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
        text_field.set_handler(
            EventType.CHANGED,
            TextFieldHandler(click_label, self.LABEL_CHANGED_ID)
        )
        window.add(text_field, 1, 2)

        able_button = Button()
        able_button.set_text_id(self.DISABLE_BUTTON_ID)
        able_button.set_handler(
            EventType.CLICK,
            AbleButtonHandler(
                button, text_field, self.ENABLE_BUTTON_ID, self.DISABLE_BUTTON_ID
            )
        )
        window.add(able_button, 1, 3)

        list_panel = Panel()
        window.add(list_panel, 2, 1)

        selection_label = Label()
        selection_label.set_text_id(self.SELECTION_LABEL_ID)
        list_panel.add(selection_label, 0, 0)

        # Add selection list to list_panel
        selection = Selection()
        item_list = {}
        for item_id in range(1, 3):
            item_text_id = hicp.add_text_get_id('Selection ' + str(item_id))
            item = SelectionItem(item_id, item_text_id)
            item_list[item_id] = item
        selection.add_items(item_list)
        list_panel.add(selection, 0, 1)

        path_label = Label()
        path_label.set_text_id(self.LABEL_PATH_ID)
        window.add(path_label, 0, 4)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        path_field.set_events(TextField.DISABLED)
        window.add(path_field, 1, 4)

        clock_label = Label()
        clock_label.set_text_id(self.LABEL_CLOCK_ID)
        window.add(clock_label, 0, 5)

        clock_text = TextField()
        clock_text.set_events(TextField.DISABLED)
        window.add(clock_text, 1, 5)

        hicp.add_time_handler(ClockHandler(clock_text))

        window.set_visible(True)
        window.update()

