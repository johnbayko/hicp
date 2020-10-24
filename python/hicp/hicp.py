import logging
import os
import os.path
import queue
import re
import sys
import threading

def newLogger(name):
    logger = logging.getLogger(__name__ + '.' + name)

    if not logger.hasHandlers():
        lf = logging.Formatter('%(name)s:%(funcName)s %(lineno)d: %(message)s')

        lh = logging.FileHandler('reception.log')
        lh.setFormatter(lf)

        logger.addHandler(lh)
        logger.setLevel(logging.DEBUG)

    return logger

class Message:
    # Constants
    EVENT = "event"
    COMMAND = "command"

    # Events
    CHANGED = "changed"
    CLICK = "click"
    CLOSE = "close"

    # Commands
    ADD = "add"
    MODIFY = "modify"
    REMOVE = "remove"

    # Events and commands
    AUTHENTICATE = "authenticate"
    CONNECT = "connect"
    DISCONNECT = "disconnect"

    # Attributes
    ATTRIBUTES = "attributes"
    APPLICATION = "application"
    CATEGORY = "category"
    CONTENT = "content"
    EDITING = "editing"
    GUI = "gui"
    ID = "id"
    METHOD = "method"
    PARENT = "parent"
    PASSWORD = "password"
    PLAIN = "plain"
    POSITION = "position"
    SIZE = "size"
    TEXT = "text"
    TEXT_DIRECTION = "text-direction"
    USER = "user"
    VISIBLE = "visible"

# These should be user visible. Move to Component or TextField (or parent,
# when TextArea is added).
    # ATTRIBUTES attributes
    # CONTENT - already defined.
    BOLD = "bold"
    FONT = "font"
    ITALIC = "italic"
    LAYOUT = "layout"
    SIZE = "size"
    UNDERLINE = "underline"

    # ATTRIBUTES FONT attributes
    SERIF = "serif"
    SANS_SERIF = "sans-serif"
    SERIF_FIXED = "serif-fixed"
    SANS_SERIF_FIXED = "sans-serif-fixed"

    # ATTRIBUTES LAYOUT attributes
    BLOCK = "block"
    INDENT_FIRST = "indent-first"
    INDENT_REST = "indent-rest"
    LIST = "list"

    # EDITING attributes
    ENABLED = "enabled"
    DISABLED = "disabled"
    SERVER = "server"

    # TEXT_DIRECTION atributes
    LEFT = "left"
    RIGHT = "right"
    UP = "up"
    DOWN = "down"

    LENGTH_RE = re.compile("(length) *= *(\d+)")
    BOUNDARY_RE = re.compile("(boundary) *=(.*)\r\n")
    ESC_RE = re.compile("(\033)(.)")
    LAST_ESC_RE = re.compile("(\033.[^\033]*)*(\033.)")

    def __init__(self, in_stream=None):
        # These may be null.
        self.logger = newLogger(type(self).__name__)

        self.disconnected = False

        # May be EVENT or COMMAND.
        self.__type = None

        # "event: " or "command: " value.
        self.__type_value = None

        self.__headers = {}

        if in_stream is None:
            # No message to read in - probably making one to send.
            return

        # Read headers into the header dict.
        line_cnt = 0
        try:
            while True:
                line = self.readline(in_stream)
                self.logger.debug("Read: " + line)  # debug

                if line == "\r\n" or line == "":
                    # End of message or EOF.
                    if 0 >= line_cnt:
                        # Message not even started, nothing to read.
                        # TODO: Not being handled correctly, return a disconnect event instead.
                        self.disconnected = True
                    break

                line_cnt = line_cnt + 1

                # Split into key and value.
                header_key = ""
                header_value = ""

                # Check for ":: " multi-line data section.
                header_split_idx = line.find(":: ")
                if 0 < header_split_idx:
                    self.logger.debug("data section")  # debug

                    header_key = line[0:header_split_idx]
                    # Skip ":: " 3 chracters.
                    termination_criterion = line[header_split_idx + 3:]

                    # Length or boundary termination criterion?
                    if 0 <= termination_criterion.find("length"):
                        length_match = LENGTH_RE.search(termination_criterion)
                        length = int(length_match.group(2), 10)

                        # header_value is the next length bytes (unless
                        # EOF is encountered).
                        header_value = in_stream.read(length)

                        # There must be a terminating CR LF, so read to
                        # the end of the input line (extra is discarded).
                        self.readline(in_stream)

                    elif 0 <= termination_criterion.find("boundary"):
                        boundary_match = BOUNDARY_RE.search(termination_criterion)
                        boundary = boundary_match.group(2)

                        # Boundary is either CR LF <text> or <text>.
                        if '' == boundary:
                            # Boundary is CR LF plus next line.
                            # The boundary excludes the end CR LF in the
                            # string returned by readline(), but it's
                            # easier to find the boundary if they are
                            # included.
