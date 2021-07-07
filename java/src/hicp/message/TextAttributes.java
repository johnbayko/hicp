package hicp.message;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.Message;

public class TextAttributes {
    private static final Logger LOGGER =
        Logger.getLogger( TextAttributes.class.getName() );

    public static class TextAttributesException extends Exception {
        public TextAttributesException(final String msg) {
            super(msg);
        }
    }

    public static class NoAttributeRange extends TextAttributesException {
        public NoAttributeRange(final String msg) {
            super(msg);
        }
    }

    public static class AttributeRange {
        private static final Logger LOGGER =
            Logger.getLogger( AttributeRange.class.getName() );

        public final String value;
        public int length;

        private int hashCode;

        public AttributeRange(final String attributeRangeStr)
            throws NoAttributeRange
        {
            // Split by = to convert to value and length,
            final String[] valueLengthSplit =
                Message.KEY_VALUE_SPLITTER.split(attributeRangeStr);

            final String lengthStr;

            if (1 == valueLengthSplit.length) {
                // Is binary attribute: "3, 2, 4"
                value = "";
                lengthStr = attributeRangeStr;
            } else {
                // Is value attribute: "a=3, b=2, a=4"
                value = valueLengthSplit[0];
                lengthStr = valueLengthSplit[1];
            }
            try {
                length = Integer.decode(lengthStr);
            } catch (NumberFormatException ex) {
                throw new NoAttributeRange("Invalid length: " + lengthStr);
            }
            setHashCode();
        }

        public AttributeRange(
            final AttributeRange otherAttributeRange
        ) {
            value = otherAttributeRange.value;
            length = otherAttributeRange.length;

            setHashCode();
        }

        public AttributeRange(
            final String newValue,
            final int newLength
        ) {
            value = newValue;
            length = newLength;

            setHashCode();
        }

        protected void setHashCode() {
            hashCode = Arrays.hashCode(new int[] {value.hashCode(), length});
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            appendTo(sb);
            return sb.toString();
        }

        public void appendTo(final StringBuilder sb) {
            if (!"".equals(value)) {
                sb.append(value).append("=");
            }
            sb.append(Integer.toString(length));
        }

        @Override
        public boolean equals(final Object o) {
            if (null == o) {
                return false;
            }
            if (!(o instanceof AttributeRange)) {
                return false;
            }
            return equals((AttributeRange)o);
        }

        public boolean equals(
            final AttributeRange otherAttributeRange
        ) {
            if (null == otherAttributeRange) {
                return false;
            }
            return ((value == otherAttributeRange.value)
                 && (length == otherAttributeRange.length)
            );
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }


    public static class NoAttributeTypeInfo extends TextAttributesException {
        public NoAttributeTypeInfo(final String msg) {
            super(msg);
        }
    }

    public static class AttributeTypeInfo {
        private static final Logger LOGGER =
            Logger.getLogger( AttributeTypeInfo.class.getName() );

        public final String name;
        public final List<AttributeRange> attributeRangeList;

        public boolean hasValues = false;

