import re
import types

from hicp.logger import newLogger
from hicp.message import Message
from hicp.hicp import EventType


class Component:
    """Components use getters and setters functions, because they need to
keep track of which items have been changed so they can be sent in
"modify" messages when updated."""

    class HeaderValues:
        def __init__(self):
            pass

        def set_from(self, other):
            """
            When headers are sent, the current values are copied to the send
            values. The current values should never be removed, so no need to
            check for not None, just copy them over.
            """
            pass
        
    COMPONENT = "component"

    BUTTON = "button"
    LABEL = "label"
    PANEL = "panel"
    SELECTION = "selection"
    TEXTPANEL = "textpanel"
    TEXTFIELD = "textfield"
    WINDOW = "window"

    def __init__(self):
        self.logger = newLogger(type(self).__name__)

        self.current = self.HeaderValues()
        self.sent = self.HeaderValues()

        self.component_id = None
        self.hicp = None

    def is_added(self):
        # self.hicp is only set when an "add" command is sent out, so also
        # indicates that any future message will be "modify".
        return self.hicp is not None

    def set_handler(self, event_type, handler):
        # Maybe in the future, this will be a map, but for now handlers are
        # tracked by individual component classes.
        pass

    def get_handler(self, event):
        # See set_handler().
        return None

    def fill_headers_add(self, message):
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(self.component_id))
        message.add_header(self.COMPONENT, self.component)

    def fill_headers_modify(self, message):
        message.add_header(Message.CATEGORY, Message.GUI)
        message.add_header(Message.ID, str(self.component_id))

        # Other fields are in the changed header list, used by update.

    def notify_sent(self):
        # Copy current values to sent.
        self.sent.set_from(self.current)

    def update(self):
#        if self.hicp is None:
        if not self.is_added():
            # No hicp to handle update call.
            return

        self.hicp.update(self)

class ContainedComponent(Component):
    """Contained within a container, needs to keep track of parent and
position."""

    class HeaderValues(Component.HeaderValues):
        def __init__(self):
            self.parent_id = None  # Number
            self.position = [None, None] # [Number, Number]
            self.size = [None, None] # [Number, Number]
            self.events = None

            super().__init__()

        def set_from(self, other):
            super().set_from(other)

            self.parent_id = other.parent_id
            self.position = other.position.copy()
            self.size = other.size.copy()
            self.events = other.events

    # Indexes to position and size
    HORIZONTAL = 0
    VERTICAL = 1

    # Convenience copies.
    ENABLED = Message.ENABLED
    DISABLED = Message.DISABLED

    def __init__(self):
        super().__init__()

    def set_parent(self, component):
        if component is None:
            # No parent component to add to.
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

        self.current.parent_id = component.component_id

    def __position_field(self):
        field = None
        position = self.current.position
        if ( position[ContainedComponent.HORIZONTAL] is not None
          or position[ContainedComponent.VERTICAL] is not None
        ):
            field = ""
            if position[ContainedComponent.HORIZONTAL] is not None:
                field = field + str(position[ContainedComponent.HORIZONTAL])
            field = field + ","
            if position[ContainedComponent.VERTICAL] is not None:
                field = field + str(position[ContainedComponent.VERTICAL])
        return field

    def set_position(self, horizontal, vertical):
        # horizontal and vertical are integers.
        self.current.position[ContainedComponent.HORIZONTAL] = horizontal
        self.current.position[ContainedComponent.VERTICAL] = vertical

    def __size_field(self):
        field = None
        size = self.current.size
        if ( size[ContainedComponent.HORIZONTAL] is not None
          or size[ContainedComponent.VERTICAL] is not None
        ):
            field = ""
            if size[ContainedComponent.HORIZONTAL] is not None:
                field = field + str(size[ContainedComponent.HORIZONTAL])
            field = field + ","
            if size[ContainedComponent.VERTICAL] is not None:
                field = field + str(size[ContainedComponent.VERTICAL])
        return field

    def set_size(self, horizontal, vertical):
        # horizontal and vertical are integers.
        self.current.size[ContainedComponent.HORIZONTAL] = horizontal
        self.current.size[ContainedComponent.VERTICAL] = vertical

    def set_events(self, field):
        self.current.events = field

    def fill_headers_add(self, message):
        super().fill_headers_add(message)
        message.add_header(Message.PARENT, str(self.current.parent_id))
        message.add_header(Message.POSITION, self.__position_field())
        message.add_header(Message.SIZE, self.__size_field())
        message.add_header(Message.EVENTS, self.current.events)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)
        if self.sent.parent_id != self.current.parent_id:
            message.add_header(Message.PARENT, self.current.parent_id)
        if self.sent.position != self.current.position:
            field = self.__position_field()
            if field is None:
                field = ","
            message.add_header(Message.POSITION, field)
        if self.sent.size != self.current.size:
            field = self.__size_field()
            if field is None:
                field = ","
            message.add_header(Message.SIZE, field)
        if self.sent.events != self.current.events:
            message.add_header(Message.EVENTS, self.current.events)