#                        boundary = "\r\n" + self.readline(in_stream)
                            boundary = self.readline(in_stream)
                            full_line = True
                        else:
                            full_line = False

                        # Read until boundary + CR LF is read.
                        header_value_list = []
                        found_boundary = False
                        prev_last_esc_affects_eol = False
                        while 1:
                            header_value_part = self.readline(in_stream)
                            # Remove any escapes, but keep track of
                            # index of last one.
                            (header_value_unescaped, esc_cnt) = \
                                ESC_RE.subn("\\2", header_value_part)
                            if 0 < esc_cnt:
                                # String had escapes.
                                last_esc_match = \
                                    LAST_ESC_RE.search(header_value_part)
                                after_last_esc_index = last_esc_match.end(2)
                            else:
                                # No esc in string.
                                after_last_esc_index = 0

                            if full_line:
                                if header_value_part == boundary \
                                    and False == prev_last_esc_affects_eol:

                                    boundary_index = 0
                                else:
                                    boundary_index = -1
                            else:
                                boundary_index = header_value_part.find(boundary)

                            if 0 <= boundary_index:
                                # Make sure last escape did not affect
                                # boundary. If it did, it's not the
                                # boundary we're looking for. 
                                if prev_last_esc_affects_eol \
                                    and after_last_esc_index <= boundary_index:

                                    found_boundary = True

                                    # Remove the boundary part from
                                    # unescaped header value. Remember
                                    # this boundary string includes the
                                    # terminating CR LF.
                                    header_value_unescaped = \
                                        header_value_unescaped[ : -len(boundary)]

                            # Add to list.
                            header_value_list = header_value_list + header_value_part
                            if found_boundary:
                                break
                            else:
                                # If sequence is:
                                # ESC CR LF
                                # CR ESC LF
                                # CR LF ESC
                                # ...then this prevents a full line boundary
                                # match next time.
                                if after_last_esc_index >= len(header_value_part) - 2:
                                    prev_last_esc_affects_eol = True
                                else:
                                    prev_last_esc_affects_eol = False

                        # convert list to single string.
                        header_value = ''.join(header_value_list)

                    else:
                        # No termination criterion. Leave header value
                        # blank.
                        pass
                else:
                    # Check for ": " single line data value.
                    header_split_idx = line.find(": ")
                    if 0 < header_split_idx:
                        # This is a valid header.
                        header_key = line[:header_split_idx]

                        # Skip ": " 2 chracters, and omit CR LF at the end.
                        header_value = line[header_split_idx + 2:-2]
                    else:
                        # No ": " or ":: ". Let header key be input line
                        # (without CR LF) and leave value as "".
                        header_key = line[:-2]

                if header_key:
                    if 1 == line_cnt:
                        # First line always "event: " or "command: "
                        self.set_type(header_key, header_value)
                    else:
                        self.add_header(header_key, header_value)
                else:
                    # Ignore non headers. Maybe log an error in the
                    # future.
                    self.logger.debug("non-header")  # debug
                    pass
        except ConnectionResetError:
            # Connection closed, interpret as diconnect event.
            self.logger.debug("ConnectionResetError")  # debug
            self.set_type(self.EVENT, self.DISCONNECT)

    # Read until CR LF is found.
    def readline(self, in_stream):
        # Some systems may stop at CR (or LF), doesn't guarantee next is
        # LF (or previous is CR), so loop until both are read.
        line = ""
        while True:
            line_part = in_stream.readline()
            if not line_part:
                # EOF
                break

            if line_part[-1] == "\x0d":
                # Ended at CR, try to get a LF as well. At EOF, read
                # will return "", and this will loop back up to the
                # readline above where the EOF will be detected.
                line = line + line_part + in_stream.read(1)
            else:
                line = line + line_part

            if line[-2] == "\x0d" and  line[-1] == "\x0a":
                # Found a CR LF combination, this is the end of the line.
                break

        return line

    def write(self, out_stream):
        if out_stream is None:
            raise UnboundLocalError("out_stream required, not defined")
        self.logger.debug("Write type " + self.__type + ": " + self.__type_value) # debug
        out_stream.write(self.__type + ": " + self.__type_value + "\r\n")

        # Write all headers
        for header_key in list(self.__headers.keys()):
            header_value = self.__headers[header_key]
            self.logger.debug("header " + header_key + ": " + header_value) # debug
            # If value has "\r\n" within it, output as data block,
            # otherwise as simple header.
            if -1 == header_value.find("\r\n"):
                # Simple header field.
                out_stream.write(header_key + ": " + header_value + "\r\n")
            else:
                # Data block.
                out_stream.write(header_key + ":: boundary=\r\n--\r\n")

                # Escape each occurrence by splitting string with "\r\n--",
                # write out each with ESC prior to "\r\n--".
                header_value_list = header_value.split("\r\n--")

                # If there is no match, header_value_list will have one entry
                # with original string.
                sep = ""  # Escaped separator between previous and current
                for header_value_part in header_value_list:
                    out_stream.write(sep + header_value_part)
                    sep = "\033\r\n--"

                # Write out terminator sequence and extra "\r\n" as
                # block terminator.
                out_stream.write("\r\n--\r\n")

        # Write end of message blank line
        self.logger.debug(" write:" + "\r\n") # debug
        out_stream.write("\r\n")

        out_stream.flush()

    def set_type(self, type, value):
        self.__type = type
        self.__type_value = value

    def get_type(self):
        return self.__type

    def get_type_value(self):
        return self.__type_value

    def clear(self):
        self.__headers.clear()

    def get_header(self, header_key):
        if header_key is None:
            return None

        try:
            # Header keys are stored as lower case.
            header_value = self.__headers[header_key.lower()]
        except KeyError:
            self.logger.debug("Header not found key: " + header_key) # debug
            for header_key in list(self.__headers.keys()):
                header_value = self.__headers[header_key] # debug
                self.logger.debug("get_header " + header_key + ": " + header_value) # debug
            header_value = None

        return header_value

    def add_header(self, header_key, header_value):
        if not isinstance(header_key, str):
            raise TypeError("header key must be a string")
        if not isinstance(header_value, str):
            raise TypeError("header value must be a string")

        self.logger.debug("Adding header key " + header_key + ":" + header_value) # debug

        # Store keys as lower case.
        self.__headers[header_key.lower()] = header_value

    def log(self, msg):
        self.logger.debug(msg)

