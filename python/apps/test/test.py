import os
import random

from datetime import datetime
from dataclasses import dataclass
from enum import IntEnum, auto

from hicp import HICP, newLogger, EventType, TimeHandler, TimeHandlerInfo, Message, Component, Panel, Window, Label, Button, TextField, Selection, SelectionItem
from hicp import App, AppInfo

class DisconnectHandler:
    def process(self, event_message):
        pass

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
    def __init__(self, len_field, label, next_text_id):
        self.logger = newLogger(type(self).__name__)
        self.__len_field = len_field
        self.__label = label
        self.__next_text_id = next_text_id

    def update(self, hicp, event, text_field):
        print(f"text_field.get_attribute_string() {text_field.get_attribute_string()}")  # debug

        new_text = event.message.get_header(Message.CONTENT)
        new_text_len = len(new_text)
        new_text_len_str = str(new_text_len)
        self.__len_field.set_content(new_text_len_str)
        self.__len_field.update()

        self.__label.set_text_id(self.__next_text_id)
        self.__label.update()

class TextFieldChanger:
    def __init__(self, field, length_field):
        self._field = field
        self._length_field = length_field

        self._position = 0
        self._del_cnt = 0
        self._add_text = ''

    def get_position(self):
        return self._position

    def set_position(self, position: int):
        self._position = position
        return self._position

    def get_del_cnt(self):
        return self._del_cnt

    def set_del_cnt(self, del_cnt: int):
        self._del_cnt = del_cnt
        return self._del_cnt

    def get_add_text(self):
        return self._add_text

    def set_add_text(self, add_text: str):
        self._add_text = add_text
        return self._add_text

    # These should be called in the Update thread.

#    def del_before(self):
#        ...

#    def del_after(self):
#        ...

    def add_text(self):
#        print(f'start add_text()')  # debug
        self._field.add_content(self._position, self._add_text)
#        print(f'after add_content({str(self._position)}, {self._add_text})')  # debug
#        print(f'get_content() {self._field.get_content()}')  # debug
        self._field.update()

        # No change event from client, so update text length here.
        field_text = self._field.get_content()
        field_text_len = len(field_text)
        self._length_field.set_content(str(field_text_len))
        self._length_field.update()


class TextPositionHandler:
    def __init__(self, text_field_changer, text_position_field):
        self._text_field_changer = text_field_changer

        position_str = text_position_field.get_content()
        try:
            self._position = int(position_str)
        except ValueError:
            self._position = 0

    def update(self, hicp, event, text_position_field):
        new_position_str = event.message.get_header(Message.CONTENT)
        try:
            # Must be integer, but no range checks, protocol must handle that.
            new_position = int(new_position_str)
            self._text_field_changer.set_position(new_position)

            # Update to canonical representation.
            new_position_str = str(new_position)
            text_position_field.set_content(new_position_str)
            text_position_field.update()
        except ValueError:
            old_position = self._text_field_changer.get_position()
            # Set contents to old valid value.
            old_position_str = str(old_position)
            text_position_field.set_content(old_position_str)
            text_position_field.update()

class TextDelCntHandler:
    def __init__(self, text_field_changer, text_del_cnt_field):
        self._text_field_changer = text_field_changer

        del_cnt_str = text_del_cnt_field.get_content()
        try:
            self._del_cnt = int(del_cnt_str)
        except ValueError:
            self._del_cnt = 0

    def update(self, hicp, event, text_del_cnt_field):
        new_del_cnt_str = event.message.get_header(Message.CONTENT)
        try:
            # Must be integer, but no range checks, protocol must handle that.
            new_del_cnt = int(new_del_cnt_str)
            self._text_field_changer.set_del_cnt(new_del_cnt)

            # Update to canonical representation.
            new_del_cnt_str = str(new_del_cnt)
            text_del_cnt_field.set_content(new_del_cnt_str)
            text_del_cnt_field.update()
        except ValueError:
            old_del_cnt = self._text_field_changer.get_del_cnt()
            # Set contents to old valid value.
            old_del_cnt_str = str(old_del_cnt)
            text_del_cnt_field.set_content(old_del_cnt_str)
            text_del_cnt_field.update()

