import os
import random

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

    def update(self, hicp, event, component):
        self.logger.debug("ButtonHandler In update handler")
        self.__label.set_text_id(self.__next_text_id)
        self.__label.update()


class TextFieldHandler:
    def __init__(self, label, next_text_id):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_text_id = next_text_id

    def update(self, hicp, event, text_field):
        self.__label.set_text_id(self.__next_text_id)
        self.__label.update()

        text_field.set_content("Woo-hoo!")
        text_field.update()

class SelectionHandler:
    def __init__(self, selection_field):
        self.__selection_field = selection_field

    def update(self, hicp, event, selection):
        self.__selection_field.set_content(
                event.message.get_header(Message.SELECTED) )
        self.__selection_field.update()

class SelectionAddHandler:
    def __init__(self, selection, next_id):
        self.__selection = selection
        self.__next_id = next_id

    def update(self, hicp, event, button):
        item_text_id = hicp.add_text_get_id('Number ' + str(self.__next_id))
        new_item_list = [
            SelectionItem(self.__next_id, item_text_id)
        ]
        self.__next_id += 1
        self.__selection.add_items(new_item_list)
        self.__selection.update()

class SelectionRemoveHandler:
    def __init__(self, selection):
        self.__selection = selection

    def update(self, hicp, event, button):
        selected_list = self.__selection.copy_selected_list()
        self.__selection.del_items(selected_list)
        self.__selection.update()

class SelectionDisableHandler:
    def __init__(self, selection):
        self.__selection = selection

    def update(self, hicp, event, button):
        item_dict = self.__selection.copy_items()

        selected_list = self.__selection.copy_selected_list()
        for selected_id in selected_list:
            try:
                si = item_dict[selected_id]
                si.events = Selection.DISABLED
            except KeyError:
                # Don't disable what's not there.
                pass

        self.__selection.set_items(item_dict.values())
        self.__selection.update()

class SelectionEnableHandler:
    def __init__(self, selection):
        self.__selection = selection

    def update(self, hicp, event, button):
        # Items don't actually change, want to keep selection after updating
        # item list.
        selected_list = self.__selection.copy_selected_list()

        item_dict = self.__selection.copy_items()
        for _, item in item_dict.items():
            item.events = Selection.ENABLED

        self.__selection.set_items(item_dict.values())
        self.__selection.set_selected_list(selected_list)
        self.__selection.update()

class SelectionRandomHandler:
    def __init__(self, selection):
        self.__selection = selection

    def update(self, hicp, event, button):
        item_dict = self.__selection.copy_items()
        selected_list = self.__selection.copy_selected_list()

        # Find what's available (not selected, enabled)
        selectable_list = []
        selected_set = set(selected_list)
        for item_id, item in item_dict.items():
            if item_id not in selected_set:
                if item.events != Selection.DISABLED:
                    selectable_list.append(item_id)

        if len(selectable_list) == 0:
            # Nothing available, nothing to select.
            return

        # Select one.
        rand_item_id = random.choice(selectable_list)

        if Selection.MULTIPLE == self.__selection.get_selection_mode():
            # Add to current selection
            selected_list.append(rand_item_id)
        else:
            # Send new selection
            selected_list = [rand_item_id]
        self.__selection.set_selected_list(selected_list)
        self.__selection.update()

        # Won't get an event back, so pretend we did.

        # Pretend to receive a changed event.
        event = Message()
        event.set_type(Message.EVENT, Message.CHANGED)

        # It's from self.__selection.
        event.add_header(Message.ID, str(self.__selection.component_id))

        # Event selection string looks like "1, 2, 4".
        selected_str = ", ".join([str(i) for i in selected_list])
        event.add_header(Message.SELECTED, selected_str) 

        hicp.fake_event(event)


class AbleButtonHandler:
    def __init__(self, other_button, text_field, selection, enabled_text_id, disabled_text_id):
        self.__other_button = other_button
        self.__text_field = text_field
        self.__selection = selection
        self.__enabled_text_id = enabled_text_id
        self.__disabled_text_id = disabled_text_id

        self.__events = Button.ENABLED

    def update(self, hicp, event, button):
        selection_events = Message.ENABLED  # debug
        if Message.ENABLED == self.__events:
            self.__events = Message.DISABLED