class Event(Message):
    STAGE_FEEDBACK = 1
    STAGE_PROCESS = 2
    STAGE_UPDATE = 3

    def __init__(self, in_stream=None):
        Message.__init__(self, in_stream)

        self.stage = Event.STAGE_FEEDBACK
        self.component = None
        self.handler = None

class Component:
    """Components use getters and setters functions, because they need to
keep track of which items have been changed so they can be sent in
"modify" messages when updated."""

    COMPONENT = "component"

    BUTTON = "button"
    LABEL = "label"
    PANEL = "panel"
    TEXTAREA = "textarea"
    TEXTFIELD = "textfield"
    WINDOW = "window"

    def __init__(self):
        self.logger = newLogger(type(self).__name__)

        self.id = None
        self.added_to_hicp = None

        self.changed_header_list = {}

    def set_changed_header(self, header, field):
        # Must be a string - the value that will be put in the update
        # message.
        if not isinstance(field, str):
            raise TypeError("header must be a string as in a message header field")

        self.changed_header_list[header] = field

    def fill_headers_add(self, message):
        self.logger.debug("Component fill add headers: " + Message.GUI + " id " + str(self.id) + " component " + self.component)  # debug
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(self.id))
        message.add_header(self.COMPONENT, self.component)

    def fill_headers_modify(self, message):
        self.logger.debug("Component fill modify headers")
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(self.id))

        # Other fields are in the changed header list, used by update.

    def update(self):
        if self.added_to_hicp is None:
            # No hicp to handle update call.
            return

        self.added_to_hicp.update(self)

    def log(self, msg):
        self.logger.debug(msg)

