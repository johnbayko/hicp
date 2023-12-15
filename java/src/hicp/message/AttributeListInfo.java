package hicp.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;

public class AttributeListInfo
    implements Iterable<AttributeInfo>
{
    private static final Logger LOGGER =
        Logger.getLogger( AttributeListInfo.class.getName() );

    private List<AttributeInfo> attributeList = new LinkedList<>();

    /* Empty attribute list, to be filled with attributes to create a new
       message. */
    public AttributeListInfo() {
    }

    /*
        Parse the attribute list on input. Ignore errors, hopefully
        they'll be user visible and lead to complaints and fixes.
        Format is:
            <attribute name>: <position>: <range list>
        Where range list is comma separated ranges with values or not.
        With values:
            <value> = <length>
        Without values (binary or default value):
            <length>
        Example string:
            underline: 10: 5\r\n
            bold: 0: 10, 5, 10\r\n
            font: 0: sans-serif=5, serif-fixed=10, sans-serif=10\r\n
            size: 10: 1.1=5, 9, 1/2=1\r\n
        Meaning:
            Underline 5 characters starting at position 10.
            Bold 10 characters starting at position 0 and 15.
            Set fonts for 5, 10, and 10 character ranges.
            Set size to 1.1 for 5 characters starting at position 10, use
            default size (1) for 9 characters, and size 1/2 for 1 character.
    */
    public AttributeListInfo(final HeaderMap headerMap) {
        final String attributeListStr =
            headerMap.getString(HeaderEnum.ATTRIBUTES);

        if (null == attributeListStr) {
            // No attributes.
            return;
        }
        // Split string by line
        final String[] attributeStrList =
            Message.splitWith(Message.LINE_SPLITTER, attributeListStr);
        if (0 == attributeStrList.length) {
            // No attributes.
            return;
        }
        for (var attributeStr : attributeStrList) {
            try {
                final var guiTextAttributeInfo =
                    new AttributeInfo(attributeStr);

                attributeList.add(guiTextAttributeInfo);
            } catch (AttributeException ex) {
                // Just skip, fix the source if the user complains.
            }
        }
    }

    public AttributeListInfo(final AttributeListInfo otherAttributeListInfo) {
        for (final var attributeInfo : otherAttributeListInfo.attributeList) {
            final var newAttributeInfo = new AttributeInfo(attributeInfo);
            attributeList.add(newAttributeInfo);
        }
    }

    /* Does not check for duplicates attribute names, because normally used to
       construct new attribute list so should be empty, with new attributes
       added once..
       TODO change to map by attribute name so existing attribute can be
       replaced. */
    public AttributeListInfo addAttributeInfo(
        final AttributeInfo newAttributeInfo
    ) {
        attributeList.add(newAttributeInfo);
        return this;
    }

    public AttributeListInfo updateHeaderMap(final HeaderMap headerMap) {
        headerMap.putString(HeaderEnum.ATTRIBUTES, toString());
        return this;
    }

    public boolean hasAttributes() {
        // Are there any named attributes, and do they all have attribute
        // ranges?
        if (attributeList.isEmpty()) {
            return false;
        }
        for (final var attributeInfo : attributeList) {
            if (attributeInfo.hasRanges()) {
                return true;
            }
        }
        // Found nothing.
        return false;
    }

    public AttributeListInfo appendTo(final StringBuilder sb) {
        String sep = "";
        for (final var attribute : attributeList) {
            /* Should this be added to the attribute string? If the attribute
              range does not actually affect the content, then no need to
              include it. */
            if (attribute.hasRanges()) {
                sb.append(sep);
                sep = "\r\n";

                attribute.appendTo(sb);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        appendTo(sb);
        return sb.toString();
    }

    // Iterable:
    public void forEach(Consumer<? super AttributeInfo> action) {
        attributeList.forEach(action);
    }

    public Iterator<AttributeInfo> iterator() {
        return attributeList.iterator();
    }

    public Spliterator<AttributeInfo> spliterator() {
        return attributeList.spliterator();
    }

    @Override
    public boolean equals(final Object o) {
        if (null == o) {
            return false;
        }
        if (!(o instanceof AttributeListInfo)) {
            return false;
        }
        return equals((AttributeListInfo)o);
    }

    public boolean equals(final AttributeListInfo otherAttributeListInfo) {
        if (null == otherAttributeListInfo) {
            return false;
        }
        if (!attributeList.equals(otherAttributeListInfo.attributeList)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return attributeList.hashCode();
    }
}
