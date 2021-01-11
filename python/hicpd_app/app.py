class AppInfo:
    def __init__(self, name, description):
        self.name = name
        self.description = description

class App:
    def get_app_name(self):
        # Without knowing the desirec name, return the class name.
        return type(self).__name__

    def get_app_info(self):
        name = self.get_app_name()

        # If no description, use name.
        desc = name

        return AppInfo(name, desc)

    def connected(self):
        # By default, do nothing.
        hicp.disconnect()