class ContainedComponent(Component):
    """Contained within a container, needs to keep track of parent and
position."""

    HORIZONTAL = 0
    VERTICAL = 1

    def __init__(self):
        Component.__init__(self)

        self.__parent_id = None  # Number
        self.__position = [None, None] # [Number, Number]
        self.__size = [None, None] # [Number, Number]

    def set_parent(self, component):
        if component is None:
            # No partent component to add to.
            self.logger.debug("ContainedComponent.set_parent() component is null")
            return
        try:
            if component.id is None:
                # No id attribute.
                self.logger.debug("ContainedComponent.set_parent() component has null id attribute")
                return
        except AttributeError:
            # No id attribute at all.
            self.logger.debug("ContainedComponent.set_parent() component has no .id attribute")
            return

        self.__parent_id = component.id
        self.set_changed_header(Message.PARENT, str(self.__parent_id))

    def __position_field(self):
        field = None
        if ( self.__position[ContainedComponent.HORIZONTAL] is not None
          or self.__position[ContainedComponent.VERTICAL] is not None
        ):
            field = ""
            if self.__position[ContainedComponent.HORIZONTAL] is not None:
                field = field + str(self.__position[ContainedComponent.HORIZONTAL])
            field = field + ","
            if self.__position[ContainedComponent.VERTICAL] is not None:
                field = field + str(self.__position[ContainedComponent.VERTICAL])
        return field

    def set_position(self, horizontal, vertical):
        # horizontal and vertical are integers.
        self.__position[ContainedComponent.HORIZONTAL] = horizontal
        self.__position[ContainedComponent.VERTICAL] = vertical

        field = self.__position_field()
        if field is None:
            field = ","
        self.set_changed_header(Message.POSITION, field)

    def __size_field(self):
        field = None
        if ( self.__size[ContainedComponent.HORIZONTAL] is not None
          or self.__size[ContainedComponent.VERTICAL] is not None
        ):
            field = ""
            if self.__size[ContainedComponent.HORIZONTAL] is not None:
                field = field + str(self.__size[ContainedComponent.HORIZONTAL])
            field = field + ","
            if self.__size[ContainedComponent.VERTICAL] is not None:
                field = field + str(self.__size[ContainedComponent.VERTICAL])
        return field

    def set_size(self, horizontal, vertical):
        # horizontal and vertical are integers.
        self.__size[ContainedComponent.HORIZONTAL] = horizontal
        self.__size[ContainedComponent.VERTICAL] = vertical

        field = self.__size_field()
        if field is None:
            field = ","
        self.set_changed_header(Message.SIZE, field)

    def fill_headers_add(self, message):
        Component.fill_headers_add(self, message)
        if self.__parent_id is not None:
            message.add_header(Message.PARENT, str(self.__parent_id))

        field = self.__position_field()
        if field is not None:
            message.add_header(Message.POSITION, field)

        field = self.__size_field()
        if field is not None:
            message.add_header(Message.SIZE, field)


class ComponentText():
    def __init__(self, control):
        self.logger = newLogger(type(self).__name__)

        self.control = control
        self.__text_id = None # Number

    def set_text_id(self, text_id):
        text_id_str = str(text_id)
        if text_id_str != self.__text_id:
            self.__text_id = text_id_str
            self.control.set_changed_header(Message.TEXT, self.__text_id)

    def set_text(self, text, hicp):
        text_id = hicp.text_manager.add_text_get_id(text)
        # Make sure text is added to client before setting here.
        hicp.add_text(text_id, text)
        self.set_text_id(text_id)

    def fill_headers_add(self, message):
        if self.__text_id is not None:
            message.add_header(Message.TEXT, self.__text_id)


