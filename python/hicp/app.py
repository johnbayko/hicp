from hicp.hicp import TextSelector

class AppInfo:
    # Description must be a dict that can be passed to set_groups_text()
    def __init__(self, name, description):
        description_ts = None
        if name is None:
            raise UnboundLocalError("name required, not defined")
        if description is None:
            raise UnboundLocalError("description required, not defined")

        if isinstance(description, str):
            # Convert string to TextSelector using [(description, '', '')].
            description_ts = TextSelector([(description, '', '')])
        elif isinstance(description, tuple):
            # Convert (text, group, subgroup) to TextSelector
            # using [(text, group, subgroup)].

            # If not strings, or wrong size, let failures happen when trying to
            # use it.
            description_ts = TextSelector([description])
        elif isinstance(description, list):
            # Convert [(text, group, subgroup)] to TextSelector

            # If list is wrong, let failure happen when trying to use it.
            description_ts = TextSelector(description)
        elif isinstance(description, TextSelector):
            # Use TextSelector directly
            description_ts = descriptoon
        else:
            # Anything else is a type error.
            raise TypeError("description type is unsupported: " + str(type(description)))

        self.name = name
        self.description = description_ts

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

class AppSpec:
    def __init__(self, app_cls, app_path):
        # If app instance needs to ba cached, can be done here.
        self.app_cls = app_cls

        # For change directory before calling app.connect()
        self.app_path = app_path
