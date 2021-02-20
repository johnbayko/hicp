import os

from hicp import HICP, newLogger, EventType, Message, Panel, Window, Label, Button, TextField
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
    def __init__(self, hicp, group, subgroup=None):
        self.logger = newLogger(type(self).__name__)
        self.__group = group
        self.__subgroup = subgroup
        self.__hicp = hicp

    def update(self, hicp, event_message, component):
        self.logger.debug("ButtonLangHandler In update handler")
        self.__hicp.set_text_group(self.__group, self.__subgroup)


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


# Test multilingual features.
class TestAppML(App):
    # Using http://www.lingoes.net/en/translator/langcode.htm
    LANG_EN = "en"
    LANG_FR = "fr"
    LANG__CA = "ca"
    LANG__GB = "gb"

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

        amazing_panel = Panel()
        window.add(amazing_panel, 0, 0)

        amazing_label = Label()
        amazing_label.set_groups_text( [
                ( "Amazing!", self.LANG_EN),
                ( "Brilliant!", self.LANG_EN, self.LANG__GB),
                ( "Sensationnel!", self.LANG_FR, self.LANG__CA)
            ], hicp)
        amazing_panel.add(amazing_label, 0, 0)

        button_en = Button()
        button_en.set_groups_text( [
                ( "English", self.LANG_EN),
                ( "English", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en.set_handler(
            EventType.CLICK,
            ButtonLangHandler(hicp, self.LANG_EN)
        )
        amazing_panel.add(button_en, 0, 1)

        button_en_gb = Button()
        button_en_gb.set_groups_text( [
                ( "English (UK)", self.LANG_EN),
                ( "English (UK)", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en_gb.set_handler(
            EventType.CLICK,
            ButtonLangHandler(hicp, self.LANG_EN, self.LANG__GB)
        )
        amazing_panel.add(button_en_gb, 0, 2)

        button_fr_ca = Button()
        button_fr_ca.set_groups_text( [
                ( "Français", self.LANG_EN),
                ( "Français", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_fr_ca.set_handler(
            EventType.CLICK,
            ButtonLangHandler(hicp, self.LANG_FR, self.LANG__CA)
        )
        amazing_panel.add(button_fr_ca, 0, 3)

        click_label = Label()
        click_label.set_groups_text( [
                ( "Please click the button.", self.LANG_EN),
                ( "Veuillez cliquer sur le bouton.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        click_label.set_size(1, 1)  # debug
        window.add(click_label, 1, 0)

        button = Button()
        button.set_groups_text( [
                ( "Button", self.LANG_EN),
                ( "Bouton", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button.set_size(1, 1)  # debug
        button.set_handler(
            EventType.CLICK,
            ButtonHandlerML(click_label, [
                ( "Thank you. Don't click the button again.",
                    self.LANG_EN),
                ( "Merci. Ne cliquez plus sur le bouton.",
                    self.LANG_FR, self.LANG__CA)
            ], hicp)
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
            TextFieldHandlerML(click_label, [
                ( "Text has been changed.", self.LANG_EN),
                ( "Le texte a été modifié.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        )
        window.add(text_field, 1, 2)

        able_button = Button()
        able_button.set_text_id(self.DISABLE_ID)
        able_button.set_handler(
            EventType.CLICK,
            AbleButtonHandler(
                button, text_field, self.ENABLE_ID, self.DISABLE_ID
            )
        )
        window.add(able_button, 1, 3)

        path_label = Label()
        path_label.set_groups_text( [
                ( "Current Path", self.LANG_EN),
                ( "Path actuel", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.add(path_label, 0, 4)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        path_field.set_events(TextField.DISABLED)
        window.add(path_field, 1, 4)

        window.set_visible(True)
        window.update()