class Label(ContainedComponent):
    def __init__(self):
        ContainedComponent.__init__(self)
        self.component = Component.LABEL
        self.component_text = ComponentText(self)

    def set_text_id(self, text_id):
        self.component_text.set_text_id(text_id)

    def set_text(self, text, hicp):
        self.component_text.set_text(text, hicp)

    def fill_headers_add(self, message):
        ContainedComponent.fill_headers_add(self, message)
        self.component_text.fill_headers_add(message)

class Button(ContainedComponent):
    def __init__(self):
        ContainedComponent.__init__(self)
        self.component = Component.BUTTON
        self.component_text = ComponentText(self)

    def set_text_id(self, text_id):
        self.component_text.set_text_id(text_id)

    def set_text(self, text, hicp):
        self.component_text.set_text(text, hicp)

    def fill_headers_add(self, message):
        ContainedComponent.fill_headers_add(self, message)
        self.component_text.fill_headers_add(message)

    def set_handle_click(self, handle_click):
        self.__handle_click = handle_click

    def get_handle_click(self):
        return self.__handle_click

class TextFieldAttribute:
    def __init__(self, length, is_multivalued, value=None):
        self.length = length
        self.is_multivalued = is_multivalued
        self.value = value

class TextField(ContainedComponent):
    # ATTRIBUTES attributes
    # CONTENT - already defined.
    BOLD = Message.BOLD
    FONT = Message.FONT
    ITALIC = Message.ITALIC
    LAYOUT = Message.LAYOUT
    SIZE = Message.SIZE
    UNDERLINE = Message.UNDERLINE

    # ATTRIBUTES FONT attributes
    SERIF = Message.SERIF
    SANS_SERIF = Message.SANS_SERIF
    SERIF_FIXED = Message.SERIF_FIXED
    SANS_SERIF_FIXED = Message.SANS_SERIF_FIXED

    # ATTRIBUTES LAYOUT attributes
    BLOCK = Message.BLOCK
    INDENT_FIRST = Message.INDENT_FIRST
    INDENT_REST = Message.INDENT_REST
    LIST = Message.LIST

    # EDITING attributes
    ENABLED = Message.ENABLED
    DISABLED = Message.DISABLED
    SERVER = Message.SERVER

    CONTENT_INVALID_RE = re.compile("[\\0-\\037]")

    def __init__(self):
        ContainedComponent.__init__(self)

        self.component = Component.TEXTFIELD
        self.__content = ""

        # Maps attribute name to a list of TextFieldAttribute objects.
        # When content is set, all attributes are cleared.
        self.__attribute_map = {}

        # __attributes is the whole attribute list, there's no way to
        # send them separately. However, each attribute can have its own
        # string stored to reduce extra work.
        self.__attribute_string_map = {}

        # Attribute header is string representation of __attribute_map.
        # It's easier to generate it when attributes are set and treat
        # it like any other header string than to detect it when headers
        # are written out and generate it then.
        self.__attributes = ""