class ComponentText():
    class HeaderValues(ContainedComponent.HeaderValues):
        def __init__(self):
            self.text_id = None # Number
            super().__init__()

        def set_from(self, other):
            super().set_from(other)
            self.text_id = other.text_id

    def __init__(self, control):
        self.logger = newLogger(type(self).__name__)

        self.current = self.HeaderValues()
        self.sent = self.HeaderValues()

        self.control = control
        self.current.text_id = None # Number

    # TODO Remove when components are all updated.
    def set_from(self, other):
        # self.control shouldn't be used, copied so that methods that need it
        # don't fail unexpectedly if used for unforseen reasons.
        self.control = other.control
        self.current.text_id = other.current.text_id

    def set_text_id(self, text_id):
        text_id_str = str(text_id)
        if text_id_str != self.current.text_id:
            self.current.text_id = text_id_str

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
        message.add_header(Message.TEXT, self.current.text_id)

    def fill_headers_modify(self, message):
        if self.sent.text_id != self.current.text_id:
            message.add_header(Message.TEXT, self.current.text_id)

    def notify_sent(self):
        # Copy current values to sent.
        self.sent.set_from(self.current)


class Label(ContainedComponent):
    class HeaderValues(ContainedComponent.HeaderValues):
        def __init__(self):
            super().__init__()

#        def set_from(self, other):
#            super().set_from(other)

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
        super().fill_headers_add(message)
        self.component_text.fill_headers_add(message)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)
        self.component_text.fill_headers_modify(message)

    def notify_sent(self):
        super().notify_sent()
        self.component_text.notify_sent()


class Button(ContainedComponent):
    class HeaderValues(ContainedComponent.HeaderValues):
        def __init__(self):
            super().__init__()

#        def set_from(self, other):
#            super().set_from(other)

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
        super().fill_headers_add(message)
        self.component_text.fill_headers_add(message)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)
        self.component_text.fill_headers_modify(message)

    def notify_sent(self):
        super().notify_sent()
        self.component_text.notify_sent()

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
            return self.get_handle_click()
        else:
            # Not an event type this component supports.
            return None

    def get_handle_click(self):
        try:
            return self.__handle_click
        except AttributeError:
            # Hasn't been set.
            return None

class TextFieldAttributeRange:
    def __init__(self, length: int, value: str=None):
        self.length = length
        self.value = value

class TextFieldAttribute:
    def __init__(self, is_multivalued: bool, length: int = 0):
        self.is_multivalued = is_multivalued

        # Convenience value
        self.default_value = "" if is_multivalued else False

        self.position = 0

        if length > 0:
            self.attribute_range_list = [
                TextFieldAttributeRange(length, self.default_value) ]
        else:
            self.attribute_range_list = []

