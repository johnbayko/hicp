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

        window = Window()
        window.set_groups_text( [
                ("Window", self.LANG_EN),
                ("Window", self.LANG_EN, self.LANG__GB),
                ("Fenȇtre", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.set_handle_close(ButtonWindowCloser())
        hicp.add(window)
        self.__logger.debug("TestAppML done add window")

        # TODO: Make amazing panel, add amazing label.
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
                ( "English", self.LANG_EN, self.LANG__GB),
                ( "English", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en.set_handle_click(
            ButtonLangHandler(hicp, self.LANG_EN)
        )
        amazing_panel.add(button_en, 0, 1)

        button_en_gb = Button()
        button_en_gb.set_groups_text( [
                ( "English (UK)", self.LANG_EN, self.LANG__CA),
                ( "English (UK)", self.LANG_EN, self.LANG__GB),
                ( "English (UK)", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en_gb.set_handle_click(
            ButtonLangHandler(hicp, self.LANG_EN, self.LANG__GB)
        )
        amazing_panel.add(button_en_gb, 0, 2)

        button_fr_ca = Button()
        button_fr_ca.set_groups_text( [
                ( "Français", self.LANG_EN),
                ( "Français", self.LANG_EN, self.LANG__GB),
                ( "Français", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_fr_ca.set_handle_click(
            ButtonLangHandler(hicp, self.LANG_FR, self.LANG__CA)
        )
        amazing_panel.add(button_fr_ca, 0, 3)

        click_label = Label()
        click_label.set_groups_text( [
                ( "Please click the button.", self.LANG_EN),
                ( "Please click the button.", self.LANG_EN, self.LANG__GB),
                ( "Veuillez cliquer sur le bouton.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        click_label.set_size(1, 1)  # debug
        window.add(click_label, 1, 0)

        button = Button()
        button.set_groups_text( [
                ( "Button", self.LANG_EN),
                ( "Button", self.LANG_EN, self.LANG__GB),
                ( "Bouton", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button.set_size(1, 1)  # debug
        button.set_handle_click(
            ButtonHandlerML(click_label, [
                ( "Thank you. Don't click the button again.",
                    self.LANG_EN),
                ( "Thank you. Don't click the button again.",
                    self.LANG_EN, self.LANG__GB),
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
        text_field.set_handle_changed(
            TextFieldHandlerML(click_label, [
                ( "Text has been changed.", self.LANG_EN),
                ( "Text has been changed.", self.LANG_EN, self.LANG__GB),
                ( "Le texte a été modifié.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        )
        window.add(text_field, 1, 2)

        path_label = Label()
        path_label.set_groups_text( [
                ( "Current Path", self.LANG_EN),
                ( "Current Path", self.LANG_EN, self.LANG__GB),
                ( "Path actuel", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.add(path_label, 0, 3)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        window.add(path_field, 1, 3)

        window.set_visible(True)
        window.update()