#        self.__editing = Message.ENABLED

    def set_content(self, content):
        # Content must be a string.
        content = str(content)

        # For text field, there should be no unprintable characters or
        # EOL sequence.
        # Truncate at first invalid character.
        content_invalid_match = self.CONTENT_INVALID_RE.search(content)
        if content_invalid_match is not None:
            content = content[:content_invalid_match.start(0)]
        self.__old_content = self.__content
        self.__content = content
        self.set_changed_header(Message.CONTENT, self.__content)

        # Clear attributes if there are any.
        if 0 < len(self.__attributes):
            self.__attribute_map.clear()
            self.__attributes = ""
            self.set_changed_header(Message.ATTRIBUTES, self.__attributes)

    def set_attribute(
        self,
        attribute,
        new_attribute_range_start=0,
        new_attribute_range_length=1,
        value=None
    ):
        # Attribute and value (if specified) must be strings.
        attribute = str(attribute)
        if value is not None:
            self.logger.debug("value is not None")  # debug
            is_multivalued = True
            value = str(value)
        else:
            self.logger.debug("value is None")
            is_multivalued = False

        # New attribute range can't start past end of content.
        if len(self.__content) < new_attribute_range_start:
            # Nothing to apply attribute to.
            self.logger.debug("Nothing to apply attribute to.")  # debug
            return

        # For consistancy, end index in all cases is the first index not
        # part of the range.
        new_attribute_range_end = \
            new_attribute_range_start + new_attribute_range_length

        # New attribute range cannot go past end of content - truncate.
        if len(self.__content) < new_attribute_range_end:
            new_attribute_range_end = len(self.__content)
            new_attribute_range_length = \
                new_attribute_range_end - new_attribute_range_start

        # For text field, there should be no unprintable characters, but
        # may include multiple EOL sequences.
        # Should either strip them out or reject this action - deal with
        # that later.

        attribute_list = self.__attribute_map.get(attribute)
        if attribute_list is None:
            # This is a new attribute for this content.
            self.logger.debug("This is a new attribute for this content.")  # debug
            if is_multivalued:
                # Initial value of multivalued attributes is user agent
                # default, indicated by "".
                default_attribute_range = \
                    TextFieldAttribute(len(self.__content), is_multivalued, "")
            else:
                # Initial value of binary attributes is False ("0").
                default_attribute_range = \
                    TextFieldAttribute(len(self.__content), is_multivalued, "0")
            attribute_list = [default_attribute_range]
            self.__attribute_map[attribute] = attribute_list

        # Construct new attribute list from old. Any attributes which
        # don't overlap the new attribute range are copied unchanged,
        # any overlapping start or end of range are truncated, and any
        # within the range are not copied, replaced by the new attribute.
        #
        # A list of the alternatives and desired results:
        #   new:     +---+             +---+
        # 1 old: +-+ :   :         +-+ :   :
        # 2      +---+   :         +---+   :
        # 3      +-----+ :         +---|   :
        # 4      +-------+         +---|   |
        # 5      +---------+       +---|   |-+
        # 6          +--+:             :   :
        # 7          +---+             :   :
        # 8          +-----+           :   |-+
        # 9          :+-+:             :   :
        # 10         :+--+             :   :
        # 11         :+----+           :   |-+
        # 12         :   +-+           :   +-+
        # 13         :   : +-+         :   : +-+
        #
        # For attributes which differ fron the new attribute:
        #   new:     +---+
        # 3 old: +-----+ :      Truncate old range, becomes same as 2
        # 4      +-------+
        #
        #            :   :
        # 8          +-----+    Truncate old range, becomes same as 12.
        # 11         :+----+
        #
        # 1      +-+ :   :      Copy old range unchanged.
        # 2      +---+   :
        # 13         :   : +-+
        #
        # 5      +---------+    Add old range truncated to start of new range.
        #            :   :      Add new range.
        #            :   :      Add old range truncated to end of new range.
        #
        # 6          +--+:      Do nothing.
        # 9          :+-+:
        #
        # 7          +---+      Add new range. Discard old range.
        # 10         :+--+
        #
        # 12         :   +-+    Add new range.
        #            :   :      Copy old range unchanged.
        # For attributes which are the same as the new attribute (new
        # range is not actually added, old range with same value is
        # extended to include new):
        #   new:     +---+
        # 1 old: +-+ :   :      Copy unchanged.
        # 4      +-------+
        # 5      +---------+
        # 8          +-----+
        # 13         :   : +-+
        #
        # 2      +---+   :      Extend new to start of old, discard old.
        # 3      +-----+ :
        #
        # 11         :+----+    Extend old to start of new, discard new,
        # 12         :   +-+    add old.
        #
        # 6          +--+:      Do nothing (discard old).
        # 9          :+-+:
        # 7          +---+
        # 10         :+--+

        # New empty attribute list.
        new_attribute_list = []

        # Find point in attribute list where new attribute applies.
        old_attribute_range_start = 0
        for old_attribute_range in attribute_list:
            old_attribute_range_end = \
                old_attribute_range_start + old_attribute_range.length

            # Determine which alternative we're dealing with here.
            if old_attribute_range_start < new_attribute_range_start:
                if old_attribute_range_end < new_attribute_range_start:
                    range_position = 1
                elif old_attribute_range_end == new_attribute_range_start:
                    range_position = 2
                elif old_attribute_range_end < new_attribute_range_end:
                    range_position = 3
                elif old_attribute_range_end == new_attribute_range_end:
                    range_position = 4
                else:
                    range_position = 5
            elif old_attribute_range_start == new_attribute_range_start:
                if old_attribute_range_end < new_attribute_range_end:
                    range_position = 6
                elif old_attribute_range_end == new_attribute_range_end:
                    range_position = 7
                else:
                    range_position = 8
            elif old_attribute_range_start < new_attribute_range_end:
                if old_attribute_range_end < new_attribute_range_end:
                    range_position = 9
                elif old_attribute_range_end == new_attribute_range_end:
                    range_position = 10
                else:
                    range_position = 11
            elif old_attribute_range_start == new_attribute_range_end:
                range_position = 12
            else:
                range_position = 13
            self.logger.debug("range_position " + str(range_position))  # debug

            if old_attribute_range.value != value:
                # Old attribute differs from new attribute.
                if 3 == range_position or 4 == range_position:
                    # Truncate range, becomes same as 2
                    old_attribute_range = TextFieldAttribute(
                            new_attribute_range_start - old_attribute_range_start,
                            old_attribute_range.is_multivalued,
                            old_attribute_range.value
                        )
                    old_attribute_range_end = new_attribute_range_start
                    range_position = 2

                if 8 == range_position or 11 == range_position:
                    # Truncate range, becomes same as 2
                    old_attribute_range = TextFieldAttribute(
                            old_attribute_range_end - new_attribute_range_end,
                            old_attribute_range.is_multivalued,
                            old_attribute_range.value
                        )
                    old_attribute_range_start = new_attribute_range_end
                    range_position = 12

                if 1 == range_position or 2 == range_position or 13 == range_position:
                    # Copy unchanged.
                    self.logger.debug("Copy unchanged.")  # debug
                    new_attribute_list.append(old_attribute_range)

                if 5 == range_position:
                    # Add range truncated to start of new range.
                    self.logger.debug("Add range truncated to start of new range.")  # debug
                    add_attribute_range = TextFieldAttribute(
                            new_attribute_range_start - old_attribute_range_start,
                            old_attribute_range.is_multivalued,
                            old_attribute_range.value
                        )
                    new_attribute_list.append(add_attribute_range)

                    # Add new range.
                    add_attribute_range = TextFieldAttribute(
                            new_attribute_range_length, is_multivalued, value
                        )
                    new_attribute_list.append(add_attribute_range)

                    # Add range truncated to end of new range.
                    add_attribute_range = TextFieldAttribute(
                            old_attribute_range_end - new_attribute_range_end,
                            old_attribute_range.is_multivalued,
                            old_attribute_range.value
                        )
                    new_attribute_list.append(add_attribute_range)

                if 7 == range_position or 10 == range_position:
                    # Add new range.
                    self.logger.debug("Add new range.")  # debug
                    add_attribute_range = TextFieldAttribute(
                            new_attribute_range_length, is_multivalued, value
                        )
                    new_attribute_list.append(add_attribute_range)

                if 12 == range_position:
                    # Add new range.
                    self.logger.debug("Add new range.")  # debug
                    add_attribute_range = TextFieldAttribute(
                            new_attribute_range_length, is_multivalued, value
                        )
                    new_attribute_list.append(add_attribute_range)

                    # Copy old range unchanged.
                    new_attribute_list.append(old_attribute_range)

            else:
                # Old attribute is the same as new attribute.
                if 1 == range_position \
                    or 4 == range_position \
                    or 5 == range_position \
                    or 8 == range_position \
                    or 13 == range_position:

                    # Copy unchanged.
                    self.logger.debug("Copy unchanged.")  # debug
                    new_attribute_list.append(old_attribute_range)

                if 2 == range_position or 3 == range_position:
                    # Extend new to start of old, discard old (do nothing).
                    self.logger.debug("Extend new to start of old, discard old (do nothing).")  # debug
                    new_attribute_range_start = old_attribute_range_start

                if 11 == range_position or 12 == range_position:
                    #   New is now
                    # part of this range, doesn't need to be added.
                    self.logger.debug("Extend old to start of new and add.")  # debug
                    add_attribute_range_length = \
                        new_attribute_range_start - old_attribute_range_start
                    add_attribute_range = TextFieldAttribute(
                            new_attribute_range_length
                                + add_attribute_range_length,
                            is_multivalued,
                            value
                        )
                    new_attribute_list.append(add_attribute_range)

                # For 9, 7, 9, 10 do nothing (discard old).

        self.__attribute_map[attribute] = new_attribute_list
        self.logger.debug("use new_attribute_list")  # debug

        # Generate new attributes string for this attribute.
        attribute_string = attribute + ": "
        # Separator string.
        sep_str = ""
        for attribute_range in new_attribute_list:
            if is_multivalued:
                if "" == attribute_range.value:
                    # No value, omit "="
                    attribute_string = \
                        attribute_string + sep_str + str(attribute_range.length)
                else:
                    attribute_string = \
                        attribute_string \
                        + sep_str \
                        + attribute_range.value \
                        + "=" \
                        + str(attribute_range.length)
            else:
                attribute_string = \
                    attribute_string + sep_str + str(attribute_range.length)

            sep_str = ", "

        self.__attribute_string_map[attribute] = attribute_string

        # Generate new __attributes string from all indiviaual attribute
        # strings.
        new_attributes = ""
        sep_str = ""
        for attribute_string in list(self.__attribute_string_map.values()):
            new_attributes = new_attributes + sep_str + attribute_string
            sep_str = "\r\n"

        self.__attributes = new_attributes
        self.set_changed_header(Message.ATTRIBUTES, self.__attributes)

    def fill_headers_add(self, message):
        ContainedComponent.fill_headers_add(self, message)
        if self.__content is not None:
            message.add_header(Message.CONTENT, self.__content)
        if self.__attributes is not None and self.__attributes != "":
            message.add_header(Message.ATTRIBUTES, self.__attributes)

    def set_handle_changed(self, handle_changed):
        self.__handle_changed = handle_changed

    def get_handle_changed(self, event):
        content = event.get_header(Message.CONTENT)
        if content is not None:
            self.__old_content = self.__content
            self.__content = content

        return self.__handle_changed