        public AttributeTypeInfo(
            final String attributeTypeStr,
            final int contentLength
        )
            throws TextAttributesException
        {
            // For each string, split to attribute and indexes by ":"
            final String[] attributeTypeInfoSplit =
                Message.COLON_SPLITTER.split(attributeTypeStr);

            // Must have at least one ":", non empty attribute. Ignore
            // additional ":" separators.
            if (2 > attributeTypeInfoSplit.length) {
                throw new NoAttributeTypeInfo(
                    "Attribute type or range list missing: \""
                        + attributeTypeStr
                        + "\""
                );
            }
            name = attributeTypeInfoSplit[0];
            if ("".equals(name)) {
                throw new NoAttributeTypeInfo(
                    "Attribute type missing: \"" + attributeTypeStr + "\""
                );
            }
            // TODO: determine whether attributes are value or binary based on
            // name. Probably add this info in message classes somewhere.

            final String rangeStr = attributeTypeInfoSplit[1];

            // Make list of AttributeRange objects
            attributeRangeList = new ArrayList<AttributeRange>();

            // Split range list string by "," to get range string list.
            // Empty range list is allowed.
            final String[] rangeStrList =
                Message.COMMA_SPLITTER.split(rangeStr);

            for (var valueLengthStr : rangeStrList) {
                try {
                    final var attributeRange =
                        new AttributeRange(valueLengthStr);

                    attributeRangeList.add(attributeRange);

                    if (!"".equals(attributeRange.value)) {
                        hasValues = true;
                    }
                } catch (NoAttributeRange ex) {
                    // Just skip this range - the indexing will be
                    // messed up, hopefully the user will complain
                    // and it'll get fixed.
                }
            }
            if (!hasValues && !hasRanges()) {
                // Binary attribute, but none specified. Must have at least
                // "off" attribute for entire string.
                // TODO: hasValues will be false even for non-binary, if
                // hasRanges() is false (none specified, can't tell if binary
                // or not). Need to encode this based on name at some point.
                attributeRangeList.add(
                    new AttributeRange("", contentLength)
                );
            }
        }

        public AttributeTypeInfo(
            final AttributeTypeInfo otherAttributeTypeInfo
        ) {
            name = otherAttributeTypeInfo.name;
            attributeRangeList = new ArrayList<AttributeRange>();

            for (var otherAttributeRange :
                otherAttributeTypeInfo.attributeRangeList
            ) {
                final var newAttributeRange =
                    new AttributeRange(otherAttributeRange);

                attributeRangeList.add(newAttributeRange);
            }

            hasValues = otherAttributeTypeInfo.hasValues;
        }

        public boolean hasRanges() {
            return (0 < attributeRangeList.size());
        }

        public boolean shouldIncludeString() {
            if (hasValues) {
                // Include if has any attributes.
                return (0 < attributeRangeList.size());
            } else {
                // Binary attributes start with "off" attribute, if that's the
                // only one then don't include it.
                return (1 < attributeRangeList.size());
            }
        }

        private void insertAtStart(final int len) {
            if (!hasRanges()) {
                // No attribute range to extend.
                return;
            }
            final var attributeRange = attributeRangeList.get(0);

            if (null == attributeRange) {
                // Should never happen.
                // No attribute range to extend here either.
                return;
            }
            attributeRange.length += len;
        }

        private void insertAfterStart(final int offset, final int len) {
            // Increase the length of whatever range this is inserted to.
            int rangeStart = 0;
            for (final var attributeRange : attributeRangeList) {
                final int nextRangeStart = rangeStart + attributeRange.length;
                /*
                    Typically inserting after the end of a range extends it,
                    rather than extending the next range. E.g for underline
                    (first position is 0):
                      "abc"
                        -
                    Insert "b" at position 2:
                      "abbc"
                        --
                    But insert "b" at position 1:
                      "abbc"
                         -
                    Need a special case for insert at 0.
                 */
                if ((rangeStart < offset) && (nextRangeStart >= offset)) {
                    attributeRange.length += len;

                    // Doesn't affect any other attribute range.
                    break;
                }
                rangeStart = nextRangeStart;
            }
        }

        // Should model actual behaviour when component that supports text
        // attributes is used, but this works for now.
        public void insert(final int offset, final int len) {
            if (0 == offset) {
                insertAtStart(len);
            } else {
                insertAfterStart(offset, len);
            }
        }