class TextField(ContainedComponent):
    # CONTENT actions
    SET = Message.SET
    ADD = Message.ADD
    DELETE = Message.DELETE

    # ATTRIBUTES attributes
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
    BOOLEAN_ATTRIBUTES = {
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

    CONTENT_INVALID_RE = re.compile("[\\0-\\037]")

    # Format is <attribute name>: <position> : <range list>
    ATTRIBUTE_SPLIT_RE = re.compile(" *(.*) *: *(.*) *: *(.*) *")

    class HeaderValues(ContainedComponent.HeaderValues):
        def __init__(self):
            self.content = ""
            self.width = None

            # Maps attribute name to TextFieldAttribute objects.
            # When content is set, all attributes are cleared.
            self.attribute_map = {}

            # attributes is the whole attribute list, there's no way to
            # send them separately. However, each attribute can have its own
            # string stored to reduce extra work.
            self.attribute_string_map = {}

            # Attribute header is string representation of attribute_map.
            # It's easier to generate it when attributes are set and treat
            # it like any other header string than to detect it when headers
            # are written out and generate it then.
            self.attributes = ""
            super().__init__()

        def set_from(self, other):
            super().set_from(other)
            self.content = other.content
            self.width = other.width
            # Map is copied, but what it contains is shared, be careful.
            # TODO Fix this.
            self.attribute_map = other.attribute_map.copy()
            self.attribute_string_map = other.attribute_string_map.copy()
            self.attributes = other.attributes

        # TODO add add_content() and del_content() methods.
        # If called once, store the change and send as "content: add:" or
        # "content: del:" actions.  If called more than
        # once before update(), they replace the existing content.
        # This has no practical purpose, it's just for testing the add and del
        # protocol.

        # The header value for the content.
        def get_content_value(self):
            return TextField.SET + ": " + self.content
            
        def get_content(self):
            return self.content

        # Similar for attributes?
            

    def __init__(self):
        ContainedComponent.__init__(self)

        self.component = Component.TEXTFIELD

    def sanitize_content(self, content: str):
        # Content must be a string.
        content = str(content)

        # For text field, there should be no unprintable characters or
        # EOL sequence.
        # Truncate at first invalid character.
        content_invalid_match = self.CONTENT_INVALID_RE.search(content)
        if content_invalid_match is not None:
            content = content[:content_invalid_match.start(0)]
        return content

    def set_content(self, content):
        self.current.content = self.sanitize_content(content)

        # Clear attributes if there are any.
        if 0 < len(self.current.attributes):
            self.current.attribute_map.clear()
            self.current.attribute_string_map.clear()
            self.current.attributes = ""

    def add_content(self, position: int, new_content: str):
        # Add to content.
        content = self.current.content

        content_len = len(content)
        # Position can be negative as a Python index, but HICP only allows
        # positive from start, so convert that.
        if position < 0:
            position = content_len - position
        # If position not in current content, don't do anything.
        if position > content_len:
            return

        new_content = self.sanitize_content(new_content)

        # Do the insert.
        content = content[:position] + new_content + content[position:]
        self.current.content = content

        # Modify affected ranges in each TextFieldAttribute by extending at
        # position by length of content.
        new_content_len = len(new_content)
        for attribute_info in self.current.attribute_map.values():

            # Find an attribute range that includes the position just inserted,
            # and extend it by the new content length.
            attribute_pos = 0
            for attribute_range in attribute_info.attribute_range_list:
                attribute_lim = attribute_pos + attribute_range.length
                # When inserting at the end of a range, you expect the range to
                # extend, so do not include first position of this range, but
                # do include first position of next range (attribute_lim).
                # Exception: when inserting at position 0, there is no prior
                # range, so extend current range.
                first_pos = attribute_pos + 1 if attribute_pos > 0 else 0

                if position >= first_pos and position <= attribute_lim:
                    # The values in the attribute range list can be directly
                    # modified.  Probably not the safe way to do it in general,
                    # but this is the only code controlling this at the moment.
                    # Keep an eye on it though.
                    attribute_range.length += new_content_len

                    # This attribute type has been taken care of, on to the
                    # next one.
                    break

                # Update attribute position.
                attribute_pos = attribute_lim

    def del_content(self, position: int, length: int):
        # Delete from content from position forward if length is positive,
        # backward if negative.
        # Modify each attribute by shortening at position by length (forward or
        # backward).
        ...

        # Delete from content.
        content = self.current.content

        content_len = len(content)
        # Position can be negative as a Python index, but HICP only allows
        # positive from start, so convert that.
        if position < 0:
            position = content_len - position
        # If position not in current content, don't do anything.
        if position > content_len:
            return

        # Do the delete.
        content = content[:position] + content[position + length:]
        self.current.content = content

        # Modify affected ranges in each TextFieldAttribute by extending at
        # position by length of content.
        for attribute_info in self.current.attribute_map.values():
            length_ramining = length

            # Find an attribute range that includes the position just inserted,
            # and extend it by the new content length.
            attribute_pos = 0
            for attribute_range in attribute_info.attribute_range_list:
                attribute_lim = attribute_pos + attribute_range.length

                # For the first attribute range, position should be past
                # attribute_pos, but might be equal. For following ranges, it
                # will be equal.
                if position >= attribute_pos and position < attribute_lim:
                    # The values in the attribute range list can be directly
                    # modified.  Probably not the safe way to do it in general,
                    # but this is the only code controlling this at the moment.
                    # Keep an eye on it though.

                    # Characters from attribute_pos to position are not
                    # deleted, so the range cannot be reduced by more than
                    # that. All other ranges can be reduced completely. In all
                    # cases, if a range is reduced to 0, remove it (after the
                    # loop, can't modify what's being iterated over).
                    min_len = position - attribute_pos
                    range_len = attribute_range.length

                    attribute_range.length -= length_remaining
                    if attribute_range.length < min_len:
                        attribute_range.length = min_len

                    length_deleted = range_len - attribute_range.length
                    length_kept = range_len - length_deleted

                    length_remaining -= length_deleted
                    attribute_pos += length_kept
                else:
                    # Nothing deleted, update attribute position.
                    attribute_pos = attribute_lim

                if length_remaining <= 0:
                    break;

            # Remove 0 length ranges.
            new_attribute_range_list = \
                [r for r in attribute_info.attribute_range_list if r.length > 0]

            # TODO Merge ranges with the same value.
            # Set attribute will search for existing ranges, and extend if
            # value is same.
            # Add content will just extend existing attributes.
            # So just need to do merges here.
            range_lim = len(new_attribute_range_list) - 1
            range_idx = 0
            while range_idx < range_lim:
                next_range_idx = range_idx + 1

                current_range = new_attribute_range_list[range_idx]
                next_range = new_attribute_range_list[next_range_idx]

                if current_range.value == next_range.value:
                    current_range.length += next_range.length
                    new_attribute_range_list.pop(next_range_idx)
                    range_lim -= 1

                    # Don't advance to next range yet.
                else:
                    range_idx += 1

            attribute_info.attribute_range_list = new_attribute_range_list


    def get_content(self):
        return self.current.get_content()

#    def content_del_before(self, del_len, del_pos=None):
#        # If this has not been sent (message will be ADD command), then
#        # directly change content.
#        # If this has been sent (message will be MODIFY command), then add to
#        # attribute map using key "change-list".
#
#    def content_del_after(self, del_len, del_pos=None):
#
#    def content_add(self, add_content, add_pos=None):

    def set_width(self, width):
        self.current.width = str(width)

    # Indicate binary attribute with value of True/False, multivalue as str
    # (value can be "" for default, not None).
    def set_attribute(
        self,
        attribute: str,
        new_attribute_range_start: int,
        new_attribute_range_length: int,
        value : bool | str
    ):
        # TODO: support MODIFY attributes

        # Attribute (if specified) must be string.
        attribute = str(attribute)

        # New attribute range can't start past end of content.
        if len(self.current.content) < new_attribute_range_start:
            # Nothing to apply attribute to.
            return

        # For consistancy, end index in all cases is the first index not
        # part of the range.
        new_attribute_range_end = \
            new_attribute_range_start + new_attribute_range_length

        # New attribute range cannot go past end of content - truncate.
        content_len = len(self.current.content)
        if content_len < new_attribute_range_end:
            new_attribute_range_end = content_len
            new_attribute_range_length = \
                new_attribute_range_end - new_attribute_range_start

        # For text field, there should be no unprintable characters, but
        # may include multiple EOL sequences.
        # Should either strip them out or reject this action - deal with
        # that later.

        attribute_info = self.current.attribute_map.get(attribute)
        if attribute_info is None:
            is_multivalued = (type(value) == str)

            # Create and add new attribute info to attribute map.
            attribute_info = \
                TextFieldAttribute(
                    is_multivalued,
                    length = len(self.current.content))

            self.current.attribute_map[attribute] = attribute_info

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
        # For attributes which differ from the new attribute:
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
        #
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
        new_attribute_range_list = []

        # Find point in attribute list where new attribute applies.
        old_attribute_range_start = attribute_info.position

        for old_attribute_range in attribute_info.attribute_range_list:
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

            if old_attribute_range.value != value:
                # Old attribute differs from new attribute.
                if 3 == range_position or 4 == range_position:
                    # Truncate range, becomes same as 2
                    old_attribute_range = TextFieldAttributeRange(
                            new_attribute_range_start - old_attribute_range_start,
                            old_attribute_range.value
                        )
                    old_attribute_range_end = new_attribute_range_start
                    range_position = 2

                if 8 == range_position or 11 == range_position:
                    # Truncate range, becomes same as 2
                    old_attribute_range = TextFieldAttributeRange(
                            old_attribute_range_end - new_attribute_range_end,
                            old_attribute_range.value
                        )
                    old_attribute_range_start = new_attribute_range_end
                    range_position = 12

                if 1 == range_position or 2 == range_position or 13 == range_position:
                    # Copy unchanged.
                    new_attribute_range_list.append(old_attribute_range)

                if 5 == range_position:
                    # Add range truncated to start of new range.
                    add_attribute_range = TextFieldAttributeRange(
                            new_attribute_range_start - old_attribute_range_start,
                            old_attribute_range.value
                        )
                    new_attribute_range_list.append(add_attribute_range)

                    # Add new range.
                    add_attribute_range = TextFieldAttributeRange(
                            new_attribute_range_length, value
                        )
                    new_attribute_range_list.append(add_attribute_range)

                    # Add range truncated to end of new range.
                    add_attribute_range = TextFieldAttributeRange(
                            old_attribute_range_end - new_attribute_range_end,
                            old_attribute_range.value
                        )
                    new_attribute_range_list.append(add_attribute_range)

                if 7 == range_position or 10 == range_position:
                    # Add new range.
                    add_attribute_range = TextFieldAttributeRange(
                            new_attribute_range_length, value
                        )
                    new_attribute_range_list.append(add_attribute_range)

                if 12 == range_position:
                    # Add new range.
                    add_attribute_range = TextFieldAttributeRange(
                            new_attribute_range_length, value
                        )
                    new_attribute_range_list.append(add_attribute_range)

                    # Copy old range unchanged.
                    new_attribute_range_list.append(old_attribute_range)

            else:
                # Old attribute is the same as new attribute.
                if 1 == range_position \
                    or 4 == range_position \
                    or 5 == range_position \
                    or 8 == range_position \
                    or 13 == range_position:

                    # Copy unchanged.
                    new_attribute_range_list.append(old_attribute_range)

                if 2 == range_position or 3 == range_position:
                    # Extend new to start of old, discard old (do nothing).
                    new_attribute_range_start = old_attribute_range_start

                if 11 == range_position or 12 == range_position:
                    # New is now part of this range, doesn't need to be added.
                    add_attribute_range_length = \
                        new_attribute_range_start - old_attribute_range_start
                    add_attribute_range = TextFieldAttributeRange(
                            new_attribute_range_length
                                + add_attribute_range_length,
                            value
                        )
                    new_attribute_range_list.append(add_attribute_range)

                # For 9, 7, 9, 10 do nothing (discard old).

        attribute_info.attribute_range_list = new_attribute_range_list

        # Generate new attributes string for this attribute.
        attribute_range_str_list = []
        for attribute_range in attribute_info.attribute_range_list:
            if attribute_info.is_multivalued:
                attribute_range_str = \
                    str(attribute_range.length) + \
                        "=" + \
                        str(attribute_range.value)
            else:
                attribute_range_str = \
                    str(attribute_range.length)
            attribute_range_str_list.append(attribute_range_str)

        # If the first range is the default value, remove it, that length
        # becomes the start position.
        first_attribute_range = attribute_info.attribute_range_list[0]
        is_first_range_default = False
        if attribute_info.is_multivalued:
            is_first_range_default = (first_attribute_range.value == "")
        else:
            is_first_range_default = (first_attribute_range.value == False)
        if is_first_range_default:
            # Range length becomes tbe start position of the first
            # non-default attribute.
            attribute_info.position = first_attribute_range.length
            # Discard.
            attribute_range_str_list.pop(0)

        # If the last range is the default value, it can be removed as
        # well.
        last_attribute_range = attribute_info.attribute_range_list[-1]
        is_last_range_default = False
        if attribute_info.is_multivalued:
            is_last_range_default = (last_attribute_range.value == "")
        else:
            is_last_range_default = (last_attribute_range.value == False)
        if is_last_range_default:
            # Discard.
            attribute_range_str_list.pop(-1)

        attribute_string = \
            attribute + ': ' + \
            str(attribute_info.position) + ': ' + \
            ', '.join(attribute_range_str_list)

        self.current.attribute_string_map[attribute] = attribute_string

        # Generate new attributes string from all indiviaual attribute
        # strings.
        self.current.attributes = \
            '\r\n'.join( list(self.current.attribute_string_map.values()) )

    def set_attribute_string(self, attribute_list_string):
        if attribute_list_string is None:
            raise UnboundLocalError("attribute_string required, not defined")

        new_attribute_map = {}
        new_attribute_string_map = {}

        if 0 < len(attribute_list_string):
            for attribute_string in attribute_list_string.split("\r\n"):
                attribute_match = self.ATTRIBUTE_SPLIT_RE.search(attribute_string)
                try:
                    (attribute_name, position, attribute_ranges_string) = \
                        attribute_match.group(1, 2, 3)

                    # Parse exception caught below.
                    position = int(position)

                    is_multivalued = \
                        (attribute_name in self.MULTIVALUED_ATTRIBUTES)

                    # If no values, skip attribute.
                    if 0 < len(attribute_ranges_string):
                        attribute_range_list = []
                        length_total = 0

                        # Length 0 means no default range - all comes from
                        # attribute string.
                        attribute_info = TextFieldAttribute(is_multivalued)

                        # Specified ranges start with True, there will always
                        # be an explicit (if position is specified) or implicit
                        # False range first.
                        prev_boolean = False

                        # If first range position doesn't start at 0,
                        # add placeholder range with default value.
                        if position > 0:
                            attribute_range_list.append(
                                TextFieldAttributeRange(
                                    position, attribute_info.default_value) )
                            length_total += position

                        # split attribute value string into list of strings.
                        attribute_range_str_list = \
                            attribute_ranges_string.split(",")

                        for attribute_range_str in attribute_range_str_list:
                            # Try splitting by "=".
                            attribute_range_parts = \
                                attribute_range_str.split("=")

                            # Index and parse errors are caught below,
                            # attribute is skipped.
                            length = int(attribute_range_parts[0].strip())
                            length_total += length

                            if is_multivalued:
                                if 2 <= len(attribute_range_parts):
                                    value = attribute_range_parts[1].strip()
                                else:
                                    value = ""
                            else:
                                value = not prev_boolean
                                prev_boolean = value

                            attribute_range = \
                                TextFieldAttributeRange(length, value)

                            attribute_range_list.append(attribute_range)

                        # There might be a gap between the last range and the
                        # actual end of the content. Fill it with a default
                        # range.
                        if length_total < len(self.current.content):
                            remaining_len = \
                                len(self.current.content) - length_total
                            last_attribute_range = \
                                TextFieldAttributeRange(
                                    remaining_len, attribute_info.default_value)
                            attribute_range_list.append(last_attribute_range)

                        attribute_info.attribute_range_list = \
                            attribute_range_list
                        new_attribute_map[attribute_name] = attribute_info

                        new_attribute_string_map[attribute_name] = attribute_string

                except (IndexError, ValueError):
                    # Not a valid attribute list, skip this one.
                    self.logger.debug("attribute list invalid: " + attribute_string)
                    pass
            
        self.current.attribute_map = new_attribute_map

        # Maybe should construct strings from attribute map because they
        # might have had errors?
        self.current.attribute_string_map = new_attribute_string_map
        self.current.attributes = attribute_list_string

    def get_attribute_string(self):
        return self.current.attributes

    def fill_headers_add(self, message):
        super().fill_headers_add(message)
        message.add_header(Message.CONTENT, self.current.get_content_value())
        if self.current.width is not None:
            message.add_header(Message.WIDTH, self.current.width)
        if self.current.attributes is not None and self.current.attributes != "":
            message.add_header(Message.ATTRIBUTES, self.current.attributes)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)

        sent_content = self.sent.get_content()
        content = self.current.get_content()

        if sent_content != content:
            message.add_header(Message.CONTENT, self.current.get_content_value())

        if self.sent.width != self.current.width:
            message.add_header(Message.WIDTH, self.current.width)
        if self.sent.attributes != self.current.attributes:
            message.add_header(Message.ATTRIBUTES, self.current.attributes)

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
            self.current.content = content

        # Get attributes from event.
        attribute_header = event.message.get_header(Message.ATTRIBUTES)
        if attribute_header is not None:
            self.set_attribute_string(attribute_header)

        # Return changed event handler.
        try:
            return self.__handle_changed
        except AttributeError:
            # Hasn't been set yet.
            return None


