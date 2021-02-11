import os
import os.path
import pathlib
import queue
import threading

from hicp.logger import newLogger
from hicp.message import Message, Event

class WriteThread(threading.Thread):
    def __init__(self, out_stream):
        self.out_stream = out_stream
        self.logger = newLogger(type(self).__name__)

        self.write_queue = queue.Queue()

        threading.Thread.__init__(self)

    def write(self, message):
        self.write_queue.put(message)

    def run(self):
        while True:
            message = self.write_queue.get()
            if message is None:
                self.logger.debug("Got None from queue") # debug
                # End of messages to write.
                return

            message.write(self.out_stream)


class ReadThread(threading.Thread):
    def __init__(self, in_stream, event_thread):
        self.in_stream = in_stream
        self.event_thread = event_thread

        self.logger = newLogger(type(self).__name__)

        threading.Thread.__init__(self)

    def run(self):
        while True:
            event = Event(in_stream=self.in_stream)

            self.event_thread.add(event)

            if event.disconnected:
                break


class ProcessThread(threading.Thread):
    def __init__(self, event_thread):

        self.event_thread = event_thread
        self.logger = newLogger(type(self).__name__)

        self.event_queue = queue.Queue()

        threading.Thread.__init__(self)

    def add(self, event):
        self.event_queue.put(event)

    def run(self):
        self.logger.debug("Process thread started")

        while True:
            self.logger.debug("ProcessThread about to get Event")
            event = self.event_queue.get()

            if event is None or event.disconnected:
                self.logger.debug("Process thread end of file input")
                break

            # Event must be entirely by event thread, so just call event
            # handler process method.
            self.event_process(event)

        self.logger.debug("Process thread end of loop")

    def event_process(self, event):
        # This thread first calls "feedback" handler, if it exists.
        try:
            component = event.component
        except AttributeError:
            component = None

        try:
            handler = event.handler
        except AttributeError:
            # No handler means nothing to do.
            self.logger.debug("event_process says event has no handler")
            return

        try:
            if component is not None:
                handler.process(event, component)
            else:
                handler.process(event)

        except AttributeError:
            # No process handler, no biggie. Ignore it.
            self.logger.debug("event handler has no process method")
        except TypeError:
            # Process handler has wrong number of arguments.
            self.logger.debug("event process handler has wrong number of args")

        # If an update handler method exists, the event stage is
        # updated and the event is returned to this thread.
        try:
            self.logger.debug("About to check for handler.update")
            if handler.update is not None:
                self.logger.debug("has handler.update")
                event.stage = Event.STAGE_UPDATE
                self.logger.debug("changed stage to UPDATE")
                self.event_thread.add(event)
                self.logger.debug("Added to event_thread")

            self.logger.debug("Done check for handler.update")

        except AttributeError:
            # No process handler method.
            self.logger.debug("event_process event handler has no update method")


