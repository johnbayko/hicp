import re

from hicp.logger import newLogger
from hicp.message import Message
from hicp.hicp import EventType

class Component:
    """Components use getters and setters functions, because they need to
keep track of which items have been changed so they can be sent in
"modify" messages when updated."""

    COMPONENT = "component"

    BUTTON = "button"
    LABEL = "label"
    PANEL = "panel"
    TEXTPANEL = "textpanel"
    TEXTFIELD = "textfield"
    WINDOW = "window"

    def __init__(self):
        self.logger = newLogger(type(self).__name__)

        self.component_id = None
        self.added_to_hicp = None

        self.changed_header_list = {}

    def set_handler(self, event_type, handler):
        # Maybe in the future, this will be a map, but for now handlers are
        # tracked by individual component classes.
        pass

    def get_handler(self, event):
        # See set_handler().
        return None

    def set_changed_header(self, header, field):
        # Must be a string - the value that will be put in the update
        # message.
        if not isinstance(field, str):
            raise TypeError("header must be a string as in a message header field")

        self.changed_header_list[header] = field

    def fill_headers_add(self, message):
        self.logger.debug("Component fill add headers: " + Message.GUI + " component_id " + str(self.component_id) + " component " + self.component)  # debug
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(self.component_id))
        message.add_header(self.COMPONENT, self.component)

    def fill_headers_modify(self, message):
        self.logger.debug("Component fill modify headers")
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(self.component_id))

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

    # Indexes to __position and __size
    HORIZONTAL = 0
    VERTICAL = 1

    # Convenience copies.
    ENABLED = Message.ENABLED
    DISABLED = Message.DISABLED

    def __init__(self):
        Component.__init__(self)

        self.__parent_id = None  # Number
        self.__position = [None, None] # [Number, Number]
        self.__size = [None, None] # [Number, Number]
        self.__events = None

    def set_parent(self, component):
        if component is None:
            # No partent component to add to.
            self.logger.debug("ContainedComponent.set_parent() component is null")
            return
        try:
            if component.component_id is None:
                # No component_id attribute.
                self.logger.debug("ContainedComponent.set_parent() component has null component_id attribute")
                return
        except AttributeError:
            # No component_id attribute at all.
            self.logger.debug("ContainedComponent.set_parent() component has no .component_id attribute")
            return

        self.__parent_id = component.component_id
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

    def set_events(self, field):
        self.__events = field
        self.set_changed_header(Message.EVENTS, field)

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

        if self.__events is not None:
            message.add_header(Message.EVENTS, self.__events)


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

    def set_text_get_id(self, text, hicp, group = None, subgroup = None):
        text_id = hicp.add_text_get_id(text, group, subgroup)

        # Make sure text is added to client before setting here.
        hicp.add_text(text_id, text)
        self.set_text_id(text_id)

        return text_id

    def set_groups_text(self, text_group_list, hicp):
        text_id = hicp.add_groups_text_get_id(text_group_list)
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
        self.component_text.set_text_get_id(text, hicp)

    def set_groups_text(self, text_group_list, hicp):
        self.component_text.set_groups_text(text_group_list, hicp)

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
        self.component_text.set_text_get_id(text, hicp)

    def set_groups_text(self, text_group_list, hicp):
        self.component_text.set_groups_text(text_group_list, hicp)

    def fill_headers_add(self, message):
        ContainedComponent.fill_headers_add(self, message)
        self.component_text.fill_headers_add(message)

    def set_handler(self, event_type, handler):
        if EventType.CLICK == event_type:
            self.__handle_click = handler
        else:
            super().set_handler(event_type, handler)

    def get_handler(self, event):
        handler = super().get_handler(event)
        if None != handler:
            return handler

        if EventType.CLICK == event.event_type:
            return self.__handle_click
        else:
            # Not an event type this component supports.
            return None

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
# TODO: Leave layout stuff alone until implemented / designed.
#    LAYOUT = Message.LAYOUT
    SIZE = Message.SIZE
    UNDERLINE = Message.UNDERLINE

    # Indicate multivalued or not.
    MULTIVALUED_ATTRIBUTES = {
            FONT,
#            LAYOUT,
            SIZE,
        }
    NOT_MULTIVALUED_ATTRIBUTES = {
            BOLD,
            ITALIC,
            UNDERLINE,
        }

    # ATTRIBUTES FONT attributes
    SERIF = Message.SERIF
    SANS_SERIF = Message.SANS_SERIF
    SERIF_FIXED = Message.SERIF_FIXED
    SANS_SERIF_FIXED = Message.SANS_SERIF_FIXED

    # ATTRIBUTES LAYOUT attributes
