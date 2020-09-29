package hicp.message;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TextAttributes {
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
        public final String value;
        public int length;

        private int hashCode;

        public AttributeRange(final String attributeRangeStr)
            throws NoAttributeRange
        {
            // Split by = to convert to value and length,
            final String[] valueLengthSplit =
                keyValueSplitter.split(attributeRangeStr);

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
        public final String name;
        public final List<AttributeRange> attributeRangeList;

        public AttributeTypeInfo(final String attributeTypeStr)
            throws TextAttributesException
        {
            // For each string, split to attribute and indexes by ":"
            final String[] attributeTypeInfoSplit =
                colonSplitter.split(attributeTypeStr);

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

            final String rangeStr = attributeTypeInfoSplit[1];

            // Make list of AttributeRange objects
            attributeRangeList = new ArrayList<AttributeRange>();

            // Split range list string by "," to get range string list.
            // Empty range list is allowed.
            final String[] rangeStrList = commaSplitter.split(rangeStr);

            for (var valueLengthStr : rangeStrList) {
                try {
                    final var attributeRange =
                        new AttributeRange(valueLengthStr);

                    attributeRangeList.add(attributeRange);
                } catch (NoAttributeRange ex) {
                    // Just skip this range - the indexing will be
                    // messed up, hopefully the user will complain
                    // and it'll get fixed.
                }
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
        }

        public void insert(final int offset, final int len) {
            // Increase the length of whatever range this is inserted to.
            int rangeStart = 0;
            for (final var attributeRange : attributeRangeList) {
                final int nextRangeStart = rangeStart + attributeRange.length;
                if ((rangeStart < offset) && (nextRangeStart > offset)) {
                    attributeRange.length += len;

                    // Doesn't affect any other attribute range.
                    break;
                }
                rangeStart += nextRangeStart;
            }
        }

        public void remove(final int removeOffsetStart, final int len) {
            // Find all ranges covered by this removal. Shorten ranges which
            // just overlap, delete ranges which are within the removal.

            final int removeOffsetLim = removeOffsetStart + len;
            int rangeStart = 0;
            final Iterator<AttributeRange> rangeIter =
                attributeRangeList.iterator();
            while (rangeIter.hasNext()) {
                final var attributeRange = rangeIter.next();
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
                final boolean includeRangeStart =
                    (rangeStart >= removeOffsetStart);

                final boolean includeRangeEnd =
                    (nextRangeStart <= removeOffsetLim);

                if (nextRangeStart <= removeOffsetStart) {
                    // No overlap, skip.
                    // This is necessary to simplify the other range checks.
                } 
                else if (!includeRangeStart && includeRangeEnd)
                {
                    final int overlap = nextRangeStart - removeOffsetStart;
                    attributeRange.length -= overlap;
                } 
                else if (!includeRangeStart && !includeRangeEnd)
                {
                    attributeRange.length -= len;
                } 
                else if (includeRangeStart && includeRangeEnd)
                {
                    rangeIter.remove();
                } 
                else if (includeRangeStart && !includeRangeEnd)
                {
                    final int overlap = removeOffsetLim - rangeStart;
                    attributeRange.length -= overlap;
                    break;
                }
                rangeStart += nextRangeStart;
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

    public static final Pattern lineSplitter =
        Pattern.compile("\r\n", Pattern.LITERAL);

    public static final Pattern commaSplitter =
        Pattern.compile("\\s,\\s");

    public static final Pattern keyValueSplitter =
        Pattern.compile("\\s=\\s");

    public static final Pattern colonSplitter =
        Pattern.compile("\\s:\\s");

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
    public TextAttributes(final String attributesStr) {
        if (null == attributesStr) {
            // No attributes.
            return;
        }
        // Split string by line
        final String[] attributeTypeStrList = lineSplitter.split(attributesStr);
        if (0 == attributeTypeStrList.length) {
            // No attributes.
            return;
        }
        for (var attributeTypeStr : attributeTypeStrList) {
            try {
                final var attributeTypeInfo =
                    new AttributeTypeInfo(attributeTypeStr);

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
        for (var attributeKey : otherTextAttributes.attributeTypesMap.keySet()) {
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
        for (final var attributeKey : attributeTypesMap.keySet()) {
            final var attributeTypeInfo = attributeTypesMap.get(attributeKey);
            attributeTypeInfo.appendTo(sb);
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