class SelectionItem():
    """
    A selection item, to specify HICP values for:
        - id: Integer which identifies item and indicates display order.
        - text id: ID of the text to display.
        - events: What events this item generates.
    and an optional object (item) associated with this selection item.
    """
    def __init__(self, item_id, text, hicp=None, events=None, item=None):
        self.item_id = item_id
        self.events = events
        self.item = item

        if text is None:
            # Has no text to display, required.
            raise UnboundLocalError("text parameter required")
        if isinstance(text, int):
            # If text is an integer, it's a text ID and can be used as is.
            self.text_id = text
        elif isinstance(text, str):
            if hicp is not None:
                # If it's a string and hicp is not None, then add the text to
                # hicp and get the text ID that way.
                self.text_id = hicp.add_text_get_id(text)
            else:
                raise UnboundLocalError("hicp parameter required when text parameter is string")
        elif isinstance(text, list):
            if hicp is not None:
                # If it's a list of tubles and hicp is not None, then add the
                # list to hicp and get the text ID that way.
                self.text_id = hicp.add_groups_text_get_id(text)
            else:
                raise UnboundLocalError("hicp parameter required when text parameter is string")
        else:
            # No usable text parameter type.
            raise TypeError("text must be specified as integer ID or string with HICP parameter")

class Selection(ContainedComponent):
    MODE = Message.MODE
    SINGLE = Message.SINGLE
    MULTIPLE = Message.MULTIPLE

    PRESENTATION = Message.PRESENTATION
    SCROLL = Message.SCROLL
    TOGGLE = Message.TOGGLE
    DROPDOWN = Message.DROPDOWN

    ENABLED = Message.ENABLED
    DISABLED = Message.DISABLED
    UNSELECT = Message.UNSELECT

    class HeaderValues(ContainedComponent.HeaderValues):
        def __init__(self):
            self.item_dict = {} # Key is int, value is SelectionItem
            self.items = None

            self.selected_list = [] # ID is int
            self.selected = None

            self.mode = None
            self.presentation = None
            self.height = None
            self.width = None

            super().__init__()

        def set_from(self, other):
            super().set_from(other)

            self.item_dict = other.item_dict.copy()
            self.items = other.items

            self.selected_list = other.selected_list.copy()
            self.selected = other.selected

            self.mode = other.mode
            self.presentation = other.presentation
            self.height = other.height
            self.width = other.width

    def __init__(self):
        ContainedComponent.__init__(self)
        self.component = Component.SELECTION

    def set_items(self, item_list):
        # TODO item id is part of SelectionItem, so take a list as a parameter,
        # and construct the dict from that.
        """item_list is a dict of int id and SelectionItem."""
        self.current.item_dict = {}
        self.add_items(item_list)

    def add_items(self, item_list):
        """item_list is a dict of int id and SelectionItem."""
        for item in item_list:
            self.current.item_dict[item.item_id] = item
        self.items_changed()

    def del_items(self, item_list):
        """item_list is a list of int ids."""
        for item_key in item_list:
            try:
                del self.current.item_dict[item_key]
            except KeyError:
                # Not there, don't want it there, situation is what we want.
                pass
        self.items_changed()

    def get_item(self, item_id):
        """Returns SelectionItem."""
        return self.current.item_dict[item_id]

    def copy_items(self):
        """Returns dict of int id and SelectionItem."""
        return self.current.item_dict.copy()

    def items_changed(self):
        item_str_list = []
        for item_id, selection_item in self.current.item_dict.items():
            if selection_item.events is not None:
                item_str_list.append(
                    '%d: text=%d, events=%s' %
                        (selection_item.item_id,
                         selection_item.text_id,
                         selection_item.events) )
            else:
                item_str_list.append(
                    '%d: text=%d' %
                        (selection_item.item_id,
                         selection_item.text_id) )

        self.current.items = '\r\n'.join(item_str_list)

        # Delete selection (it needs to be set with IDs for these items if you
        # want to keep the selections)
        self.set_selected_list(None)

    def set_selection_mode(self, mode):
        self.current.mode = mode

    def get_selection_mode(self):
        return self.current.mode

    def set_presentation(self, presentation):
        self.current.presentation = presentation

    def set_selected_string(self, selected_list_str):
        # Split by ','.
        # ''.split(',') will create a 1 element list of [''], not a 0
        split_str = selected_list_str.split(',')
        try:
            selected_list = [int(s) for s in split_str]
            self.set_selected_list(selected_list)
        except ValueError:
            # Not valid selection string
            self.current.selected_list = []
            self.current.selected = None

    def set_selected_list(self, selected_list):
        # Verify that selected items are actual items
        valid_list = []
        valid_str_list = []
        if selected_list is not None:
            for selected_item in selected_list: # Integers
                if selected_item in self.current.item_dict:
                    valid_list.append(selected_item)
                    valid_str_list.append(str(selected_item))

        self.current.selected_list = valid_list
        self.current.selected = ", ".join(valid_str_list)

    def add_selected_item(self, item_id):
        if self.current.selected_list is None:
            self.current.selected_list = [item_id]
        else:
            if item_id in self.current.item_dict:
                self.current.selected_list.append(item_id)
            
        self._update_selected()

    def del_selected_item(self, item_id):
        try:
            self.current.selected_list.remove(item_id)

            self._update_selected()
        except ValueError:
            # Not there, so removed, so ignore
            pass

    # Update selected string from selected list and add to changed header list.
    def _update_selected(self):
        valid_str_list = [str(s) for s in self.current.selected_list]
        self.current.selected = ", ".join(valid_str_list)

    def copy_selected_list(self):
        """Returns a copy of the selected list."""
        return self.current.selected_list.copy()

    def get_selected_item_list(self):
        """Returns a list of SelectionItem."""
        item_list = []
        for item_id in self.current.selected_list:
            try:
                item = self.current.item_dict[item_id]
                item_list.append(item)
            except IndexError:
                # Selection out of sync with item list, skip this selection
                pass
        return item_list

    def set_height(self, height):
        self.current.height = str(height)

    def set_width(self, width):
        self.current.width = str(width)

    def fill_headers_add(self, message):
        super().fill_headers_add(message)
        # "items" from item list
        message.add_header(Message.ITEMS, self.current.items)
        message.add_header(Message.MODE, self.current.mode)
        message.add_header(Message.PRESENTATION, self.current.presentation)
        if len(self.current.selected_list) > 0:
            message.add_header(Message.SELECTED, self.current.selected)
        message.add_header(Message.HEIGHT, self.current.height)
        message.add_header(Message.WIDTH, self.current.width)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)
        # Assume items string is always a canonical representation (always the
        # same if items are the same).
        if self.sent.items != self.current.items:
            message.add_header(Message.ITEMS, self.current.items)

        if self.sent.mode != self.current.mode:
            message.add_header(Message.MODE, self.current.mode)

        if self.sent.presentation != self.current.presentation:
            message.add_header(Message.PRESENTATION, self.current.presentation)

        if self.sent.selected != self.current.selected:
            message.add_header(Message.SELECTED, self.current.selected)

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
            return self.get_handle_changed(event)
        else:
            # Not an event type this component supports.
            return None

    def get_handle_changed(self, event):
        # Get selection from event.
        selection_header = event.message.get_header(Message.SELECTED)
        if selection_header is not None:
            self.set_selected_string(selection_header)

        try:
            return self.__handle_changed
        except AttributeError:
            # Hasn't been set.
            return None


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
#        if self.hicp is None:
        if not self.is_added():
            return
        self.hicp.add(component)
        # If that's a Container, add its components too (it wouldn't have
        # self.hicp set, so would have skipped the above step).
        if isinstance(component, Container):
            component.add_component_list_to_hicp()

    def add_component_list_to_hicp(self):
        for component in self.__component_list:
            component.set_parent(self)
            self.add_to_hicp(component)


