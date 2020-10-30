import os
import os.path
import queue
import re
import sys
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

    def log(self, msg):
        self.logger.debug(msg)


class ReadThread(threading.Thread):
    def __init__(self, in_stream, event_thread):
        self.in_stream = in_stream
        self.event_thread = event_thread

        self.logger = newLogger(type(self).__name__)

        threading.Thread.__init__(self)

    def run(self):
        self.logger.debug("Read thread started")  # debug

        while True:
            event = Event(in_stream=self.in_stream)

            self.event_thread.add(event)

            if event.disconnected:
                self.logger.debug("ReadThread End of file input")
                break

        self.logger.debug("Read thread ended")  # debug


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


    def log(self, msg):
        self.logger.debug(msg)


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

        self.hicp = hicp
        self.component_list = component_list
        self.event_handler_list = event_handler_list
        self.write_thread = write_thread
        self.default_app = default_app

        self.logger = newLogger(type(self).__name__)
        self.app_list = app_list
        self.authenticator = authenticator

        self.event_queue = queue.Queue()

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
            self.logger.debug("EventThread about to get Event")  # debug
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
                    connect_event = event

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
                        self.start_application(event)
                        state = STATE_RUNNING
                    else:
                        # Can the default application do
                        # authentication?
                        try:
                            self.default_app.authenticate()
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
                    if Event.STAGE_FEEDBACK == event.stage:
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
                                self.logger.debug("click no handler found")  # debug
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
                                self.logger.debug("changed no handler found")  # debug
                                event.handler = None

                        if event.handler is not None:
                            # Handler was added to event message.
                            self.event_feedback(event)

                    elif Event.STAGE_UPDATE == event.stage:
                        # event already has component and event fields,
                        # just call event_update()
                        self.logger.debug("Update stage: " + str(event.stage))
                        self.event_update(event)
                        pass
                    else:
                        # Shouldn't happen. Maybe log it?
                        self.logger.debug("Unidentified stage: " + str(event.stage))
                        pass
            else:
                # Should never happen.
                self.logger.debug("Invalid state: " + str(state))
                state = STATE_WAIT_CONNECT

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

    def start_application(self, event):
        # select app and start running.
        application = self.default_app
        if self.app_list is not None:
            app_name = event.get_header(Message.APPLICATION)
            if app_name is not None:
                # User wants specific app.
                try:
                    application = self.app_list[app_name]
                except KeyError:
                    # App not found, use default application.
                    pass

        # Notify application that it's connected so it can send messages
        # to define the user interface.
        application.connected(self.hicp)

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

    def log(self, msg):
        self.logger.debug(msg)


class TextManagerGroup:
    "Contains id to string mapping for a text manager group"

    def __init__(self):
        self.logger = newLogger(type(self).__name__)

        # Doesn't need to know what group it's in, just the text info.
        self.id_to_text = {}
        self.text_to_id = {}

    def add_text(self, text_id, text):
        self.text_to_id[text] = text_id
        self.id_to_text[text_id] = text

    def find_free_id(self):
        keys = self.id_to_text.keys()
        if 0 == len(keys):
            # Nothing, any ID will do.
            return 0
        ks = sorted(keys)

        # Start at lowest key value
        prev_key = ks[0]
        for key in ks:
            if (key - prev_key) > 1:
                # There was a gap in keys
                return prev_key + 1
            prev_key = key
        # No gap found, next key is end of list
        return prev_key + 1

    def add_text_get_id(self, text: str):
        if text in self.text_to_id:
            # Already there.
            return self.text_to_id[text]

        free_id = self.find_free_id()
        self.add_text(free_id, text)
        return free_id

    def get_id(self, text: str):
        # None if not there.
        return self.text_to_id.get(text)

    def get_text(self, text_id):
        # None if not there.
        return self.id_to_text.get(text_id)

    def get_all_text(self):
        "Return a copy of id/text dictionary."
        return self.id_to_text.copy()

