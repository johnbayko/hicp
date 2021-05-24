import os

from datetime import datetime

from hicp import HICP, newLogger, EventType, TimeHandler, TimeHandlerInfo, Message, Panel, Window, Label, Button, TextField, Selection, SelectionItem
from hicp import App, AppInfo

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
    def __init__(self, hicp, clock_handler, group, subgroup=None):
        self.logger = newLogger(type(self).__name__)
        self.__hicp = hicp
        self.__clock_handler = clock_handler
        self.__group = group
        self.__subgroup = subgroup

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonLangHandler In update handler")
        self.__hicp.set_text_group(self.__group, self.__subgroup)
        self.__clock_handler.text_group_changed()


class TextFieldHandlerML:
    def __init__(self, label, next_group_text, hicp):
        self.logger = newLogger(type(self).__name__)
        self.__label = label
        self.__next_group_text = next_group_text
        self.__hicp = hicp

    def update(self, hicp, event_message, text_field):
        self.__label.set_groups_text(self.__next_group_text, self.__hicp)
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


class AbleButtonHandler:
    def __init__(self, other_button, text_field, selection, enabled_text_id, disabled_text_id):
        self.__other_button = other_button
        self.__text_field = text_field
        self.__selection = selection
        self.__enabled_text_id = enabled_text_id
        self.__disabled_text_id = disabled_text_id

        self.__events = Button.ENABLED

    def update(self, hicp, event_message, button):
        selection_events = Message.ENABLED  # debug
        if Button.ENABLED == self.__events:
            self.__events = Button.DISABLED
            selection_events = Message.UNSELECT  # debug
            new_text_id = self.__enabled_text_id
        else:
            self.__events = Button.ENABLED
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
    def __init__(self, hicp, time_format_id):
        self.time_info = TimeHandlerInfo(1, is_repeating=True)

        self.hicp = hicp
        self.time_format_id = time_format_id
        self.time_format = self.hicp.get_text(self.time_format_id)

    def get_info(self):
        return self.time_info

    def set_clock_text(self, clock_text):
        self.clock_text = clock_text

        if self.time_format is not None:
            # Display now as initial time, instead of blank for a second.
            self.update_clock_text(datetime.now())

    def update_clock_text(self, new_time):
        if self.time_format is not None:
            self.clock_text.set_content(
                new_time.strftime(self.time_format) )
        else:
            # default to ISO
            self.clock_text.set_content(
                new_time.isoformat(sep=' ', timespec='seconds') )

        self.clock_text.update()

    def text_group_changed(self):
        self.time_format = self.hicp.get_text(self.time_format_id)

        if self.clock_text is not None:
            self.update_clock_text(datetime.now())

    def process(self, event):
        # Update clock_text from event time.
        self.update_clock_text(event.event_time)


