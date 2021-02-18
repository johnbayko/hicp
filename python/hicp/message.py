import re

from hicp.logger import newLogger

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
    # TODO: Maybe make individual event classes?
    ATTRIBUTES = "attributes"
    APPLICATION = "application"
    CATEGORY = "category"
    CONTENT = "content"
    EVENTS = "events"
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

    # EVENTS attributes
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
                        length_match = \
                            self.LENGTH_RE.search(termination_criterion)
                        length = int(length_match.group(2), 10)

                        # header_value is the next length bytes (unless
                        # EOF is encountered).
                        header_value = in_stream.read(length)

                        # There must be a terminating CR LF, so read to
                        # the end of the input line (extra is discarded).
                        self.readline(in_stream)

                    elif 0 <= termination_criterion.find("boundary"):
                        boundary_match = \
                            self.BOUNDARY_RE.search(termination_criterion)
                        boundary = boundary_match.group(2)

                        # Boundary is either CR LF <text> or <text>.
                        if '' == boundary:
                            # Boundary is CR LF plus next line.
                            # The boundary excludes the next CR LF in the
                            # string returned by readline(), but it's
                            # easier to find the boundary if they are
                            # included.
                            boundary = self.readline(in_stream)

                            # This lets us compare the full line to the
                            # boundary string, so indicate that.
                            full_line_boundary = True
                        else:
                            full_line_boundary = False

                        # Read until boundary + CR LF is read.
                        header_value_list = []
                        prev_line_eol_esc = False
                        while 1:
                            header_value_part = self.readline(in_stream)

                            # Remove any escapes, but keep track of
                            # index of last one.
                            (header_value_unescaped, esc_cnt) = \
                                self.ESC_RE.subn("\\2", header_value_part)

                            if 0 < esc_cnt:
                                # String had escapes.
                                last_esc_match = \
                                    self.LAST_ESC_RE.search(header_value_part)
                                after_last_esc_index = last_esc_match.end(2)
                            else:
                                # No esc in string.
                                after_last_esc_index = 0

                            if full_line_boundary:
                                if header_value_unescaped == boundary \
                                    and False == prev_line_eol_esc \
                                    and 0 == esc_cnt:

                                    # Found the boundary. header value doesn't
                                    # include this line.
                                    # The final CR LF is also not included,
                                    # Remove it from the last header value list
                                    # string.
                                    header_value_list[-1] = \
                                        header_value_list[-1][:-2]
                                    break
                            else:
                                # Check for boundary. Should always be at end
                                # of the line, but accept it if not - discard
                                # rest of line.
                                boundary_index = \
                                    header_value_unescaped.find(boundary)

                                if 0 <= boundary_index \
                                    and after_last_esc_index <= boundary_index:

                                    # Found end of line boundary. Remove
                                    # boundary and add to header value list.
                                    header_value_no_boundary = \
                                        header_value_unescaped[ : boundary_index]
                                    header_value_list = \
                                        header_value_list + [header_value_no_boundary]
                                    break

                            # No boundary found, add to header value list
                            header_value_list = \
                                header_value_list + [header_value_unescaped]

                            prev_line_eol_esc = \
                                (after_last_esc_index >= len(header_value_part) - 2)

                        # Convert list to single string.
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
        # self.logger.debug("Write: " + self.__type + ": " + self.__type_value) # debug
        out_stream.write(self.__type + ": " + self.__type_value + "\r\n")

        # Write all headers
        for header_key in list(self.__headers.keys()):
            header_value = self.__headers[header_key]
            # self.logger.debug("header " + header_key + ": " + header_value) # debug
            # If value has "\r\n" within it, output as data block,
            # otherwise as simple header.
            if -1 == header_value.find("\r\n"):
                # Simple header field.
                # self.logger.debug("Write: " + header_key + ": " + header_value + "\r\n") # debug
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
        # self.logger.debug(" write:" + "\r\n") # debug
        out_stream.write("\r\n")

        out_stream.flush()

    def set_type(self, message_type, value):
        self.__type = message_type
        self.__type_value = value

    def get_type(self):
        return self.__type

    def get_type_value(self):
        return self.__type_value

    def clear(self):
        self.__headers.clear()

    def get_header(self, header_key):
        "Return value for key, or None if no header with that key, or key is None."
        if header_key is None:
            return None

        try:
            # Header keys are stored as lower case.
            header_value = self.__headers[header_key.lower()]
        except KeyError:
            self.logger.debug("Header not found key: " + header_key) # debug
            for header_key in list(self.__headers.keys()):  # debug
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

