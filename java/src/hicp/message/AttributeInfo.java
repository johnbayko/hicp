package hicp.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttributeInfo {

    public enum AttributeName {
        // Has values.
        FONT("font", true),
//        LAYOUT("layout", true),
        SIZE("size", true),

        // No values.
        BOLD("bold", false),
        ITALIC("italic", false),
        UNDERLINE("underline", false);

        public final String name;
        public final boolean hasValues;

        private static final Map<String, AttributeName> enumMap =
            Arrays.stream(AttributeName.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );

        AttributeName(final String forName, final boolean hasValues) {
            this.name = forName;
            this.hasValues = hasValues;
        }

        public static AttributeName getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public final String name;
    public final AttributeName attributeName;

    public final int position;

    private final List<AttributeRange> rangeList;

    // This can be useful when deciding how to handle range list.
    private boolean hasValues = false;

    private List<FontAttributeRange> fontRangeList = null;
    private List<SizeAttributeRange> sizeRangeList = null;
    private List<BooleanAttributeRange> booleanRangeList = null;

    public AttributeInfo(final String attributeStr)
        throws AttributeException
    {
        // For each string, split parts by ":"
        final String[] attributeFields =
            Message.splitWith(Message.COLON_SPLITTER, attributeStr);

        // Must have at least two ":", non empty attribute. Ignore
        // additional ":" separators.
        if (3 > attributeFields.length) {
            throw new AttributeException(
                "Attribute name, position, or range list missing: \""
                    + attributeStr
                    + "\""
            );
        }
        name = attributeFields[0];
        if ("".equals(name)) {
            throw new AttributeException(
                "Attribute type missing: \"" + attributeStr + "\""
            );
        }
        attributeName = AttributeName.getEnum(name);
        {
            final String positionStr = attributeFields[1];
            try {
                position = Integer.parseInt(positionStr);
            } catch (NumberFormatException ex) {
                throw new AttributeException("Invalid position: " + positionStr);
            }
        }
        // Make list of AttributeRange objects
        rangeList = new ArrayList<>();
        {
            final String rangeListStr = attributeFields[2];

            // Split range list string by "," to get range string list.
            // Empty range list is allowed.
            final String[] rangeStrList =
                Message.splitWith(Message.COMMA_SPLITTER, rangeListStr);

            boolean foundValue = false;
            for (var rangeStr : rangeStrList) {
                try {
                    final var range = new AttributeRange(rangeStr);

                    rangeList.add(range);

                    if (null != range.value) {
                        foundValue = true;
                    }
                } catch (AttributeException ex) {
                    // Just skip this range - the indexing will be
                    // messed up, hopefully the user will complain
                    // and it'll get fixed at the source.
                }
            }
            // An attribute with values can specify a default by omitting the
            // "=value" part. Normally at least one value should be specified,
            // (or there's no point in sending ranges for an attribute), but
            // it's still legal to send all ranges as default (e.g. "10, 5, 10"
            // is the same as "25").
            // If an attribute is known, use the attribute hasValue info to be
            // sure. Otherwise, can only guess based on whether any values were
            // actually found.
            hasValues =
                (null != attributeName)
                    ? attributeName.hasValues
                    : foundValue;
        }
    }

    public AttributeInfo(
        final String newName,
        final int newPosition,
        final List<AttributeRange> newRangeList
    ) {
        name = newName;
        attributeName = AttributeName.getEnum(newName);

        position = newPosition;

        rangeList = newRangeList;
    }

    public AttributeInfo(final AttributeInfo otherAttributeInfo) {
        name = otherAttributeInfo.name;
        attributeName = otherAttributeInfo.attributeName;

        position = otherAttributeInfo.position;

        rangeList = new ArrayList<>();
        // Copy rangeList so it's independent.
        for (final var range : otherAttributeInfo.rangeList) {
            final var newRange = new AttributeRange(range);
            rangeList.add(newRange);
        }
    }

    public boolean hasRanges() {
        return (0 < rangeList.size());
    }

    public List<AttributeRange> getRangeList() {
        return rangeList;
    }

    public boolean hasValues() {
        return hasValues;
    }

    public List<FontAttributeRange> getFontRangeList() {
        if (null == fontRangeList) {
            // Make a font attribute list from the attribute range values.
            fontRangeList = new ArrayList<>();

            for (var range : rangeList) {
                final var fontRange =
                    new FontAttributeRange(range.length, range.value);
                fontRangeList.add(fontRange);
            }
        }
        return fontRangeList;
    }

    public List<SizeAttributeRange> getSizeRangeList()
        throws AttributeException
    {
        if (null == sizeRangeList) {
            // Make a size attribute list from the attribute range values.
            sizeRangeList = new ArrayList<>();

            for (var range : rangeList) {
                final var sizeRange =
                    new SizeAttributeRange(range.length, range.value);
                sizeRangeList.add(sizeRange);
            }
        }
        return sizeRangeList;
    }

    public List<BooleanAttributeRange> getBooleanRangeList() {
        if (null == booleanRangeList) {
            // Make a size attribute list from the attribute range values.
            booleanRangeList = new ArrayList<>();

            // First range is on.
            boolean isOn = true;
            for (var range : rangeList) {
                final var booleanRange =
                    new BooleanAttributeRange(range.length, isOn);
                isOn = !isOn;

                booleanRangeList.add(booleanRange);
            }
        }
        return booleanRangeList;
    }

    public AttributeInfo appendTo(final StringBuilder sb) {
        sb.append(name).append(": ")
            .append(position).append(": ");

        String sep = "";
        for (var range : rangeList) {
            sb.append(sep);
            sep = ", ";

            range.appendTo(sb);
        }
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        appendTo(sb);
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (null == o) {
            return false;
        }
        if (!(o instanceof AttributeInfo)) {
            return false;
        }
        return equals((AttributeInfo)o);
    }

    public boolean equals(
        final AttributeInfo otherAttributeInfo
    ) {
        if (null == otherAttributeInfo) {
            return false;
        }
        if (!name.equals(otherAttributeInfo.name)) {
            return false;
        }
        if (position != otherAttributeInfo.position) {
            return false;
        }
        if ( !rangeList.equals(
                otherAttributeInfo.rangeList
            )
        ) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(
            new int[] { name.hashCode(), position, rangeList.hashCode() }
        );
    }
}
