import importlib
import inspect
import multiprocessing
import os
import os.path
import pathlib
import pkgutil
import queue
import sys
import threading

from datetime import datetime, timedelta
from enum import Enum, auto

# Can't import App, AppSpec from here. Specify them as hicp.app.App etc.
import hicp.app
from hicp.logger import newLogger
from hicp.message import Message

class Authenticator:
    "A simple authenticator, uses plain text file with 'user, password' lines"

    def __init__(self, user_password_path):
        self.__user_password = {}
        self.__logger = newLogger(type(self).__name__)

        user_password_file = open(user_password_path, "r")
        for user_password_line in user_password_file:
            # split on comma
            user_password_split = user_password_line.find(",")
            # Ignore lines in wrong format.
            if 0 < user_password_split:
                user = user_password_line[0:user_password_split].strip()
                password = user_password_line[user_password_split + 1:].strip()
                self.__user_password[user] = password
            else:
                self.__logger.error("Wrong format in file: " + user_password_line)

    def authenticate(self, message):
        method = message.get_header(Message.METHOD)
        if method is None:
            # No authentication method, fails.
            return False

        if method != Message.PLAIN:
            # This only handles plain passwords.
            return False

        check_user = message.get_header(Message.USER)
        check_password = message.get_header(Message.PASSWORD)

        try:
            if check_password == self.__user_password[check_user]:
                return True
            else:
                return False
        except KeyError:
            # No such user
            return False

    def get_methods(self):
        return ["plain"]


def find_free_id(map_to_search):
    ids = map_to_search.keys()
    if 0 == len(ids):
        # Nothing, any ID will do.
        return 0
    sorted_ids = sorted(ids)

    # Start at lowest key value
    prev_id = sorted_ids[0]
    for this_id in sorted_ids:
        if (this_id - prev_id) > 1:
            # There was a gap in keys
            return prev_id + 1
        prev_id = this_id
    # No gap found, next key is end of list
    return prev_id + 1

class EventType(Enum):
  # Message event types.
  AUTHENTICATE = (auto(), Message.AUTHENTICATE, False)
  CHANGED = (auto(), Message.CHANGED, True)
  CLICK = (auto(), Message.CLICK, True)
  CLOSE = (auto(), Message.CLOSE, True)
  CONNECT = (auto(), Message.CONNECT, False)
  DISCONNECT = (auto(), Message.DISCONNECT, False)
  # Non message event types.
  TIME = (auto(), "TIME", False)

  def __init__(self, event_id, event_name="", from_component=False):
    self.event_id = event_id
    self.event_name = event_name
    self.from_component = from_component

  @classmethod
  def get_by_name(cls, event_name):
    for _, event_type in cls.__members__.items():
      if event_type.event_name == event_name:
        return event_type
    return None

class Event():
    # Also change to an enum?
    STAGE_FEEDBACK = 1
    STAGE_PROCESS = 2
    STAGE_UPDATE = 3

    def __init__(self, source):
        self.stage = Event.STAGE_FEEDBACK
        self.message = None
        self.event_type = None
        self.component = None
        self.handler = None
        self.event_time = datetime.now()

        if isinstance(source, EventType):
            self.eventType = source
        elif isinstance(source, Message):
            if source.disconnected:
                # No event message received, but treat it as DISCONNECT
                self.event_type = EventType.DISCONNECT
            else:
                self.message = source
                self.event_type = EventType.get_by_name(source.get_type_value())
        else:
            raise TypeError('Event parameter not EventType or Message: ' + str(source))

    def set_handler(self, handler):
        self.handler = handler

    def set_event_time(self, event_time):
        self.event_time = event_time


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
                # End of messages to write.
                return

            message.write(self.out_stream)