class Container(ContainedComponent):
    def __init__(self):
        ContainedComponent.__init__(self)
        self.__component_list = []

    def add(self, component, horizontal, vertical):
        component.set_parent(self)
        component.set_position(horizontal, vertical)
        if self.added_to_hicp is not None:
            self.added_to_hicp.add(component)
        self.__component_list.append(component)

class Panel(Container):
    def __init__(self):
        Container.__init__(self)

        self.component = Component.PANEL

class Window(Container):
    def __init__(self):
        Container.__init__(self)

        self.component = Component.WINDOW
        self.component_text = ComponentText(self)
        self.__visible = False

    def set_text_id(self, text_id):
        self.component_text.set_text_id(text_id)

    def set_text(self, text, hicp):
        self.component_text.set_text(text, hicp)

    def set_visible(self, visible):
        self.__visible = visible
        if visible:
            self.set_changed_header(Message.VISIBLE, "true")
        else:
            self.set_changed_header(Message.VISIBLE, "false")

    def set_handle_close(self, handle_close):
        self.__handle_close = handle_close

    def get_handle_close(self):
        return self.__handle_close

    def fill_headers_add(self, message):
        Component.fill_headers_add(self, message)

        if self.__visible is not None:
            message.add_header(Message.VISIBLE, "true")
        else:
            message.add_header(Message.VISIBLE, "false")

        self.component_text.fill_headers_add(message)


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

        except AttributeError:
            # No process handler, no biggie. Ignore it.
            self.logger.debug("event_update event handler has no update method")
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

    def __init__(self, default_group: str):
        if default_group is None or '' == default_group:
            raise UnboundLocalError("default_group required, not defined")

        self.groups: Dict[str, TextManagerGroup] = {}
        self.set_group(default_group)

    def get_group(self, group: str = None):
        if group is None:
            group = self.default_group

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
        self.default_group = new_group

    # TODO: Maybe don't need these - just get group and use directly.
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