class EventThread(threading.Thread):
    def __init__(
        self,
        hicp,
        component_list,
        event_handler_list,
        write_thread,
        default_app,
        app_list=None,
        authenticator=None):

        self.logger = newLogger(type(self).__name__)

        self.hicp = hicp
        self.component_list = component_list
        self.event_handler_list = event_handler_list
        self.write_thread = write_thread
        self.default_app = default_app
        self.app_list = app_list
        self.authenticator = authenticator

        # Used later before starting an app.
        self.start_path = pathlib.Path(os.getcwd())

        self.suspend_app = False
        self.event_queue = queue.Queue()
        self.connect_event = None

        threading.Thread.__init__(self)

    def add(self, event):
        self.event_queue.put(event)

    def run(self):
        self.logger.debug("Event thread started")

        STATE_WAIT_CONNECT = 1
        STATE_WAIT_AUTHENTICATE = 2
        STATE_RUNNING = 3

        self.process_thread = ProcessThread(self)
        self.process_thread.start()

        state = STATE_WAIT_CONNECT
        self.logger.debug("Initial state STATE_WAIT_CONNECT") # debug
        while True:
            event = self.event_queue.get()

            if event is None or event.disconnected:
                self.logger.debug("End of file input")
                self.process_thread.add(event)
                self.logger.debug("Passed on to process thread")
                break

            if event.get_type() != Message.EVENT:
                # Ignore all non-event messages
                self.logger.debug("Non-event " + event.get_type())  # debug
                continue

            event_type = event.get_type_value()

            if STATE_WAIT_CONNECT == state:
                if Message.CONNECT == event_type:
                    # save connection info.
                    self.connect_event = event

                    if self.authenticator is not None:
                        # Send an authentication request
                        self.authenticate()

                        # Wait for authentication reply
                        state = STATE_WAIT_AUTHENTICATE
                    else:
                        # No authentication needed, start app running.
                        self.start_application()
                        state = STATE_RUNNING

            elif STATE_WAIT_AUTHENTICATE == state:
                if Message.DISCONNECT == event_type:
                    state = STATE_WAIT_CONNECT

                elif Message.AUTHENTICATE == event_type:
                    # Check authentication
                    if self.authenticator.authenticate(event):
                        # Authenticated, start app running
                        self.start_application()
                        state = STATE_RUNNING
                    else:
                        # Can the application do
                        # authentication?
                        try:
                            app = self.get_app()
                            if app is not None:
                                app.authenticate(hicp, event)
                                state = STATE_RUNNING
                        except AttributeError:
                            # Can't authenticate, send disconnect
                            # command and go back to wait for connect.
                            self.disconnect()
                            state = STATE_WAIT_CONNECT
                else:
                    # Ignore any other messages.
                    pass
            elif STATE_RUNNING == state :
                if Message.DISCONNECT == event_type:
                    # No longer running, wait for next connection.
                    state = STATE_WAIT_CONNECT
                elif Message.AUTHENTICATE == event_type:
                    # Don't need authentication now, ignore (might be
                    # extra).
                    pass
                else:
                    # Real event.
                    if self.suspend_app:
                        # Ignore all app events - app is being shutdown.
                        pass
                    elif Event.STAGE_FEEDBACK == event.stage:
                        self.logger.debug("Feedback stage: " + str(event.stage)) # debug
                        self.set_event_component(event)

                        # Find the proper event handler based on event type.
                        event.handler = None

                        self.logger.debug("event type: " + event_type) # debug
                        if Message.CLOSE == event_type:
                            self.logger.debug("Got close event, stage " + str(event.stage))
                            # Add the close event handler to event message.
                            try:
                                if event.component is not None:
                                    event.handler = event.component.get_handle_close()
                            except AttributeError:
                                # Component does not respond to close
                                # message, handler is incorrect.
                                event.handler = None

                        elif Message.CLICK == event_type:
                            self.logger.debug("Got click event, stage " + str(event.stage))
                            # Add the close event handler to event message.
                            try:
                                if event.component is not None:
                                    event.handler = event.component.get_handle_click()
                            except AttributeError:
                                # Component does not respond to close
                                # message, handler is incorrect.
                                event.handler = None