class ReadThread(threading.Thread):
    def __init__(
        self,
        in_stream,
        component_list,
        event_thread,
        time_thread):

        self.in_stream = in_stream
        # This is updated by hicp, so only read from it, no iterating.
        self.component_list = component_list
        self.event_thread = event_thread
        self.time_thread = time_thread

        self.logger = newLogger(type(self).__name__)
        self.disconnect_handler = None

        threading.Thread.__init__(self)

    def run(self):
        is_disconnect = False

        while not is_disconnect:
            message = Message(in_stream=self.in_stream)

            if message.get_type() != Message.EVENT:
                # Ignore all non-event messages
                continue

            is_disconnect = self.got_event_msg(message)

    def got_event_msg(self, message):
        event = Event(message)

        if event.event_type.from_component:
            # Find the component ID.
            component_id = event.message.get_header(Message.ID)
            if component_id is None:
                # Message missing something, can't use this event.
                return False

            try:
                event.component = self.component_list[component_id]
            except KeyError:
                # Component not found, so can't do anything with event.
                # May have sent remove message, but didn't work for some
                # reason and other side thinks it's still there, but may be
                # some other reason, so don't try to "fix" it.
                self.logger.debug("Event for unknown component id " + component_id)
                return False

            event.set_handler(event.component.get_handler(event))

        else:
            if EventType.DISCONNECT == event.event_type:
                handler = self.disconnect_handler
                try:
                    if handler is not None and handler.process is not None:
                        event.set_handler(handler)

                except AttributeError:
                    # No process handler method, so don't use.
                    self.logger.debug("disconnect event handler has no process method")
                    pass

        self.event_thread.add(event)

        if EventType.DISCONNECT == event.event_type:
            # Pass through to time thread so it knows to stop.
            self.time_thread.add(event)
            return True

        return False

    def set_disconnect_handler(self, handler):
        self.disconnect_handler = handler


class TimeHandlerInfo:
    def __init__(self, time_info, is_repeating = False):
        if isinstance(time_info, int):
            # time is in seconds, and can be repeating.
            self.seconds = time_info
            self.seconds_delta = timedelta(seconds=self.seconds)

            self.expected_time = datetime.now() + self.seconds_delta

            self.is_repeating = is_repeating

        elif isinstance(time_info, timedelta):
            # time is a delta, and can be repeating.
            self.seconds_delta = time_info
            self.seconds = self.seconds_delta.total_seconds()

            self.expected_time = datetime.now() + self.seconds_delta

            self.is_repeating = is_repeating

        elif isinstance(time_info, datetime):
            # Specific time, time doesn't repeat.
            self.expected_time = time_info
            self.is_repeating = False

        else:
            # Unrecognized info.
            raise TypeError('Time info parameter not int (seconds) or datetime: ' + str(time_info))

class TimeHandler:
    def get_info(self):
        """Actual handler overrides this and returns a TimeHandlerInfo
        indicating when this event should occur."""
        return None

class TimeThread(threading.Thread):
    def __init__(
        self,
        event_thread):

        self.event_thread = event_thread

        self.logger = newLogger(type(self).__name__)

        self.time_queue = queue.Queue()
        self.handler_list = {}

        threading.Thread.__init__(self)

    def add(self, event):
        self.time_queue.put(event)

    def run(self):
        while True:
            # Scan handler list for timeout to use.
            # Only looking for timeout here, list updates come after .get().
            timeout = None
            timeout_delta = None
            now = datetime.now()
            for handler_id, handler in self.handler_list.items():
                handler_info = handler.get_info()
                expected_time = handler_info.expected_time
                if expected_time > now:
                    new_delta = expected_time - now
                    if timeout_delta is None or new_delta < timeout_delta:
                        timeout_delta = new_delta

            if timeout_delta is not None:
                timeout = timeout_delta.total_seconds()
            try:
                event = self.time_queue.get(timeout=timeout)

                if EventType.TIME == event.event_type:
                    pass
                elif EventType.DISCONNECT == event.event_type:
                    break
                else:
                    # Unrecognized event type? Ignore for now
                    self.logger.debug(
                        "Unrecognized event sent to TimeThread: "
                        + str(event.event_type))

            except queue.Empty:
                # Timeout happens here. Send timeout event to event thread for
                # every handler whose expected time is in the past.
                handlers_to_remove = []  # List of handler ids.
                now = datetime.now()
                for handler_id, handler in self.handler_list.items():
                    handler_info = handler.get_info()
                    expected_time = handler_info.expected_time
                    if expected_time <= now:
                        new_event = Event(EventType.TIME)
                        new_event.set_handler(handler)
                        new_event.set_event_time(expected_time)

                        self.event_thread.add(new_event)

                        if handler_info.is_repeating:
                            # This repeats, update expected time.
                            handler_info.expected_time = \
                                handler_info.expected_time + \
                                handler_info.seconds_delta
                        else:
                            # Non repeating handlers should be removed.
                            handlers_to_remove.append(handler_id)

                for handler_id in handlers_to_remove:
                    del self.handler_list[handler_id]

                pass

    def add_handler(self, handler):
        """Add a time handler, return an ID that can be used to delete the
        handler later."""

        if not isinstance(handler, TimeHandler):
            raise TypeError('Time handler not instance of TimeHandler: ' + str(handler))
        if handler.get_info() is None:
            # No handling info, nothing to handle
            return

        handler_id = find_free_id(self.handler_list)
        self.handler_list[handler_id] = handler

        # Insert empty pseudo event to input queue to re-start timeout.
        self.add(Event(EventType.TIME))
        return handler_id

    def del_handler(self, handler_id):
        del self.handler_list[handler_id]

        # Insert empty pseudo event to input queue to re-start timeout.
        self.add(Event(EventType.TIME))


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

            self.event_process(event)

            # Disconnect events don't use update.
            # Also that thread should have stopped due to the disconnect event
            # by now.
            if EventType.DISCONNECT == event.event_type:
                self.logger.debug("Process thread end of file input")
                break

            self.event_move_to_update(event)

        self.logger.debug("Process thread end of loop")

    def event_process(self, event):
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
            pass
        except TypeError:
            # Process handler has wrong number of arguments.
            self.logger.debug("event process handler has wrong number of args")

    def event_move_to_update(self, event):
        # If an update handler method exists, the event stage is
        # updated and the event is returned to this thread.
        try:
            if event.handler.update is not None:
                event.stage = Event.STAGE_UPDATE
                self.event_thread.add(event)

        except AttributeError:
            # No process handler method, not required.
            self.logger.debug("event handler has no update method")
            pass