class TextManager:
    "Manage text groups, IDs, and strings"

    def __init__(self, current_group: str):
        if current_group is None or '' == current_group:
            raise UnboundLocalError("current_group required, not defined")

        self.groups: Dict[str, TextManagerGroup] = {}
        self.deault_group = None

        self.set_group(current_group)

    def get_group(self, group: str = None):
        if group is None:
            group = self.current_group

        if group in self.groups:
            return self.groups[group]

        tmg = TextManagerGroup()
        self.groups[group] = tmg
        return tmg

    def set_group(self, new_group: str):
        if new_group is None or '' == new_group:
            raise UnboundLocalError("group required and not '', not defined")

        # Creates group as side effect.
        self.get_group(new_group)
        self.current_group = new_group

    def add_text(self, text_id, text, group: str = None):
        self.get_group(group).add_text(text_id, text)

    def add_text_get_id(self, text, group: str = None):
        return self.get_group(group).add_text_get_id(text)

    def get_text(self, text_id, group: str = None):
        return self.get_group(group).get_text(text_id)

    def get_id(self, text, group=None):
        return self.get_group(group).get_id(text)

    def get_all_text(self, group:str = None):
        "Return a dict of id/text that can be passed to HICP.add_all_text()"
        return self.get_group(group).get_all_text()


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
        default_app,
        app_list=None,
        text_group=None,
        authenticator=None):

        # These must be specified for this to work.
        if in_stream is None:
            raise UnboundLocalError("in_stream required, not defined")

        if out_stream is None:
            raise UnboundLocalError("out_stream required, not defined")

        if default_app is None:
            raise UnboundLocalError("default_app required, not defined")

        self.logger = newLogger(type(self).__name__)

        self.in_stream = in_stream
        self.out_stream = out_stream
        self.__default_app = default_app
        self.__app_list = app_list
        if text_group is None:
            # Default language. If they don't specify, make it awkward enough
            # so they make the effort.
            # Canadian English: Remember the "our"s, but not the "ise"s.
            text_group = "en-ca"
        self.__text_group = text_group
        self.text_manager = TextManager(text_group)
        self.__authenticator = authenticator

    def start(self):
        # Things for this object
        # TODO: Maybe should be in __init__()?
        self.__gui_id = 0
        self.__component_list = {}
        self.__event_handler_list = {}

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

    def set_text_group(self, text_group):
        "Define what text group to use."
        if text_group != self.__text_group:
            self.__text_group = text_group
            self.text_manager.set_group(text_group)

            # Group is changed, send all text from new group to client.
            for text_id, text in self.text_manager.get_all_text().items():
                self.send_add_text_message(text_id, text)

    def add_all_text(self, text_dict):
        # Add each id/string entry in the dictionary
        for text_id, text in text_dict.items():
            self.add_text(text_id, text)

    def add_text(self, text_id, text_string):
        if text_id is None:
            raise UnboundLocalError("text_id required, not defined")

        if text_string is None:
            raise UnboundLocalError("text_string required, not defined")

        self.send_add_text_message(text_id, text_string)
        self.text_manager.add_text(text_id, text_string)

    def send_add_text_message(self, text_id, text_string):
        message = Message()
        message.set_type(Message.COMMAND, Message.ADD)

        message.add_header(Message.CATEGORY, Message.TEXT)
        message.add_header(Message.ID, str(text_id))
        message.add_header(Message.TEXT, text_string)

        self.__write_thread.write(message)

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
        component.id = self.get_gui_id()

        # Add component to list first - when the other end gets it, it
        # might send an event right away, and this side should be ready for
        # processing without delay.
        self.__component_list[str(component.id)] = component

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
        message.add_header(Message.ID, str(component.id))

        self.__write_thread.write(message)
        self.logger.debug("sent Remove component message.") # debug

        # If there were changed headers, they don't matter now.
        component.changed_header_list.clear()

        self.logger.debug("Cleared header list.") # debug

        # Remove from component list.
        del self.__component_list[str(component.id)]

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
        self.logger.debug("hicp.disconnect() entered") # debug
        self.__event_thread.disconnect()
        self.logger.debug("hicp.disconnect() done") # debug

    def log(self, msg):
        self.logger.debug(msg)