# here
                        elif Message.CHANGED == event_type:
                            self.logger.debug("Got changed event, stage " + str(event.stage))
                            # Add the close event handler to event message.
                            try:
                                if event.component is not None:
                                    event.handler = \
                                        event.component.get_handle_changed(event)
                            except AttributeError:
                                # Component does not respond to changed
                                # message, handler is incorrect.
                                event.handler = None

                        if event.handler is not None:
                            # Handler was added to event message.
                            self.event_feedback(event)

                    elif Event.STAGE_UPDATE == event.stage:
                        # event already has component and event fields,
                        # just call event_update()
                        self.logger.debug("Update stage: " + str(event.stage))
                        self.event_update(event)
                    else:
                        # Shouldn't happen. Maybe log it?
                        self.logger.debug("Unidentified stage: " + str(event.stage))
                        pass
            else:
                # Should never happen.
                self.logger.debug("Invalid state: " + str(state))
                state = STATE_WAIT_CONNECT

        # Fix current directory. See start_application() for why.
        os.chdir(self.start_path)

        self.logger.debug("Wait for process thread")
        self.process_thread.join()
        self.logger.debug("Done wait for process thread")

    def authenticate(self):
        message = Message()
        message.set_type(Message.COMMAND, Message.AUTHENTICATE)

        method_field = ""
        method_sep = ""
        for method in self.authenticator.get_methods():
            method_field = method_field + method_sep + method
            method_sep = ", "
        message.add_header(Message.METHOD, method_field)

        self.write_thread.write(message)

    def get_app_name_to_start(self):
        if self.app_list is None:
            # Nothing to start, so done.
            return None

        if self.connect_event is None:
            # Not conected.
            return None

        app_name = self.connect_event.get_header(Message.APPLICATION)

        if app_name is None:
            # Connection event didn't specify one, use default.
            app_name = self.default_app

        if app_name is None or app_name not in self.app_list.keys():
            # No default, pick first app in list.
            app_name = next(iter(self.app_list))

        return app_name

    def start_application(self):
        # select app and start running.
        app_name = self.get_app_name_to_start()

        self.start_app_by_name(app_name)

    def start_app_by_name(self, app_name):
        # I want to run apps in their own directory, but have to change back to
        # the start directory to instantiate an app from its class.
        os.chdir(self.start_path)

        if app_name is None:
            # No app found to start.
            return False

        app_spec = self.app_list.get(app_name)
        if app_spec is None:
            # No app found to start.
            return False

        app_cls = app_spec.app_cls
        app = app_cls()

        # App runs in the app directory, in case there are things like resource
        # files needed.
        os.chdir(app_spec.app_path)

        # Notify app that it's connected so it can send messages
        # to define the user interface.
        app.connected(self.hicp)

        return True

    def set_suspend_app(self, suspend_flag):
        self.suspend_app = suspend_flag

    def disconnect(self):
        message = Message()
        message.set_type(Message.COMMAND, Message.DISCONNECT)
        self.write_thread.write(message)

    def set_event_component(self, event):
        # Find the component ID.
        component_id = event.get_header(Message.ID)
        if component_id is None:
            return

        try:
            event.component = self.component_list[component_id]
        except KeyError:
            # Component not found, so can't do anything with event.
            # May have sent remove message, but didn't work for some
            # reason and other side thinks it's still there, but may be
            # some other reason, so don't try to "fix" it.
            self.logger.debug("Close event for unknown component id " + component_id)
            return

    def event_feedback(self, event):
        # This thread first calls "feedback" handler, if it exists.
        try:
            component = event.component
        except AttributeError:
            component = None

        try:
            handler = event.handler
        except AttributeError:
            # No handler means nothing to do.
            self.logger.debug("event_feedback says event has no handler")
            return

        # Call feedback method.
        try:
            if component is not None:
                handler.feedback(self.hicp, event, component)
            else:
                handler.feedback(self.hicp, event)

        except AttributeError:
            # No feedback handler, no
            # biggie. Ignore it.
            self.logger.debug("event handler has no feedback method")
        except TypeError:
            # Feedback handler has wrong number of arguments.
            self.logger.debug("event feedback handler has wrong number of args")

        # If a process handler method exists, the event stage is updated
        # and the event is sent to the process thread.
        try:
            if handler.process is not None:
                event.stage = Event.STAGE_PROCESS
                self.process_thread.add(event)
                return

            # Otherwise (shouldn't really be here, but it's not an
            # error) check for an update handler method.

        except AttributeError:
            # No process handler method.
            self.logger.debug("event handler has no process method")

        # Else if an update handler method exists, the event stage is
        # updated and the event is returned to this thread.
        try:
            if handler.update is not None:
                event.stage = Event.STAGE_UPDATE
                self.add(event)

        except AttributeError:
            # No process handler method.
            self.logger.debug("event_feedback event handler has no update method")

    def event_update(self, event):
        # This thread first calls "feedback" handler, if it exists.
        try:
            component = event.component
        except AttributeError:
            component = None

        try:
            handler = event.handler
        except AttributeError:
            # No handler means nothing to do.
            self.logger.debug("event_update says event has no handler")
            return

        try:
            if component is not None:
                handler.update(self.hicp, event, component)
            else:
                handler.update(self.hicp, event)

        except AttributeError as ae:
            # No process handler, no biggie. Ignore it.
            self.logger.debug("event_update event handler missing method: " + str(ae))
        except TypeError:
            # Process handler has wrong number of arguments.
            self.logger.debug("event update handler has wrong number of args")