class EventThread(threading.Thread):
    def __init__(
        self,
        hicp,
        write_thread,
        default_app,
        app_list=None,
        authenticator=None):

        self.logger = newLogger(type(self).__name__)

        self.hicp = hicp
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
        while True:
            event = self.event_queue.get()

            event_type = event.event_type

            if STATE_WAIT_CONNECT == state:
                if EventType.CONNECT == event_type:
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
                if EventType.DISCONNECT == event.event_type:
                    state = STATE_WAIT_CONNECT

                elif EventType.AUTHENTICATE == event_type:
                    # Check authentication
                    if self.authenticator.authenticate(event.message):
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
                            # Not connecting, but no longer authenticating,
                            # change to running until reuqested disconnect
                            # happens.
                            state = STATE_RUNNING
                else:
                    # Ignore any other messages.
                    pass
            elif STATE_RUNNING == state :
                if EventType.DISCONNECT == event.event_type:
                    # No longer running, wait for next connection.
                    state = STATE_WAIT_CONNECT

                    if self.suspend_app:
                        # Ignore all app events - app is being shutdown.
                        pass
                    elif event.handler is not None:
                        event.stage = Event.STAGE_PROCESS

                    # Disconnect events always pass through to stop all threads.
                    self.process_thread.add(event)

                elif EventType.AUTHENTICATE == event_type:
                    # Don't need authentication now, ignore (might be
                    # extra).
                    pass
                else:
                    # Event with handler
                    if self.suspend_app:
                        # Ignore all app events - app is being shutdown.
                        pass
                    elif Event.STAGE_FEEDBACK == event.stage:
                        self.event_feedback(event)

                    elif Event.STAGE_UPDATE == event.stage:
                        self.event_update(event)
                    else:
                        # Shouldn't happen. Maybe log it?
                        self.logger.debug("Unidentified stage: " + str(event.stage))
                        pass
            else:
                # Should never happen.
                self.logger.debug("Invalid state: " + str(state))
                state = STATE_WAIT_CONNECT

            # If diconnected, end of loop.
            if EventType.DISCONNECT == event.event_type:
                break

        # Fix current directory. See start_application() for why.
        os.chdir(self.start_path)

        self.logger.debug("Wait for process thread")
        self.process_thread.join()
        self.logger.debug("Done wait for process thread")

    def authenticate(self):
        message = Message()
        message.set_type(Message.COMMAND, Message.AUTHENTICATE)

        message.add_header(
            Message.METHOD, ', '.join(self.authenticator.get_methods()) )

        self.write_thread.write(message)

    def get_app_name_to_start(self):
        if self.app_list is None:
            # Nothing to start, so done.
            return None

        if self.connect_event is None:
            # Not conected.
            return None

        app_name = self.connect_event.message.get_header(Message.APPLICATION)

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
        # This thread calls "update" handler if it exists after "feedback".
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

