package hicp.message;

public class BooleanAttributeRange {
    public final int length;
    public final boolean isOn;

    public BooleanAttributeRange(
        final int newLength,
        final boolean newIsOn
    ) {
        length = newLength;
        isOn = newIsOn;
    }

    public BooleanAttributeRange(
        final BooleanAttributeRange otherBooleanAttributeRange
    ) {
        length = otherBooleanAttributeRange.length;
        isOn = otherBooleanAttributeRange.isOn;
    }

    public BooleanAttributeRange appendTo(final StringBuilder sb) {
        sb.append(length);
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        appendTo(sb);
        return sb.toString();
    }
}