class TextSelector:
    "Selects text based on specified (optional) groups and subgroups."

    def __init__(self, text_group_list = [('', '', '')]):
        """Initialise with optional list of texts for groups (text, group, subgroup)."""

        self.text_list = []
        self.add_all_text(text_group_list)

    def get_text_item_exact(self, group='', subgroup=''):
        """Get the text for the exact group and subgroup specified."""

        for text_item in self.text_list:
            (chk_group, chk_subgroup, chk_text) = text_item
            if chk_group == group and chk_subgroup == subgroup:
                return text_item
        # Nothing found.
        return None

    def add_text(self, text, group='', subgroup=''):
        "Replace or add text for the given group and subgroup."

        old_text_item = self.get_text_item_exact(group, subgroup)
        if old_text_item is not None:
            # Remove the old one.
            self.text_list.remove(old_text_item)
        self.text_list.append((group, subgroup, text))

    def add_all_text(self, text_group_list = [('', '', '')] ):
        # Tuples of (text, group, subgroup) (same as parameters for add_text()).
        # Default is '' for omitted values.
        for text_group in text_group_list:
            text = ''
            group = ''
            subgroup = ''
            if 3 == len(text_group):
                (text, group, subgroup) = text_group
            elif 2 == len(text_group):
                (text, group) = text_group
            elif 1 == len(text_group):
                (text) = text_group
            self.add_text(text, group, subgroup)

    def get_text(self, group='', subgroup=''):
        "Get text that matches the group and subgroup reasonably close."

        match_text = None

        match_strength = 0
        for text_item in self.text_list:
            (chk_group, chk_subgroup, chk_text) = text_item
            if chk_group == group and chk_subgroup == subgroup:
                chk_match_strength = 4
            elif chk_group == group and subgroup == '':
                # Subgroup not specified
                chk_match_strength = 3
            elif chk_group == group:
                # subgroup specified, but not matched
                chk_match_strength = 2
            else:
                chk_match_strength = 1

            if chk_match_strength > match_strength:
                match_text = chk_text
                match_strength = chk_match_strength

        return match_text

class TextManager:
    def __init__(self, start_group, start_subgroup):
        self.logger = newLogger(type(self).__name__)

        # Doesn't need to know what group it's in, just the text info.
        self.id_to_selector = {}

        # set groups without validating (can't use set_group() yet)
        self.group = start_group
        self.subgroup = start_subgroup

    def validate_group(self, group=None, subgroup=None):
        if subgroup is None:
            if group is None:
                subgroup = self.subgroup
            else:
                # Group specified but sub group intentionally not, use ''
                subgroup = ''

        if group is None:
            group = self.group

        return (group, subgroup)

    def set_group(self, new_group='', new_subgroup=''):
        (new_group, new_subgroup) = self.validate_group(new_group, new_subgroup)

        self.group = new_group
        self.subgroup = new_subgroup

    def get_group(self):
        return (self.group, self.subgroup)

    def is_group(self, chk_group=None, chk_subgroup=None):
        if chk_group is None and chk_subgroup is None:
            # None means default group, so true.
            return True
        if chk_group != self.group:
            return False
        if chk_subgroup is None:
            # group matched, no subgroup to check
            return True
        if chk_subgroup != self.subgroup:
            return False
        return True

    def add_text(self, text_id, text, group = None, subgroup = None):
        (group, subgroup) = self.validate_group(group, subgroup)

        selector = self.id_to_selector.get(text_id)
        if selector is None:
            # No text for this ID yet.
            selector = TextSelector()
            self.id_to_selector[text_id] = selector

        selector.add_text(text, group, subgroup)

    def find_free_id(self):
        text_ids = self.id_to_selector.keys()
        if 0 == len(text_ids):
            # Nothing, any ID will do.
            return 0
        sorted_text_ids = sorted(text_ids)

        # Start at lowest key value
        prev_text_id = sorted_text_ids[0]
        for text_id in sorted_text_ids:
            if (text_id - prev_text_id) > 1:
                # There was a gap in keys
                return prev_text_id + 1
            prev_text_id = text_id
        # No gap found, next key is end of list
        return prev_text_id + 1

    def add_text_get_id(self, text, group = None, subgroup = None):
        (group, subgroup) = self.validate_group(group, subgroup)

        # Don't want two ids for the same text, group, subgroup values, so
        # see if it's alreay been added.
        # Can't use TextSelector as key because two different selectors can
        # have identical values. So scan through id_to_selector list.
        # Can be sped up a lot with more complex data structures, but don't
        # bother unless needed.
        for text_id, selector in self.id_to_selector.items():
            if text == selector.get_text(group, subgroup):
                # Found it, return the id for it.
                return text_id

        # Is new text, group, subgroup, so add it and return new id.
        text_id = self.find_free_id()
        self.add_text(text_id, text, group, subgroup)
        return text_id

    def add_groups_text_get_id(self, text_group_list):
        """Add a text group list of the form used for TextSelector() -
        [(text, group, subgroup), ...]
        Allows duplicates if alrteady exists."""

        text_selector = TextSelector(text_group_list)

        text_id = self.find_free_id()
        self.id_to_selector[text_id] = text_selector

        return text_id

    def get_text(self, text_id):
        selector = self.id_to_selector.get(text_id)
        if selector is not None:
            return selector.get_text(self.group, self.subgroup)
        return None

    def keys(self):
        return self.id_to_selector.keys()