def text_id_sorter(e):
    (text, text_id, o) = e
    return text

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
        text_id = find_free_id(self.id_to_selector)
        self.add_text(text_id, text, group, subgroup)
        return text_id

    def add_groups_text_get_id(self, text_group_list):
        """Add a text group list of the form used for TextSelector() -
        [(text, group, subgroup), ...]
        Allows duplicates if alrteady exists."""

        text_selector = TextSelector(text_group_list)

        return self.add_text_selector_get_id(text_selector)

    def add_text_selector_get_id(self, text_selector):
        """Add a TextSelector.
        Allows duplicates if alrteady exists."""

        text_id = find_free_id(self.id_to_selector)
        self.id_to_selector[text_id] = text_selector

        return text_id

    def get_text(self, text_id):
        selector = self.id_to_selector.get(text_id)
        if selector is not None:
            return selector.get_text(self.group, self.subgroup)
        return None

    def sort(self, unsorted_list):
        """Sort the (text_id, object) list based on the string values
        corresponding to the text ids, for the current group and subgroup,
        return as a new list."""
        # Get list with strings for these ids.
        sorting_list = []
        for (text_id, o) in unsorted_list:
            text = self.get_text(text_id)
            if text is not None:
                sorting_list.append((text, text_id, o))
        # Sort
        sorting_list.sort(key=text_id_sorter)

        # Convert back to ID list for return.
        return_list = []
        for (text, text_id, o) in sorting_list:
            return_list.append((text_id, o))

        return return_list

    def keys(self):
        return self.id_to_selector.keys()


class HICP(multiprocessing.Process):
    "HICP control class"

    LEFT = Message.LEFT
    RIGHT = Message.RIGHT
    UP = Message.UP
    DOWN = Message.DOWN

    def __init__(
        self,
        io_socket,
        text_group=None,
        text_subgroup=None):

        # These must be specified for this to work.
        if io_socket is None:
            raise UnboundLocalError("io_socket required, not defined")

        self.logger = newLogger(type(self).__name__)
        self.text_manager = TextManager(text_group, text_subgroup)

        self.io_socket = io_socket

        if text_group is None:
            # Default language. If they don't specify, make it awkward enough
            # so they make the effort.
            # Canadian English: Remember the "our"s, but not the "ise"s.
            text_group = "en"
            text_subgroup = "ca"

        self.__text_group = text_group
        self.__text_subgroup = text_subgroup

        # Things for this object
        self.__gui_id = 0
        self.__component_list = {}

        multiprocessing.Process.__init__(self)

    def _get_app_path(self):
        hicp_path = os.getenv('HICPPATH', default='.')
        app_path = os.path.join(hicp_path, 'apps')
        return app_path

    def _get_default_app_path(self):
        hicp_path = os.getenv('HICPPATH', default='.')
        app_path = os.path.join(hicp_path, 'default_app')
        return app_path

    def find_apps(self):
        """Find and load apps in the app path.

        If source files are changed, restart the server, it's not worth
        reloading modules and tracking down and killing active apps.
        """
        new_app_list = {}
        new_default_app = None

        app_path = self._get_app_path()
        app_dirs_list = \
            [os.path.join(app_path, f) for f
                in os.listdir(app_path)
                if os.path.isdir(os.path.join(app_path, f)) ]

        # Add default app dir to beginning of list, if it exists.
        default_app_path = self._get_default_app_path()
        if os.path.isdir(default_app_path):
            app_dirs_list.insert(0, default_app_path)

        for importer, package_name, _ in pkgutil.iter_modules(app_dirs_list):
            full_package_name = '%s.%s' % (app_path, package_name)
            module_spec = importer.find_spec(package_name)
            module = importlib.util.module_from_spec(module_spec)
            sys.modules[module.__name__] = module
            module_spec.loader.exec_module(module)
            for cls_name, cls in inspect.getmembers(module, inspect.isclass):
                # Filter out imported classes
                if inspect.getmodule(cls) == module:
                    app = None
                    if issubclass(cls, hicp.app.App):
                        module_dirname = os.path.dirname(module_spec.origin)
                        module_path = pathlib.Path(module_dirname)

                        app_name = cls.get_app_name()
                        if module_dirname == default_app_path:
                            new_default_app = app_name

                        new_app_list[app_name] = hicp.app.AppSpec(cls, module_path)

            # Not practical to unload a module with no apps found, just leave
            # it around as garbage.

        self.__app_list = new_app_list

        if new_default_app is not None:
            self.__default_app = new_default_app
        else:
            # None found, default to first app in list - might not be the same
            # each time, so probably won't work normally.
            # Take one iteration of new_app_list (will iterate keys, which are
            # app names).
             self.__default_app = next(iter(new_app_list))

    def run(self):
        self.find_apps()

        f = self.io_socket.makefile(mode='rw', encoding='utf-8', newline='\n')
        # TODO: consolidate these into one stream field,
        # having two is just a leftover from earlier implementation.
        self.in_stream = f
        self.out_stream = f

        self.__write_thread = WriteThread(self.out_stream)
        self.__write_thread.start()

        self.__authenticator = Authenticator(os.path.join(sys.path[0], "users"))

        self.__event_thread = EventThread(
            self,
            self.__write_thread,
            self.__default_app,
            self.__app_list,
            self.__authenticator)
        self.__event_thread.start()

        self.__time_thread = TimeThread(self.__event_thread)
        self.__time_thread.start()

        self.__read_thread = ReadThread(
            self.in_stream,
            self.__component_list,
            self.__event_thread,
            self.__time_thread)
        self.__read_thread.start()

        self.__read_thread.join()

        self.__time_thread.join()

        self.__event_thread.join()

        # Stop write thread.
        self.__write_thread.write(None)
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


    def add_text_selector_get_id(self, text_selector):
        if text_selector is None:
            raise UnboundLocalError("text_selector required, not defined")

        text_id = self.text_manager.add_text_selector_get_id(text_selector) 

        # Send text down if there is one for current group and subgroup.
        text = self.text_manager.get_text(text_id)
        if text is not None:
            self.send_add_text_message(text_id, text)

        return text_id

    def get_text(self, text_id):
        if text_id is None:
            raise UnboundLocalError("text_id required, not defined")

        return self.text_manager.get_text(text_id)

    def sort(self, unsorted_list):
        if unsorted_list is None:
            raise UnboundLocalError("unsorted_list required, not defined")

        return self.text_manager.sort(unsorted_list)

    def set_disconnect_handler(self, handler):
        if handler.process is None:
            raise AttributeError("Handler is missing process() method.")
        # feedback or update methods are ignored.

        self.__read_thread.set_disconnect_handler(handler)

    def add_time_handler(self, handler):
        return self.__time_thread.add_handler(handler)

    def del_time_handler(self, handler_id):
        self.__time_thread.del_handler(handler_id)

    def get_gui_id(self):
        gui_id = self.__gui_id
        self.__gui_id = self.__gui_id + 1
        return gui_id

    def add(self, component):
        """Make a message to add the component, and send it to the write thread."""

