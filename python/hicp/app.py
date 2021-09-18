from hicp.hicp import EventType, TextSelector
from hicp.component import Window

class AppInfo:
    # Description must be a dict that can be passed to set_groups_text()
    def __init__(self, app_name, display_name, description):
        if app_name is None:
            raise UnboundLocalError("app_name required, not defined")
        if display_name is None:
            raise UnboundLocalError("display_name required, not defined")
        if description is None:
            raise UnboundLocalError("description required, not defined")

        self.app_name = app_name
        self.display_name = self.make_text_selector(display_name)
        self.description = self.make_text_selector(description)

    def make_text_selector(self, text_param):
        description_ts = None

        if isinstance(text_param, str):
            # Convert string to TextSelector using [(text_param, '', '')].
            return  TextSelector([(text_param, '', '')])
        elif isinstance(text_param, tuple):
            # Convert (text, group, subgroup) to TextSelector
            # using [(text, group, subgroup)].

            # If not strings, or wrong size, let failures happen when trying to
            # use it.
            return  TextSelector([text_param])
        elif isinstance(text_param, list):
            # Convert [(text, group, subgroup)] to TextSelector

            # If list is wrong, let failure happen when trying to use it.
            return  TextSelector(text_param)
        elif isinstance(text_param, TextSelector):
            # Use TextSelector directly
            return  text_param

        # Anything else is a type error.
        raise TypeError("parameter type is unsupported: " + str(type(text_param)))

class AppWindowCloser:
    def update(self, hicp, event_message, component):
        hicp.remove(component)
        hicp.disconnect()

class App:
    @classmethod
    def get_app_name(cls):
        # Without knowing the desired name, return the class name.
        return cls.__name__

    @classmethod
    def get_app_info(cls):
        name = cls.get_app_name()

        # If no description, use name.
        desc = {'en-ca':name}

        return AppInfo(name, desc)

    def connected(self):
        # By default, do nothing.
        hicp.disconnect()

    def new_app_window(self):
        """Create a window with a close handler that disconnects when window is closed."""
        app_window = Window()
        app_window.set_handler(EventType.CLOSE, AppWindowCloser())
        return app_window

# For internal use.
class AppSpec:
    def __init__(self, app_cls, app_path):
        # If app instance needs to ba cached, can be done here.
        self.app_cls = app_cls

        # For change directory before calling app.connect()
        self.app_path = app_path