class HICP:
    "HICP control class"

    LEFT = Message.LEFT
    RIGHT = Message.RIGHT
    UP = Message.UP
    DOWN = Message.DOWN

    def __init__(
        self,
        in_stream,
        out_stream,
        app_list,
        default_app=None,
        text_group=None,
        text_subgroup=None,
        authenticator=None):

        # These must be specified for this to work.
        if in_stream is None:
            raise UnboundLocalError("in_stream required, not defined")

        if out_stream is None:
            raise UnboundLocalError("out_stream required, not defined")

        self.logger = newLogger(type(self).__name__)
        self.text_manager = TextManager(text_group, text_subgroup)

        self.in_stream = in_stream
        self.out_stream = out_stream
        self.__default_app = default_app
        self.__app_list = app_list

        if text_group is None:
            # Default language. If they don't specify, make it awkward enough
            # so they make the effort.
            # Canadian English: Remember the "our"s, but not the "ise"s.
            text_group = "en"
            text_subgroup = "ca"

        self.__text_group = text_group
        self.__text_subgroup = text_subgroup
        self.__authenticator = authenticator

        # Things for this object
        self.__gui_id = 0
        self.__component_list = {}
        self.__event_handler_list = {}

    def start(self):
        self.logger.debug("about to make WriteThread()")  # debug
        self.__write_thread = WriteThread(self.out_stream)
        self.__write_thread.start()

        self.logger.debug("about to make EventThread()")  # debug
        self.__event_thread = EventThread(
            self,
            self.__component_list,
            self.__event_handler_list,
            self.__write_thread,
            self.__default_app,
            self.__app_list,
            self.__authenticator)
        self.__event_thread.start()

        self.logger.debug("about to make ReadThread()")  # debug
        self.__read_thread = ReadThread(self.in_stream, self.__event_thread)
        self.__read_thread.start()
        self.logger.debug("about to join read_thread")  # debug
        self.__read_thread.join()

        self.logger.debug("about to join event_thread")  # debug
        self.__event_thread.join()

        # Stop write thread.
        self.logger.debug("about to __write_thread.write(None)")  # debug
        self.__write_thread.write(None)
        self.logger.debug("about to join __write_thread")  # debug
        self.__write_thread.join()

    def switch_app(self, app_name):
        # Set event thread to discard event processing
        self.__event_thread.set_suspend_app(True)

        # Remove all components in self.__component_list.
        # __component_list will be modified, so make copy of keys() and iterate
        # through that instead.
        component_id_list = list(self.__component_list.keys())
        for component_id in component_id_list:
            component = self.__component_list[component_id]
            self.remove(component)

        # Resume event thread processing
        self.__event_thread.set_suspend_app(False)

        # start new ap
        if not self.__event_thread.start_app_by_name(app_name):
            # if fails, disconnect
            self.disconnect()

    def get_all_app_info(self):
        # Return a copy of the app info from app list.
        app_info_dict = {}
        for app_spec in self.__app_list.values():
            app_cls = app_spec.app_cls
            app_info = app_cls.get_app_info()
            app_info_dict[app_info.app_name] = app_info

        return app_info_dict

    def set_text_group(self, text_group, text_subgroup = None):
        "Define what text group to use."
        if text_group != self.__text_group or text_subgroup != self.__text_subgroup:
            self.__text_group = text_group
            self.__text_subgroup = text_subgroup
            self.text_manager.set_group(text_group, text_subgroup)

            # Group is changed, send all text from new group to client.
            for text_id in self.text_manager.keys():
                text = self.text_manager.get_text(text_id)
                self.send_add_text_message(text_id, text)

    def get_text_group(self):
        return self.text_manager.get_group()

    def add_all_text(self, text_dict):
        """Add a dictionary of ID:text pairs - no group/subgroup support."""
        # Add each id/string entry in the dictionary
        for text_id, text in text_dict.items():
            self.add_text(text_id, text)

    def add_text(self, text_id, text, group=None, subgroup=None):
        if text_id is None:
            raise UnboundLocalError("text_id required, not defined")

        if text is None:
            raise UnboundLocalError("text required, not defined")

        # Send text down if group and subgroup match.
        if self.text_manager.is_group(group, subgroup):
            self.send_add_text_message(text_id, text)

        self.text_manager.add_text(text_id, text, group, subgroup)

    def send_add_text_message(self, text_id, text_string):
        message = Message()
        message.set_type(Message.COMMAND, Message.ADD)

        message.add_header(Message.CATEGORY, Message.TEXT)
        message.add_header(Message.ID, str(text_id))
        message.add_header(Message.TEXT, text_string)

        self.__write_thread.write(message)

    def add_text_get_id(self, text, group=None, subgroup=None):
        if text is None:
            raise UnboundLocalError("text required, not defined")

        text_id = self.text_manager.add_text_get_id(text, group, subgroup)

        # Send text down if group and subgroup match.
        if self.text_manager.is_group(group, subgroup):
            self.send_add_text_message(text_id, text)

        return text_id

    def add_groups_text_get_id(self, text_group_list):
        if text_group_list is None:
            raise UnboundLocalError("text_group_list required, not defined")

        text_id = self.text_manager.add_groups_text_get_id(text_group_list) 

        # Send text down if there is one for current group and subgroup.
        text = self.text_manager.get_text(text_id)
        if text is not None:
            self.send_add_text_message(text_id, text)

        return text_id

    def get_text(self, text_id):
        if text_id is None:
            raise UnboundLocalError("text_id required, not defined")

        return self.text_manager.get_text(text_id)

    def get_gui_id(self):
        gui_id = self.__gui_id
        self.__gui_id = self.__gui_id + 1
        return gui_id

    def add_event_handler(self, event_name, handler):
        self.__event_handler_list[event_name] = handler

    def add(self, component):
        """Make a message to add the component."""

        if component.added_to_hicp is not None:
            # Already added. Maybe update instead.
            self.logger.debug("Alread added component") # debug
            if self == component.added_to_hicp:
                # Component was added to this HICP object, it can be
                # updated.
                self.logger.debug("about to update instead") # debug
                self.update(component)
            return

        component.added_to_hicp = self
        component.component_id = self.get_gui_id()

        # Add component to list first - when the other end gets it, it
        # might send an event right away, and this side should be ready for
        # processing without delay.
        self.__component_list[str(component.component_id)] = component

        message = Message()
        message.set_type(Message.COMMAND, Message.ADD)

        component.fill_headers_add(message)
        component.changed_header_list.clear()

        self.__write_thread.write(message)

    def update(self, component):
        """Normally, the component's update method is called, then it calls this method."""

        if component.added_to_hicp is None:
            # Not added yet. Only happens if added_to_hicp.update(component) is
            # called, component.update() can't call this because it has
            # no hicp component to pass that call to.
            self.add(component)
        else:
            if self != component.added_to_hicp:
                # Component was added to different HICP object, it
                # cannot be updated.
                self.logger.debug("Component has different hicp value.") # debug
                return

        header_list = component.changed_header_list

        if 0 == len(header_list):
            # There are no modified fields.
            self.logger.debug("No modified fields.") # debug
            return

        message = Message()
        message.set_type(Message.COMMAND, Message.MODIFY)

        component.fill_headers_modify(message)

        # Add all changed headers.
        for header_key in list(header_list.keys()):
            message.add_header(header_key, header_list[header_key])

        self.__write_thread.write(message)

        component.changed_header_list.clear()

    def remove(self, component):
        self.logger.debug("Remove component.") # debug
        if component.added_to_hicp is None:
            # Not added yet, so can't remove.
            self.logger.debug("Can't remove component, has not been added.") # debug
            return

        if self != component.added_to_hicp:
            # Component was added to different HICP object, it
            # cannot be updated.
            self.logger.debug("Component has different hicp value.") # debug
            return

        message = Message()
        message.set_type(Message.COMMAND, Message.REMOVE)
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(component.component_id))

        self.__write_thread.write(message)
        self.logger.debug("sent Remove component message.") # debug

        # If there were changed headers, they don't matter now.
        component.changed_header_list.clear()

        self.logger.debug("Cleared header list.") # debug

        # Remove from component list.
        del self.__component_list[str(component.component_id)]

        self.logger.debug("Removed component from component list.") # debug

    def text_direction(self, first_direction, second_direction):
        message = Message()
        message.set_type(Message.COMMAND, Message.MODIFY)
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(
            Message.TEXT_DIRECTION,
            first_direction + "," + second_direction
        )
        self.__write_thread.write(message)

    def disconnect(self):
        self.__event_thread.disconnect()
        self.logger.debug("hicp.disconnect() done") # debug

