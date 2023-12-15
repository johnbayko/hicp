package hicp.message;

import java.util.Arrays;

public class AttributeRange {
    public final int length;
    public final String value;

    private final int _hashCode;

    public AttributeRange(final String rangeStr)
        throws AttributeException
    {
        final String newValue;
        final int newLength;

        // Split by = to convert to lwngth and value.
        final String[] rangeStrList =
            Message.splitWith(Message.KEY_VALUE_SPLITTER, rangeStr);

        if (0 == rangeStrList.length) {
            // Not a valid attribute range
            throw new AttributeException("No range values");
        }

        if (1 <= rangeStrList.length) {
            final String lengthStr = rangeStrList[0];
            try {
                newLength = Integer.parseInt(lengthStr);
            } catch (NumberFormatException ex) {
                throw new AttributeException("Invalid length: " + lengthStr);
            }
        } else {
            throw new AttributeException("Range has no length: " +rangeStr);
        }

        if (2 <= rangeStrList.length) {
            newValue = rangeStrList[1];
        } else {
            newValue = null;
        }

        // Duplicates plain constructor, keep them synchronized.
        // Duplication needed because this() must be called first in a
        // constructor, but parameters aren't available until here.
        length = newLength;
        value = newValue;

        _hashCode =
            Arrays.hashCode(
                new int[] { length, (null == value) ? 0 : value.hashCode() }
            );
    }

    public AttributeRange(
        final AttributeRange otherAttributeRange
    ) {
        this(
            otherAttributeRange.length,
            otherAttributeRange.value
        );
    }

    public AttributeRange(
        final int newLength
    ) {
        this(newLength, null);
    }

    public AttributeRange(
        final int newLength,
        final String newValue
    ) {
        length = newLength;
        value = newValue;

        _hashCode =
            Arrays.hashCode(
                new int[] { length, (null == value) ? 0 : value.hashCode() }
            );
    }

    public AttributeRange appendTo(final StringBuilder sb) {
        sb.append(length);
        if (null != value) {
            sb.append("=").append(value);
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
        if (length != otherAttributeRange.length) {
            return false;
        }
        if (null == value) {
            if (value != otherAttributeRange.value) {
                return false;
            }
        } else {
            if (!value.equals(otherAttributeRange.value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return _hashCode;
    }
}
