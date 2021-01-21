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
    def __init__(self, group, subgroup, hicp):
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
    LANG_EN = "en"
    LANG_FR = "fr"
    LANG__CA = "ca"

    def __init__(self):
        self.__logger = newLogger(type(self).__name__)

    @classmethod
    def get_app_name(cls):
        return 'testml'

    @classmethod
    def get_app_info(cls):
        name = cls.get_app_name()
        desc = [
            (
                "Test some components with multiple languages.",
                self.LANG_EN,
                self.LANG__CA
            ),
            (
                "Testez quelques pièces avec multiple langues.",
                self.LANG_FR,
                self.LANG__CA
            ),
        ]
        return AppInfo(name, desc)

    def connected(self, hicp):
        self.__logger.debug("TestAppML connected")
        hicp.text_direction(hicp.RIGHT, hicp.DOWN) # debug
        hicp.set_text_group(self.LANG_EN, self.LANG__CA)

        window = Window()
        window.set_groups_text( [
                ("Window", self.LANG_EN, self.LANG__CA),
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
                ( "Amazing!", self.LANG_EN, self.LANG__CA),
                ( "Sensationnel!", self.LANG_FR, self.LANG__CA)
            ], hicp)
        amazing_panel.add(amazing_label, 0, 0)

        button_en = Button()
        button_en.set_groups_text( [
                ( "English", self.LANG_EN, self.LANG__CA),
                ( "English", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_en.set_handle_click(
            ButtonLangHandler(self.LANG_EN, self.LANG__CA, hicp)
        )
        amazing_panel.add(button_en, 0, 1)

        button_fr = Button()
        button_fr.set_groups_text( [
                ( "Français", self.LANG_EN, self.LANG__CA),
                ( "Français", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button_fr.set_handle_click(
            ButtonLangHandler(self.LANG_FR, self.LANG__CA, hicp)
        )
        amazing_panel.add(button_fr, 0, 2)

        click_label = Label()
        click_label.set_groups_text( [
                ( "Please click the button.", self.LANG_EN, self.LANG__CA),
                ( "Veuillez cliquer sur le bouton.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        click_label.set_size(1, 1)  # debug
        window.add(click_label, 1, 0)

        button = Button()
        button.set_groups_text( [
                ( "Button", self.LANG_EN, self.LANG__CA),
                ( "Bouton", self.LANG_FR, self.LANG__CA)
            ], hicp)
        button.set_size(1, 1)  # debug
        button.set_handle_click(
            ButtonHandlerML(click_label, [
                ( "Thank you. Don't click the button again.",
                    self.LANG_EN, self.LANG__CA),
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
                ( "Text has been changed.", self.LANG_EN, self.LANG__CA),
                ( "Le texte a été modifié.", self.LANG_FR, self.LANG__CA)
            ], hicp)
        )
        window.add(text_field, 1, 2)

        path_label = Label()
        path_label.set_groups_text( [
                ( "Current Path", self.LANG_EN, self.LANG__CA),
                ( "Path actuel", self.LANG_FR, self.LANG__CA)
            ], hicp)
        window.add(path_label, 0, 3)

        path_field = TextField()
        path_field.set_content(os.getcwd())
        window.add(path_field, 1, 3)

        window.set_visible(True)
        window.update()