#        if component.added_to_hicp is not None:
        if component.is_added():
            # Already added. Maybe update instead.
            self.logger.debug("Alread added component") # debug
            if self == component.hicp:
                # Component was added to this HICP object, it can be
                # updated.
                self.logger.debug("about to update instead") # debug
                self.update(component)
            return

        component.hicp = self
        component.component_id = self.get_gui_id()

        # Add component to list first - when the other end gets it, it
        # might send an event right away, and this side should be ready for
        # processing without delay.
        self.__component_list[str(component.component_id)] = component

        message = Message()
        message.set_type(Message.COMMAND, Message.ADD)

        component.fill_headers_add(message)

        self.__write_thread.write(message)

        component.notify_sent()

    def update(self, component):
        """Normally, the component's update method is called, then it calls this method."""

#        if component.added_to_hicp is None:
        if not component.is_added():
            # Not added yet. Only happens if hicp.update(component) is
            # called, component.update() can't call this because it has
            # no hicp component to pass that call to.
            self.add(component)
        else:
            if self != component.hicp:
                # Component was added to different HICP object, it
                # cannot be updated.
                self.logger.debug("Component has different hicp value.") # debug
                return

        message = Message()
        message.set_type(Message.COMMAND, Message.MODIFY)

        component.fill_headers_modify(message)

        self.__write_thread.write(message)

        component.notify_sent()

    def remove(self, component):
#        if component.added_to_hicp is None:
        if not component.is_added():
            # Not added yet, so can't remove.
            self.logger.debug("Can't remove component, has not been added.") # debug
            return

        if self != component.hicp:
            # Component was added to different HICP object, it
            # cannot be updated.
            self.logger.debug("Component has different hicp value.") # debug
            return

        message = Message()
        message.set_type(Message.COMMAND, Message.REMOVE)
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(component.component_id))

        self.__write_thread.write(message)

        # Remove from component list.
        del self.__component_list[str(component.component_id)]

    def fake_event(self, event_msg, component_id=None):
        # If no component_id, assume event already has that set.
        if component_id is not None:
            event_msg.add_header(Message.ID, component_id)

        # Insert the event message into the queue.
        self.__read_thread.got_event_msg(event_msg)

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

