import os
import random

from datetime import datetime

from hicp import HICP, newLogger, EventType, TimeHandler, TimeHandlerInfo, Message, Panel, Window, Label, Button, TextField, Selection, SelectionItem
from hicp import App, AppInfo

class Lang:
    # Using http://www.lingoes.net/en/translator/langcode.htm
    EN = "en"
    FR = "fr"

    CA = "ca"
    GB = "gb"
    US = "us"


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


class LangSelectionHandler:
    ENGLISH = 1
    ENGLISH_GB = 2
    FRENCH_CA = 3

    DEFAULT = ENGLISH

    def __init__(self, clock_handler):
        self.__clock_handler = clock_handler

        self.__group_info = {
            self.ENGLISH : (Lang.EN, None),
            self.ENGLISH_GB : (Lang.EN, Lang.GB),
            self.FRENCH_CA : (Lang.FR, Lang.FR),
        }
        self.__prev_selection_id = None

    def update(self, hicp, event, selection):
        selection_list = selection.copy_selected_list()
        try:
            # Take first selection.
            selection_id = selection_list[0]
            if selection_id != self.__prev_selection_id:
                try:
                    (group, subgroup) = self.__group_info[selection_id]

                    hicp.set_text_group(group, subgroup)
                    self.__clock_handler.text_group_changed()

                    self.__prev_selection_id = selection_id
                except KeyError:
                    return
        except KeyError:
            # No selection
            return


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

class SelectionAddHandler:
    def __init__(self, selection, next_id):
        self.__selection = selection
        self.__next_id = next_id

    def update(self, hicp, event, button):
        item_text_id = hicp.add_groups_text_get_id( [
                ("Number " + str(self.__next_id), Lang.EN),
                ("Numero " + str(self.__next_id), Lang.FR, Lang.CA)
            ])
        new_item_list = {
            self.__next_id : SelectionItem(self.__next_id, item_text_id)
        }
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
        item_list = self.__selection.copy_items()

        selected_list = self.__selection.copy_selected_list()
        for selected_id in selected_list:
            try:
                si = item_list[selected_id]
                if si.item_id == selected_id:
                    si.events = Selection.DISABLED
            except KeyError:
                # Don'e disable what's not there.
                pass

        self.__selection.set_items(item_list)
        self.__selection.update()

class SelectionEnableHandler:
    def __init__(self, selection):
        self.__selection = selection

    def update(self, hicp, event, button):
        # Items don't actually change, want to keep selection after updating
        # item list.
        selected_list = self.__selection.copy_selected_list()

        item_list = self.__selection.copy_items()
        for _, item in item_list.items():
            item.events = Selection.ENABLED

        self.__selection.set_items(item_list)
        self.__selection.set_selected_list(selected_list)
        self.__selection.update()