# Test multilingual features.
class TestAppML(App):
    # Using http://www.lingoes.net/en/translator/langcode.htm
    LANG_EN = "en"
    LANG_FR = "fr"
    LANG__CA = "ca"
    LANG__GB = "gb"
    LANG__US = "us"

    def __init__(self):
        self.__logger = newLogger(type(self).__name__)

    @classmethod
    def get_app_name(cls):
        return 'testml'

    @classmethod
    def get_app_info(cls):
        app_name = cls.get_app_name()
        display_name = [
            (
                "Test ML",
                cls.LANG_EN
            ),
            (
                "Test ML",
                cls.LANG_EN,
                cls.LANG__GB
            ),
            (
                "Test ML",
                cls.LANG_FR,
                cls.LANG__CA
            ),
        ]
        desc = [
            (
                "Test some components with multiple languages.",
                cls.LANG_EN
            ),
            (
                "Test some components with multiple languages.",
                cls.LANG_EN,
                cls.LANG__GB
            ),
            (
                "Testez quelques pièces avec multiple langues.",
                cls.LANG_FR,
                cls.LANG__CA
            ),
        ]
        return AppInfo(app_name, display_name, desc)

    def connected(self, hicp):
        self.__logger.debug("TestAppML connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug
        hicp.set_text_group(self.LANG_EN)

        time_format_id = hicp.add_groups_text_get_id( [
                ("%m/%d/%Y %I:%M:%S %p", self.LANG_EN),
                ("%d/%m/%Y %I:%M:%S %p", self.LANG_EN, self.LANG__GB),
                ("%d/%m/%Y %H:%M:%S", self.LANG_FR, self.LANG__CA)
            ] )
        clock_handler = ClockHandler(hicp, time_format_id)

        self.ENABLE_ID = hicp.add_groups_text_get_id( [
                ("Enable", self.LANG_EN),
                ("Activer", self.LANG_FR, self.LANG__CA)
            ])
        self.DISABLE_ID = hicp.add_groups_text_get_id( [
                ("Disable", self.LANG_EN),
                ("Désactiver", self.LANG_FR, self.LANG__CA)
            ])

        window = self.new_app_window()
        window.set_groups_text( [
                ("Window", self.LANG_EN),
                ("Fenȇtre", self.LANG_FR, self.LANG__CA)
            ], hicp)
        hicp.add(window)
        self.__logger.debug("TestAppML done add window")

        amazing_label = Label()
        amazing_label.set_groups_text( [
                ( "Amazing!", self.LANG_EN),
                ( "Brilliant!", self.LANG_EN, self.LANG__GB),
                ( "Sensationnel!", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.add(amazing_label, 0, 0)

        # Components being tested get their own panel
        component_panel = Panel()

        status_label = Label()
        status_label.set_groups_text( [
                ( "Please click the button.", self.LANG_EN),
                ( "Veuillez cliquer sur le bouton.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        status_label.set_size(1, 1)  # debug
        component_panel.add(status_label, 1, 0)

        button = Button()
        button.set_groups_text( [
                ( "Button", self.LANG_EN),
                ( "Bouton", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button.set_size(1, 1)  # debug
        button.set_handler(
            EventType.CLICK,
            ButtonHandlerML(status_label, [
                ( "Thank you. Don't click the button again.",
                    self.LANG_EN),
                ( "Merci. Ne cliquez plus sur le bouton.",
                    self.LANG_FR, self.LANG__CA)
            ], hicp)
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
            TextFieldHandlerML(status_label, [
                ( "Text has been changed.", self.LANG_EN),
                ( "Le texte a été modifié.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        )
        component_panel.add(text_field, 0, 1)

        selection_label = Label()
        selection_label.set_groups_text( [
                ( "Selection", self.LANG_EN),
                ( "Sélection", self.LANG_FR, self.LANG__CA)
            ], hicp)
        component_panel.add(selection_label, 0, 2)

        # Add selection list to list_panel
        selection = Selection()
        item_list = {}
        for item_id in range(1, 4):
            item_text_id = hicp.add_groups_text_get_id( [
                    ("Number " + str(item_id), self.LANG_EN),
                    ("Numero " + str(item_id), self.LANG_FR, self.LANG__CA)
                ])
            # Disable item 2 for testing.
            if item_id != 2:
                item = SelectionItem(item_id, item_text_id)
            else:
                item = SelectionItem(item_id, item_text_id, events=Message.DISABLED)
            item_list[item_id] = item
        selection.add_items(item_list)
        component_panel.add(selection, 0, 3)

        selection_field = TextField()
        selection_field.set_events(TextField.DISABLED)
        component_panel.add(selection_field, 0, 4)

        selection.set_handler(
            EventType.CHANGED,
            SelectionHandler(selection_field)
        )

        window.add(component_panel, 1, 1)

        # Modifiers get another panel
        modifier_panel = Panel()

        button_en = Button()
        button_en.set_groups_text( [
                ( "English", self.LANG_EN),
                ( "English", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en.set_handler(
            EventType.CLICK,
            ButtonLangHandler(hicp, clock_handler, self.LANG_EN)
        )
        modifier_panel.add(button_en, 0, 0)

        button_en_gb = Button()
        button_en_gb.set_groups_text( [
                ( "English (UK)", self.LANG_EN),
                ( "English (UK)", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en_gb.set_handler(
            EventType.CLICK,
            ButtonLangHandler(hicp, clock_handler, self.LANG_EN, self.LANG__GB)
        )
        modifier_panel.add(button_en_gb, 0, 1)

        button_fr_ca = Button()
        button_fr_ca.set_groups_text( [
                ( "Français", self.LANG_EN),
                ( "Français", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_fr_ca.set_handler(
            EventType.CLICK,
            ButtonLangHandler(hicp, clock_handler, self.LANG_FR, self.LANG__CA)
        )
        modifier_panel.add(button_fr_ca, 0, 2)

        # Button to emable/disable component panel stuff.
        able_button = Button()
        able_button.set_text_id(self.DISABLE_ID)
        able_button.set_handler(
            EventType.CLICK,
            AbleButtonHandler(
                button, text_field, selection, self.ENABLE_ID, self.DISABLE_ID
            )
        )
        modifier_panel.add(able_button, 0, 3)

        window.add(modifier_panel, 0, 1)

        path_label = Label()
        path_label.set_groups_text( [
                ( "Current Path", self.LANG_EN),
                ( "Path actuel", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.add(path_label, 0, 2)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        path_field.set_events(TextField.DISABLED)
        window.add(path_field, 1, 2)

        clock_label = Label()
        clock_label.set_groups_text( [
                ( "Current Time", self.LANG_EN),
                ( "Heure actuel", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.add(clock_label, 0, 3)

        clock_text = TextField()
        clock_text.set_events(TextField.DISABLED)
        window.add(clock_text, 1, 3)

        clock_handler.set_clock_text(clock_text)
        hicp.add_time_handler(clock_handler)

        window.set_visible(True)
        window.update()