class Panel(Container):
    class HeaderValues(Container.HeaderValues):
        def __init__(self):
            super().__init__()

#        def set_from(self, other):
#            super().set_from(other)

    def __init__(self):
        Container.__init__(self)

        self.component = Component.PANEL
        self.component_text = ComponentText(self)

    def set_text_id(self, text_id):
        self.component_text.set_text_id(text_id)

    def set_text(self, text, hicp):
        self.component_text.set_text_get_id(text, hicp)

    def set_groups_text(self, text_group_list, hicp):
        self.component_text.set_groups_text(text_group_list, hicp)

    def fill_headers_add(self, message):
        super().fill_headers_add(message)
        self.component_text.fill_headers_add(message)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)
        self.component_text.fill_headers_modify(message)

    def notify_sent(self):
        super().notify_sent()
        self.component_text.notify_sent()


class Window(Container):
    class HeaderValues(Container.HeaderValues):
        def __init__(self):
            self.visible = False
            super().__init__()

        def set_from(self, other):
            super().set_from(other)
            self.visible = other.visible

    def __init__(self):
        Container.__init__(self)

        self.component = Component.WINDOW
        self.component_text = ComponentText(self)

    def set_text_id(self, text_id):
        self.component_text.set_text_id(text_id)

    def set_text(self, text, hicp):
        self.component_text.set_text_get_id(text, hicp)

    def set_groups_text(self, text_group_list, hicp):
        self.component_text.set_groups_text(text_group_list, hicp)

    def set_visible(self, visible):
        self.current.visible = visible

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
            return self.get_handle_close()
        else:
            # Not an event type this component supports.
            return None

    def get_handle_close(self):
        try:
            return self.__handle_close
        except AttributeError:
            # Hasn't been set.
            return None

    def fill_headers_add(self, message):
        super().fill_headers_add(message)

        if self.current.visible is not None:
            message.add_header(Message.VISIBLE, "true")
        else:
            message.add_header(Message.VISIBLE, "false")

        self.component_text.fill_headers_add(message)

    def fill_headers_modify(self, message):
        super().fill_headers_modify(message)

        if self.sent.visible != self.current.visible:
            visible_value = "true" if self.current.visible else "false"
            message.add_header(Message.VISIBLE, visible_value)

        self.component_text.fill_headers_modify(message)

    def notify_sent(self):
        super().notify_sent()
        self.component_text.notify_sent()