class SelectionRandomHandler:
    def __init__(self, selection):
        self.__selection = selection

    def update(self, hicp, event, button):
        item_list = self.__selection.copy_items()
        selected_list = self.__selection.copy_selected_list()

        # Find what's available (not selected, enabled)
        selectable_list = []
        selected_set = set(selected_list)
        for item_id, item in item_list.items():
            if item_id not in selected_set:
                if item.events != Selection.DISABLED:
                    selectable_list.append(item_id)

        if len(selectable_list) == 0:
            # Nothin available, nothing to select.
            return

        # Select one.
        rand_item_id = random.choice(selectable_list)

        # Add to current selection
        selected_list.append(rand_item_id)
        self.__selection.set_selected_list(selected_list)
        self.__selection.update()


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
                Lang.EN
            ),
            (
                "Test ML",
                Lang.EN,
                Lang.GB
            ),
            (
                "Test ML",
                Lang.FR,
                Lang.CA
            ),
        ]
        desc = [
            (
                "Test some components with multiple languages.",
                Lang.EN
            ),
            (
                "Test some components with multiple languages.",
                Lang.EN,
                Lang.GB
            ),
            (
                "Testez quelques pièces avec multiple langues.",
                Lang.FR,
                Lang.CA
            ),
        ]
        return AppInfo(app_name, display_name, desc)

    def connected(self, hicp):
        self.__logger.debug("TestAppML connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug
        hicp.set_text_group(Lang.EN)

        time_format_id = hicp.add_groups_text_get_id( [
                ("%m/%d/%Y %I:%M:%S %p", Lang.EN),
                ("%d/%m/%Y %I:%M:%S %p", Lang.EN, Lang.GB),
                ("%d/%m/%Y %H:%M:%S", Lang.FR, Lang.CA)
            ] )
        clock_handler = ClockHandler(hicp, time_format_id)

        self.ENABLE_ID = hicp.add_groups_text_get_id( [
                ("Enable", Lang.EN),
                ("Activer", Lang.FR, Lang.CA)
            ])
        self.DISABLE_ID = hicp.add_groups_text_get_id( [
                ("Disable", Lang.EN),
                ("Désactiver", Lang.FR, Lang.CA)
            ])

        window = self.new_app_window()
        window.set_groups_text( [
                ("Window", Lang.EN),
                ("Fenȇtre", Lang.FR, Lang.CA)
            ], hicp)
        hicp.add(window)
        self.__logger.debug("TestAppML done add window")

        amazing_label = Label()
        amazing_label.set_groups_text( [
                ( "Amazing!", Lang.EN),
                ( "Brilliant!", Lang.EN, Lang.GB),
                ( "Sensationnel!", Lang.FR, Lang.CA)
            ], hicp)
        window.add(amazing_label, 0, 0)

        # Components being tested get their own panel
        component_panel = Panel()

        status_label = Label()
        status_label.set_groups_text( [
                ( "Please click the button.", Lang.EN),
                ( "Veuillez cliquer sur le bouton.", Lang.FR, Lang.CA)
            ], hicp)
        status_label.set_size(1, 1)  # debug
        component_panel.add(status_label, 1, 0)

        button = Button()
        button.set_groups_text( [
                ( "Button", Lang.EN),
                ( "Bouton", Lang.FR, Lang.CA)
            ], hicp)
        button.set_size(1, 1)  # debug
        button.set_handler(
            EventType.CLICK,
            ButtonHandlerML(status_label, [
                ( "Thank you. Don't click the button again.",
                    Lang.EN),
                ( "Merci. Ne cliquez plus sur le bouton.",
                    Lang.FR, Lang.CA)
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
                ( "Text has been changed.", Lang.EN),
                ( "Le texte a été modifié.", Lang.FR, Lang.CA)
            ], hicp)
        )
        component_panel.add(text_field, 0, 1)

        # There's going to be a bunch of controls for testing the selection
        # component, so make a panel for them.
        selection_panel = Panel()

        selection_label = Label()
        selection_label.set_groups_text( [
                ( "Selection", Lang.EN),
                ( "Sélection", Lang.FR, Lang.CA)
            ], hicp)
        selection_panel.add(selection_label, 0, 0)

        # Add selection list to list_panel
        selection = Selection()
        item_list = {}
        for item_id in range(1, 5):
            item_text_id = hicp.add_groups_text_get_id( [
                    ("Number " + str(item_id), Lang.EN),
                    ("Numero " + str(item_id), Lang.FR, Lang.CA)
                ])
            item = SelectionItem(item_id, item_text_id)
            item_list[item_id] = item
        selection.add_items(item_list)
#        selection.set_presentation(Selection.SCROLL)  # debug
#        selection.set_presentation(Selection.TOGGLE)  # debug
        selection.set_presentation(Selection.DROPDOWN)  # debug
        selection.set_selection_mode(Selection.SINGLE)  # debug
#        selection.set_selection_mode(Selection.MULTIPLE)  # debug
#        selection.set_height(5)  # debug
        selection_panel.add(selection, 0, 1)

        # Add button
        selection_add_button = Button()
        selection_add_button.set_groups_text( [
                ( "Add new", Lang.EN),
                ( "Ajouter nouveau", Lang.FR, Lang.CA)
            ], hicp)
        selection_add_button.set_handler(
            EventType.CLICK,
            SelectionAddHandler(selection, len(item_list) + 1)
        )
        selection_panel.add(selection_add_button, 1, 1)

        # Remove button
        selection_remove_button = Button()
        selection_remove_button.set_groups_text( [
                ( "Remove selected", Lang.EN),
                ( "Supprimer choix", Lang.FR, Lang.CA)
            ], hicp)
        selection_remove_button.set_handler(
            EventType.CLICK,
            SelectionRemoveHandler(selection)
        )
        selection_panel.add(selection_remove_button, 1, 2)

        # Disable button
        selection_disable_button = Button()
        selection_disable_button.set_groups_text( [
                ( "Disable selected", Lang.EN),
                ( "Désactiver choix", Lang.FR, Lang.CA)
            ], hicp)
        selection_disable_button.set_handler(
            EventType.CLICK,
            SelectionDisableHandler(selection)
        )
        selection_panel.add(selection_disable_button, 1, 3)

        # Enable button
        selection_enable_button = Button()
        selection_enable_button.set_groups_text( [
                ( "Enable all", Lang.EN),
                ( "Activer tout", Lang.FR, Lang.CA)
            ], hicp)
        selection_enable_button.set_handler(
            EventType.CLICK,
            SelectionEnableHandler(selection)
        )
        selection_panel.add(selection_enable_button, 1, 4)

        # Select random
        selection_random_button = Button()
        selection_random_button.set_groups_text( [
                ( "Select random", Lang.EN),
                ( "Choisir au hazard", Lang.FR, Lang.CA)
            ], hicp)
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

        # Modifiers get another panel
        modifier_panel = Panel()

        select_lang = Selection()
        select_lang.set_presentation(Selection.DROPDOWN)
        select_lang.set_selection_mode(Selection.SINGLE)
        lang_list = {}
        lang_text_id = hicp.add_groups_text_get_id( [
                ( "English", Lang.EN),
                ( "English", Lang.FR, Lang.CA)
            ] )
        lang_item = SelectionItem(LangSelectionHandler.ENGLISH, lang_text_id)
        lang_list[lang_item.item_id] = lang_item
        lang_text_id = hicp.add_groups_text_get_id( [
                ( "English (UK)", Lang.EN),
                ( "English (UK)", Lang.FR, Lang.CA)
            ] )
        lang_item = SelectionItem(LangSelectionHandler.ENGLISH_GB, lang_text_id)
        lang_list[lang_item.item_id] = lang_item
        lang_text_id = hicp.add_groups_text_get_id( [
                ( "Français", Lang.EN),
                ( "Français", Lang.FR, Lang.CA)
            ] )
        lang_item = SelectionItem(LangSelectionHandler.FRENCH_CA, lang_text_id)
        lang_list[lang_item.item_id] = lang_item
        select_lang.add_items(lang_list)
        select_lang.set_handler(
            EventType.CHANGED,
            LangSelectionHandler(clock_handler)
        )
        modifier_panel.add(select_lang, 0, 0)

        # Button to emable/disable component panel stuff.
        able_button = Button()
        able_button.set_text_id(self.DISABLE_ID)
        able_button.set_handler(
            EventType.CLICK,
            AbleButtonHandler(
                button, text_field, selection, self.ENABLE_ID, self.DISABLE_ID
            )
        )
        modifier_panel.add(able_button, 0, 1)

        window.add(modifier_panel, 0, 1)

        path_label = Label()
        path_label.set_groups_text( [
                ( "Current Path", Lang.EN),
                ( "Path actuel", Lang.FR, Lang.CA)
            ], hicp)
        window.add(path_label, 0, 2)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        path_field.set_events(TextField.DISABLED)
        window.add(path_field, 1, 2)

        clock_label = Label()
        clock_label.set_groups_text( [
                ( "Current Time", Lang.EN),
                ( "Heure actuel", Lang.FR, Lang.CA)
            ], hicp)
        window.add(clock_label, 0, 3)

        clock_text = TextField()
        clock_text.set_events(TextField.DISABLED)
        window.add(clock_text, 1, 3)

        clock_handler.set_clock_text(clock_text)
        hicp.add_time_handler(clock_handler)

        window.set_visible(True)
        window.update()