#            selection_events = Message.DISABLED  # debug
            selection_events = Message.UNSELECT  # debug
            new_text_id = self.__enabled_text_id
        else:
            self.__events = Message.ENABLED
            new_text_id = self.__disabled_text_id

        self.__other_button.set_events(self.__events)
        self.__other_button.update()

        self.__text_field.set_events(self.__events)
        self.__text_field.update()

        self.__selection.set_events(selection_events)  # debug
        self.__selection.update()

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
    SELECTION_ADD_ID = 12
    SELECTION_REMOVE_ID = 13
    SELECTION_DISABLE_ID = 14
    SELECTION_ENABLE_ID = 15
    SELECTION_RANDOM_ID = 16

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
            self.SELECTION_ADD_ID : "Add new",
            self.SELECTION_REMOVE_ID : "Remove selected",
            self.SELECTION_DISABLE_ID : "Disable selected",
            self.SELECTION_ENABLE_ID : "Enable all",
            self.SELECTION_RANDOM_ID : 'Select random',
        })
        self.__logger.debug("TestApp done add text")

        window = self.new_app_window()
        window.set_text_id(self.WINDOW_TITLE_ID)
        hicp.add(window)

        amazing_label = Label()
        amazing_label.set_text_id(self.AMAZING_ID)
        window.add(amazing_label, 0, 0)

        # Components being tested get their own panel
        component_panel = Panel()

        status_label = Label()
        status_label.set_text_id(self.LABEL_CLICK_ID)
        status_label.set_size(1, 1)  # debug
        component_panel.add(status_label, 1, 0)

        button = Button()
        button.set_text_id(self.BUTTON_ID)
        button.set_size(1, 1)  # debug
        button.set_handler(
            EventType.CLICK,
            ButtonHandler(status_label, self.LABEL_THANKS_ID)
        )
        component_panel.add(button, 0, 0)

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
            TextFieldHandler(status_label, self.LABEL_CHANGED_ID)
        )
        component_panel.add(text_field, 0, 1)

        # There's going to be a bunch of controls for testing the selection
        # component, so make a panel for them.
        selection_panel = Panel()

        selection_label = Label()
        selection_label.set_text_id(self.SELECTION_LABEL_ID)
        selection_panel.add(selection_label, 0, 0)

        # Add selection list to selection_panel
        selection = Selection()
        item_list = []
        for item_id in range(1, 12):
            item = SelectionItem(item_id, 'Number ' + str(item_id), hicp)
            item_list.append(item)
        selection.add_items(item_list)
        selection.set_presentation(Selection.SCROLL)  # debug
#        selection.set_presentation(Selection.TOGGLE)  # debug
#        selection.set_presentation(Selection.DROPDOWN)  # debug
#        selection.set_selection_mode(Selection.SINGLE)  # debug
        selection.set_selection_mode(Selection.MULTIPLE)  # debug
#        selection.set_width(3)  # debug
#        selection.set_height(5)  # debug
        selection_panel.add(selection, 0, 1)

        # Add button
        selection_add_button = Button()
        selection_add_button.set_text_id(self.SELECTION_ADD_ID)
        selection_add_button.set_handler(
            EventType.CLICK,
            SelectionAddHandler(selection, len(item_list) + 1)
        )
        selection_panel.add(selection_add_button, 1, 1)

        # Remove button
        selection_remove_button = Button()
        selection_remove_button.set_text_id(self.SELECTION_REMOVE_ID)
        selection_remove_button.set_handler(
            EventType.CLICK,
            SelectionRemoveHandler(selection)
        )
        selection_panel.add(selection_remove_button, 1, 2)

        # Disable button
        selection_disable_button = Button()
        selection_disable_button.set_text_id(self.SELECTION_DISABLE_ID)
        selection_disable_button.set_handler(
            EventType.CLICK,
            SelectionDisableHandler(selection)
        )
        selection_panel.add(selection_disable_button, 1, 3)

        # Enable button
        selection_enable_button = Button()
        selection_enable_button.set_text_id(self.SELECTION_ENABLE_ID)
        selection_enable_button.set_handler(
            EventType.CLICK,
            SelectionEnableHandler(selection)
        )
        selection_panel.add(selection_enable_button, 1, 4)

        # Select random
        selection_random_button = Button()
        selection_random_button.set_text_id(self.SELECTION_RANDOM_ID)
        selection_random_button.set_handler(
            EventType.CLICK,
            SelectionRandomHandler(selection)
        )
        selection_panel.add(selection_random_button, 1, 5)

        selection_field = TextField()
        selection_field.set_events(TextField.DISABLED)
        selection_panel.add(selection_field, 0, 6)

        selection.set_handler(
            EventType.CHANGED,
            SelectionHandler(selection_field)
        )

        component_panel.add(selection_panel, 0, 2)

        window.add(component_panel, 1, 1)

        # Button to emable/disable component panel stuff.
        able_button = Button()
        able_button.set_text_id(self.DISABLE_BUTTON_ID)
        able_button.set_handler(
            EventType.CLICK,
            AbleButtonHandler(
                button, text_field, selection, self.ENABLE_BUTTON_ID, self.DISABLE_BUTTON_ID
            )
        )
        window.add(able_button, 0, 1)

        path_label = Label()
        path_label.set_text_id(self.LABEL_PATH_ID)
        window.add(path_label, 0, 2)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        path_field.set_events(TextField.DISABLED)
        window.add(path_field, 1, 2)

        clock_label = Label()
        clock_label.set_text_id(self.LABEL_CLOCK_ID)
        window.add(clock_label, 0, 3)

        clock_text = TextField()
        clock_text.set_events(TextField.DISABLED)
        window.add(clock_text, 1, 3)

        hicp.add_time_handler(ClockHandler(clock_text))

        window.set_visible(True)
        window.update()