        // Should model actual behaviour when component that supports text
        // attributes is used, but this works for now.
        public void remove(final int removeOffsetStart, final int len) {
            final int removeOffsetLim = removeOffsetStart + len;

            // Find all ranges covered by this removal. Shorten ranges which
            // just overlap, delete ranges which are within the removal.
            int deletedLen = 0;
            int rangeStart = 0;
            int attributeIdx = 0;
            final int attributeIdxLim = attributeRangeList.size();
            while (attributeIdx < attributeIdxLim) {
                final var attributeRange = attributeRangeList.get(attributeIdx);
                final int nextRangeStart = rangeStart + attributeRange.length;

                /*
                    Range to remove: |   |
                    Attribute range: +---+

                        +---+ |   |   Do nothing
                            +---+ |   Reduce length by overlap
                            +-----+   Reduce length by overlap (= len)
                            +-------+ Reduce length by len
                              +--+|   Remove range
                              |+-+|   Remove range
                              +---+   Remove range
                              |+--+   Remove range
                              | +---+ Reduce length by overlap and quit.
                              +-----+ Reduce length by overlap (= len) and quit.
                 */
                {
                    final boolean willRemoveRangeStart =
                        (rangeStart >= removeOffsetStart);

                    final boolean willRemoveRangeEnd =
                        (nextRangeStart <= removeOffsetLim);

                    if (nextRangeStart <= removeOffsetStart) {
                        // No overlap, skip.
                        // This is necessary to simplify the other range checks.
                    } 
                    else if (!willRemoveRangeStart && willRemoveRangeEnd)
                    {
                        // Remove from right of range.
                        final int overlap = nextRangeStart - removeOffsetStart;
                        attributeRange.length -= overlap;
                    } 
                    else if (!willRemoveRangeStart && !willRemoveRangeEnd)
                    {
                        // Remove from middle fo range.
                        attributeRange.length -= len;
                    } 
                    else if (willRemoveRangeStart && willRemoveRangeEnd)
                    {
                        // All of range removed.
                        attributeRange.length = 0;
                    }
                    else if (willRemoveRangeStart && !willRemoveRangeEnd)
                    {
                        // Remove from left of range.
                        final int overlap = removeOffsetLim - rangeStart;
                        attributeRange.length -= overlap;
                    }
                }
                // Do actual delete,
                // or clean up after one or more ranges deleted.
                final boolean shouldDelete = (0 == attributeRange.length);

                boolean didDelete = false;
                if (shouldDelete) {
                    // Do actual delete now.
                    if ((attributeIdx == 0) && !hasValues) {
                        // Special case, binary attribute always has to start
                        // with "off", so instead of deleting, set length to 0.
                        attributeRange.length = 0;
                        // TODO: length is already 0, that's what makes shouldDelete true.
                    } else {
                        attributeRangeList.remove(attributeIdx);
                        didDelete = true;
                        deletedLen++;
                    }
                }
                // If this was deleted, then the current index is gone,
                // next has shifted to current, so dont increment
                // attributeIdx then - only if not deleted.
                if (!didDelete) {
                    attributeIdx++;
                }
                if (nextRangeStart >= removeOffsetLim) {
                    // Next range is past end of remove range, no more ranges
                    // to check.
                    break;
                }
                rangeStart = nextRangeStart;
            }

            // Done deleting. Is there an attribute range left?
            if (attributeIdx < attributeRangeList.size()) {
                // Yes, might need to merge attribute ranges.

                final var attributeRange = attributeRangeList.get(attributeIdx);
                if (0 < deletedLen) {
                    // Is there a previous range to merge this one with?
                    if (0 < attributeIdx) {
                        final int prevIdx = attributeIdx - 1;
                        final var prevRange = attributeRangeList.get(prevIdx);

                        // If the attribute value after the deleted range
                        // is the same as before (e.g. "a=2, b=1, a=2"
                        // becoems "a=2, a=2"), merge them (e.g.  "a=4").
                        final boolean shouldMergeValues =
                                hasValues
                             && prevRange.value.equals(attributeRange.value);

                        // If binary attribute and number of deletes is
                        // odd, then this disrupts on-off-on-off pattern -
                        // on-on-off will not be interpreted correctly, so
                        // merge on-on so it becomes on-off again.
                        final boolean isDeletedLenOdd =
                            0 != (deletedLen % 2);
                        final boolean shouldMergeBinary =
                                !hasValues
                             && isDeletedLenOdd;

                        if (shouldMergeValues || shouldMergeBinary) {
                            prevRange.length += attributeRange.length;

                            attributeRangeList.remove(attributeIdx);
                        }
                    }
                }
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            appendTo(sb);
            return sb.toString();
        }

        public void appendTo(final StringBuilder sb) {
            sb.append(name)
                .append(": ");

            String sep = "";
            for (var attributeRange : attributeRangeList) {
                sb.append(sep);
                sep = ", ";

                attributeRange.appendTo(sb);
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (null == o) {
                return false;
            }
            if (!(o instanceof AttributeTypeInfo)) {
                return false;
            }
            return equals((AttributeTypeInfo)o);
        }

        public boolean equals(
            final AttributeTypeInfo otherAttributeTypeInfo
        ) {
            if (null == otherAttributeTypeInfo) {
                return false;
            }
            if (!name.equals(otherAttributeTypeInfo.name)) {
                return false;
            }
            if ( !attributeRangeList.equals(
                    otherAttributeTypeInfo.attributeRangeList
                )
            ) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(
                new int[] {name.hashCode(), attributeRangeList.hashCode()}
            );
        }
    }

    private Map<String, AttributeTypeInfo> attributeTypesMap =
        new HashMap<>();

    /*
        Parse the attribute list on input. Ignore errors, hopefully
        they'll be user visible and lead to complaints and fixes.
        Example string:
            bold: 10, 5, 10\r\n
            font: sans-serif=5, serif-fixed=10, sans-serif=10\r\n
            size: 1=10, 1.1=5, 1=9, 1/2=1\r\n
    */
    public TextAttributes(final String attributesStr, final int contentLength) {
        if (null == attributesStr) {
            // No attributes.
            return;
        }
        // Split string by line
        final String[] attributeTypeStrList =
            Message.LINE_SPLITTER.split(attributesStr);
        if (0 == attributeTypeStrList.length) {
            // No attributes.
            return;
        }
        for (var attributeTypeStr : attributeTypeStrList) {
            try {
                final var attributeTypeInfo =
                    new AttributeTypeInfo(attributeTypeStr, contentLength);

                attributeTypesMap
                    .put(attributeTypeInfo.name, attributeTypeInfo);
            } catch (TextAttributesException ex) {
                // Just skip, fix the source if the user complains.
            }
        }
    }

    /*
        Copy attributes.
     */
    public TextAttributes(final TextAttributes otherTextAttributes) {
        if (null == otherTextAttributes) {
            // Leave as default / empty, can add attributes later.
            return;
        }
        for (var attributeKey: otherTextAttributes.attributeTypesMap.keySet()) {
            final var otherAttributeTypeInfo =
                otherTextAttributes.attributeTypesMap.get(attributeKey);

            final var newAttributeTypeInfo =
                new AttributeTypeInfo(otherAttributeTypeInfo);

            attributeTypesMap.put(attributeKey, newAttributeTypeInfo);
        }
    }

    // Apply insert to all attribute types.
    public void insert(final int offset, final int len) {
        for (final var attributeKey : attributeTypesMap.keySet()) {
            final var attributeTypeInfo = attributeTypesMap.get(attributeKey);
            attributeTypeInfo.insert(offset, len);
        }
    }

    // Apply remove to all attribute type.
    public void remove(final int offset, final int len) {
        for (final var attributeKey : attributeTypesMap.keySet()) {
            final var attributeTypeInfo = attributeTypesMap.get(attributeKey);
            attributeTypeInfo.remove(offset, len);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        for (final var attributeKey : attributeTypesMap.keySet()) {
            final var attributeTypeInfo = attributeTypesMap.get(attributeKey);
            if (attributeTypeInfo.shouldIncludeString()) {
                sb.append(sep);
                sep = "\r\n";

                attributeTypeInfo.appendTo(sb);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (null == o) {
            return false;
        }
        if (!(o instanceof TextAttributes)) {
            return false;
        }
        return equals((TextAttributes)o);
    }

    public boolean equals(final TextAttributes otherTextAttributes) {
        if (null == otherTextAttributes) {
            return false;
        }
        if (!attributeTypesMap.equals(otherTextAttributes.attributeTypesMap)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return attributeTypesMap.hashCode();
    }
}