class TextAddTextHandler:
    def __init__(self, text_field_changer, text_add_text_field):
        self._text_field_changer = text_field_changer

        self._add_text = text_add_text_field.get_content()

    def update(self, hicp, event, text_add_text_field):
        new_add_text = event.message.get_header(Message.CONTENT)
        self._text_field_changer.set_add_text(new_add_text)

class TextAddButtonHandler:
    def __init__(self, text_field_changer):
        self._text_field_changer = text_field_changer

    def update(self, hicp, event, component):
        print(f'start TextAddButtonHandler.update()')  # debug
        self._text_field_changer.add_text()


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
        new_item_list = [
            SelectionItem(self.__next_id, 'Number ' + str(self.__next_id), hicp)
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


@dataclass
class AbleHandlerComponent:
    component: Component
    enable: str = Message.ENABLED
    disable: str = Message.DISABLED

class AbleButtonHandler:
    def __init__(self, able_list: list[AbleHandlerComponent], enabled_text_id, disabled_text_id):
        self.__able_vals: list[AbleHandlerComponent] = able_list

        self.__enabled_text_id = enabled_text_id
        self.__disabled_text_id = disabled_text_id

        self.__events = Button.ENABLED

    def update(self, hicp, event, button):
        is_enabling = (Message.DISABLED == self.__events)

        self.__events = Message.ENABLED if is_enabling else Message.DISABLED

        for able_val in self.__able_vals:
            able_val.component.set_events(
                able_val.enable if is_enabling else able_val.disable
            )
            able_val.component.update()

        button.set_text_id(
            self.__disabled_text_id if is_enabling else self.__enabled_text_id
        )
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


class TextEnum(IntEnum):
    WINDOW_TITLE_ID = auto()
    AMAZING_ID = auto()
    BUTTON_ID = auto()
    LABEL_CLICK_ID = auto()
    LABEL_THANKS_ID = auto()
    LABEL_CHANGED_ID = auto()
    LABEL_PATH_ID = auto()
    LABEL_CLOCK_ID = auto()
    DISABLE_BUTTON_ID = auto()
    ENABLE_BUTTON_ID = auto()
    SELECTION_LABEL_ID = auto()
    SELECTION_ADD_ID = auto()
    SELECTION_REMOVE_ID = auto()
    SELECTION_DISABLE_ID = auto()
    SELECTION_ENABLE_ID = auto()
    SELECTION_RANDOM_ID = auto()
    TEXT_LABEL_ID = auto()
    TEXT_LENGTH_ID = auto()
    LABEL_TEXT_POSITION_ID = auto()
    LABEL_DELETE_ID = auto()
    TEXT_DEL_BEFORE_ID = auto()
    TEXT_DEL_AFTER_ID = auto()
    LABEL_DEL_LENGTH_ID = auto()
    TEXT_ADD_ID = auto()

class TestApp(App):
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
            TextEnum.WINDOW_TITLE_ID : "Button window",
            TextEnum.AMAZING_ID : "Amazing!",
            TextEnum.BUTTON_ID : "Button",
            TextEnum.LABEL_CLICK_ID : "Please click the button.",
            TextEnum.LABEL_THANKS_ID : "Thank you. Don't click the button again.",
            TextEnum.LABEL_CHANGED_ID : "Text has been changed.",
            TextEnum.LABEL_PATH_ID : "Current path:",
            TextEnum.LABEL_CLOCK_ID : "Current time:",
            TextEnum.DISABLE_BUTTON_ID : "Disable",
            TextEnum.ENABLE_BUTTON_ID : "Enable",
            TextEnum.SELECTION_LABEL_ID : "Selection",
            TextEnum.SELECTION_ADD_ID : "Add new",
            TextEnum.SELECTION_REMOVE_ID : "Remove selected",
            TextEnum.SELECTION_DISABLE_ID : "Disable selected",
            TextEnum.SELECTION_ENABLE_ID : "Enable all",
            TextEnum.SELECTION_RANDOM_ID : 'Select random',
            TextEnum.TEXT_LABEL_ID : 'Text field',
            TextEnum.TEXT_LENGTH_ID : 'Length',
            TextEnum.LABEL_TEXT_POSITION_ID : 'Position',
            TextEnum.LABEL_DELETE_ID : 'Delete',
            TextEnum.TEXT_DEL_BEFORE_ID : 'Before',
            TextEnum.TEXT_DEL_AFTER_ID : 'After',
            TextEnum.LABEL_DEL_LENGTH_ID : 'Length',
            TextEnum.TEXT_ADD_ID : 'Add'
        })
        self.__logger.debug("TestApp done add text")

        window = self.new_app_window()
        window.set_text_id(TextEnum.WINDOW_TITLE_ID)
        hicp.add(window)

        # Components being tested get their own panel
        component_panel = Panel()
        component_panel.set_text_id(TextEnum.AMAZING_ID)

        status_label = Label()
        status_label.set_text_id(TextEnum.LABEL_CLICK_ID)
        status_label.set_size(1, 1)  # debug
        component_panel.add(status_label, 1, 0)

        button = Button()
        button.set_text_id(TextEnum.BUTTON_ID)
        button.set_size(1, 1)  # debug
        button.set_handler(
            EventType.CLICK,
            ButtonHandler(status_label, TextEnum.LABEL_THANKS_ID)
        )
        component_panel.add(button, 0, 0)

        text_panel = Panel()
        text_panel.set_text_id(TextEnum.TEXT_LABEL_ID)

        text_field = TextField()
        text_field.set_content("This _is ^text.")
        # debug - test binary attribute - underline "is"
        # Should be: 5 2 6
        text_field.set_attribute(TextField.UNDERLINE, 6, 2, True)
        # debug - test value attribute - size of "text"
        # Should be: 8 2=4 1
        text_field.set_attribute(TextField.SIZE, 10, 4, "2")
        text_panel.add(text_field, 0, 0)

        # Text length label
        text_len_label = Label()
        text_len_label.set_text_id(TextEnum.TEXT_LENGTH_ID)
        text_panel.add(text_len_label, 0, 1)

        # Text length field
        text_len_field = TextField()
        text_len_field.set_events(TextField.DISABLED)
        # User events aren't generated when set from the server side (ie
        # text_field.set_content() ), so explicitly set the length here.
        text_len_field.set_content(str(len(text_field.get_content())))
        text_panel.add(text_len_field, 1, 1)

        text_field.set_handler(
            EventType.CHANGED,
            TextFieldHandler(
                text_len_field, status_label, TextEnum.LABEL_CHANGED_ID
            )
        )

        text_field_changer = TextFieldChanger(text_field, text_len_field)

        # Note modify messages should only work when text field is disabled,
        # but this should test both cases.

        # Modify position
        text_position_label = Label()
        text_position_label.set_text_id(TextEnum.LABEL_TEXT_POSITION_ID)
        text_panel.add(text_position_label, 0, 2)

        text_position_field = TextField()
        text_position_field.set_content('0')
        text_position_field.set_handler(
            EventType.CHANGED,
            TextPositionHandler(text_field_changer, text_position_field)
        )
        text_panel.add(text_position_field, 1, 2)

        # Del
        text_delete_label = Label()
        text_delete_label.set_text_id(TextEnum.LABEL_DELETE_ID)
        text_panel.add(text_delete_label, 0, 3)

        # Del before
        text_del_before_button = Button()
        text_del_before_button.set_text_id(TextEnum.TEXT_DEL_BEFORE_ID)
        # TODO: Add handler
        text_panel.add(text_del_before_button, 1, 3)

        # Del after
        text_del_after_button = Button()
        text_del_after_button.set_text_id(TextEnum.TEXT_DEL_AFTER_ID)
        # TODO: Add handler
        text_panel.add(text_del_after_button, 2, 3)

        # Del count
        text_length_label = Label()
        text_length_label.set_text_id(TextEnum.LABEL_DEL_LENGTH_ID)
        text_panel.add(text_length_label, 3, 3)

        text_del_cnt_field = TextField()
        text_del_cnt_field.set_content('0')
        text_del_cnt_field.set_width(3)
        text_del_cnt_field.set_handler(
            EventType.CHANGED,
            TextDelCntHandler(text_field_changer, text_del_cnt_field)
        )
        text_panel.add(text_del_cnt_field, 4, 3)

        # Add
        text_add_button = Button()
        text_add_button.set_text_id(TextEnum.TEXT_ADD_ID)
        text_add_button.set_handler(
            EventType.CLICK,
            TextAddButtonHandler(text_field_changer)
        )
        text_panel.add(text_add_button, 0, 4)

        # Add text
        text_add_text_field = TextField()
        # Placeholder content to set size
        text_add_text_field.set_content('text to add')
        text_add_text_field.set_handler(
            EventType.CHANGED,
            TextAddTextHandler(text_field_changer, text_add_text_field)
        )
        text_panel.add(text_add_text_field, 1, 4)

        component_panel.add(text_panel, 0, 1)

        # There's going to be a bunch of controls for testing the selection
        # component, so make a panel for them.
        selection_panel = Panel()
        selection_panel.set_text_id(TextEnum.SELECTION_LABEL_ID)

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
        selection_add_button.set_text_id(TextEnum.SELECTION_ADD_ID)
        selection_add_button.set_handler(
            EventType.CLICK,
            SelectionAddHandler(selection, len(item_list) + 1)
        )
        selection_panel.add(selection_add_button, 1, 1)

        # Remove button
        selection_remove_button = Button()
        selection_remove_button.set_text_id(TextEnum.SELECTION_REMOVE_ID)
        selection_remove_button.set_handler(
            EventType.CLICK,
            SelectionRemoveHandler(selection)
        )
        selection_panel.add(selection_remove_button, 1, 2)

        # Disable button
        selection_disable_button = Button()
        selection_disable_button.set_text_id(TextEnum.SELECTION_DISABLE_ID)
        selection_disable_button.set_handler(
            EventType.CLICK,
            SelectionDisableHandler(selection)
        )
        selection_panel.add(selection_disable_button, 1, 3)

        # Enable button
        selection_enable_button = Button()
        selection_enable_button.set_text_id(TextEnum.SELECTION_ENABLE_ID)
        selection_enable_button.set_handler(
            EventType.CLICK,
            SelectionEnableHandler(selection)
        )
        selection_panel.add(selection_enable_button, 1, 4)

        # Select random
        selection_random_button = Button()
        selection_random_button.set_text_id(TextEnum.SELECTION_RANDOM_ID)
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
        able_button.set_text_id(TextEnum.DISABLE_BUTTON_ID)
        able_button.set_handler(
            EventType.CLICK,
            AbleButtonHandler(
                [
                    AbleHandlerComponent(button,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),

                    AbleHandlerComponent(text_field,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),
                    # Text manipulation only works when text field is disabled,
                    # so these have enable/disable values reversed.
#                    AbleHandlerComponent(text_position_field,
#                        enable=Message.DISABLED,
#                        disable=Message.ENABLED),
#                    AbleHandlerComponent(text_del_before_button,
#                        enable=Message.DISABLED,
#                        disable=Message.ENABLED),
#                    AbleHandlerComponent(text_del_after_button,
#                        enable=Message.DISABLED,
#                        disable=Message.ENABLED),
#                    AbleHandlerComponent(text_del_cnt_field,
#                        enable=Message.DISABLED,
#                        disable=Message.ENABLED),
#                    AbleHandlerComponent(text_add_button,
#                        enable=Message.DISABLED,
#                        disable=Message.ENABLED),
#                    AbleHandlerComponent(text_add_text_field,
#                        enable=Message.DISABLED,
#                        disable=Message.ENABLED),

                    AbleHandlerComponent(selection,
                        enable=Message.ENABLED,
                        disable=Message.UNSELECT),
                    AbleHandlerComponent(selection_add_button,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),
                    AbleHandlerComponent(selection_remove_button,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),
                    AbleHandlerComponent(selection_disable_button,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),
                    AbleHandlerComponent(selection_enable_button,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),
                    AbleHandlerComponent(selection_random_button,
                        enable=Message.ENABLED,
                        disable=Message.DISABLED),
                ],
                TextEnum.ENABLE_BUTTON_ID, TextEnum.DISABLE_BUTTON_ID
            )
        )
        window.add(able_button, 0, 1)

        path_label = Label()
        path_label.set_text_id(TextEnum.LABEL_PATH_ID)
        window.add(path_label, 0, 2)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        path_field.set_events(TextField.DISABLED)
        window.add(path_field, 1, 2)

        clock_label = Label()
        clock_label.set_text_id(TextEnum.LABEL_CLOCK_ID)
        window.add(clock_label, 0, 3)

        clock_text = TextField()
        clock_text.set_events(TextField.DISABLED)
        window.add(clock_text, 1, 3)

        hicp.add_time_handler(ClockHandler(clock_text))

        window.set_visible(True)
        window.update()