#    BLOCK = Message.BLOCK
#    INDENT_FIRST = Message.INDENT_FIRST
#    INDENT_REST = Message.INDENT_REST
#    LIST = Message.LIST

    # EDITING attributes
    ENABLED = Message.ENABLED
    DISABLED = Message.DISABLED
    SERVER = Message.SERVER

    CONTENT_INVALID_RE = re.compile("[\\0-\\037]")

    ATTRIBUTE_SPLIT_RE = re.compile(" *(.*) *: *(.*) *")

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
        self.__content = content
        self.set_changed_header(Message.CONTENT, self.__content)

        # Clear attributes if there are any.
        if 0 < len(self.__attributes):
            self.__attribute_map.clear()
            self.__attributes = ""
            self.set_changed_header(Message.ATTRIBUTES, self.__attributes)

    def get_content(self):
        return self.__content

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
            self.logger.debug("value is None")  # debug
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

    def set_attribute_string(self, attribute_list_string):
        if attribute_list_string is None:
            raise UnboundLocalError("attribute_string required, not defined")

        new_attribute_map = {}
        new_attribute_string_map = {}

        if 0 < len(attribute_list_string):
            for attribute_string in attribute_list_string.split("\r\n"):
                attribute_match = self.ATTRIBUTE_SPLIT_RE.search(attribute_string)
                try:
                    (attribute_name, attribute_values_string) = \
                        attribute_match.group(1, 2)

                    # If no values, skip attribute.
                    # ''.split(',') will create a 1 element list of [''], not a 0
                    # element list. Easy to forget.
                    if 0 < len(attribute_values_string):
                        # split attribute value string into list of strings.
                        attribute_values_list = attribute_values_string.split(",")

                        attribute_list = []
                        for attribute_value_str in attribute_values_list:
                            # Try splitting by "=".
                            attribute_value_str_list = attribute_value_str.split("=")
                            if 2 == len(attribute_value_str_list):
                                value = attribute_value_str_list[0].strip()
                                length = int(attribute_value_str_list[1])
                            else:
                                value = ""
                                length = int(attribute_value_str_list[0])

                            is_multivalued = attribute_name in self.MULTIVALUED_ATTRIBUTES
                            attribute_list.append(
                                TextFieldAttribute(length, is_multivalued, value) )

                        new_attribute_map[attribute_name] = attribute_list
                        new_attribute_string_map[attribute_name] = attribute_string

                except (IndexError, ValueError):
                    # No valid attribute list, skip this one.
                    self.logger.debug("attribute list invalid: " + attribute_string)
                    pass
            
        self.__attribute_map = new_attribute_map
        # Maybe should construct strings from attribute map because they
        # might have had errors?
        self.__attribute_string_map = new_attribute_string_map
        self.__attributes = attribute_list_string

    def get_attribute_string(self):
        return self.__attributes

    def fill_headers_add(self, message):
        ContainedComponent.fill_headers_add(self, message)
        if self.__content is not None:
            message.add_header(Message.CONTENT, self.__content)
        if self.__attributes is not None and self.__attributes != "":
            message.add_header(Message.ATTRIBUTES, self.__attributes)

    def set_handler(self, event_type, handler):
        if EventType.CHANGED == event_type:
            self.__handle_changed = handler
        else:
            super().set_handler(event_type, handler)

    def get_handler(self, event):
        handler = super().get_handler(event)
        if None != handler:
            return handler

        if EventType.CHANGED == event.event_type:
            return self.__get_handle_changed(event)
        else:
            # Not an event type this component supports.
            return None

    def __get_handle_changed(self, event):
        # Get content from event
        content = event.message.get_header(Message.CONTENT)
        if content is not None:
            self.__content = content

        # Get attributes from event.
        attribute_header = event.message.get_header(Message.ATTRIBUTES)
        if attribute_header is not None:
            self.set_attribute_string(attribute_header)

        # Return changed event handler.
        return self.__handle_changed


class Container(ContainedComponent):
    def __init__(self):
        ContainedComponent.__init__(self)
        self.__component_list = []

    def add(self, component, horizontal, vertical):
        component.set_parent(self)
        component.set_position(horizontal, vertical)
        self.add_to_hicp(component)
        self.__component_list.append(component)

    def add_to_hicp(self, component):
        if self.added_to_hicp is None:
            return
        self.added_to_hicp.add(component)
        # If that's a Container, add it's components too (it wouldn't have
        # self.added_to_hicp set, so would have skipped the above step).
        if isinstance(component, Container):
            component.add_component_list_to_hicp()

    def add_component_list_to_hicp(self):
        for component in self.__component_list:
            component.set_parent(self)
            self.added_to_hicp.add(component)


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
        self.component_text.set_text_get_id(text, hicp)

    def set_groups_text(self, text_group_list, hicp):
        self.component_text.set_groups_text(text_group_list, hicp)

    def set_visible(self, visible):
        self.__visible = visible
        if visible:
            self.set_changed_header(Message.VISIBLE, "true")
        else:
            self.set_changed_header(Message.VISIBLE, "false")

    def set_handler(self, event_type, handler):
        if EventType.CLOSE == event_type:
            self.__handle_close = handler
        else:
            super().set_handler(event_type, handler)

    def get_handler(self, event):
        handler = super().get_handler(event)
        if None != handler:
            return handler

        if EventType.CLOSE == event.event_type:
            return self.__handle_close
        else:
            # Not an event type this component supports.
            return None

    def fill_headers_add(self, message):
        Component.fill_headers_add(self, message)

        if self.__visible is not None:
            message.add_header(Message.VISIBLE, "true")
        else:
            message.add_header(Message.VISIBLE, "false")

        self.component_text.fill_headers_add(message)



