class AppInfo:
    def __init__(self, name, description):
        self.name = name
        self.description = description

class App:
    @classmethod
    def get_app_name(cls):
        # Without knowing the desired name, return the class name.
        return cls.__name__

    @classmethod
    def get_app_info(cls):
        name = cls.get_app_name()

        # If no description, use name.
        desc = name

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
