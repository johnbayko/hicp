package hicp.message;

import java.util.regex.Pattern;

public class SizeAttributeRange {
    public final int length;
    public final double numerator;
    public final double denominator;

    private static final Pattern SIZE_SPLITTER =
        Pattern.compile("\\s*/\\s*");

    public SizeAttributeRange(
        final int newLength,
        final String value
    )
        throws AttributeException
    {
        length = newLength;
        
        if (null == value) {
            // Default size is 1.
            numerator = denominator = 1.0;
            return;
        }
        final String[] sizeStringList = 
            Message.splitWith(SIZE_SPLITTER, value);
        if (0 == sizeStringList.length) {
            throw new AttributeException("Unrecognized size: " + value);
        }
        if (1 <= sizeStringList.length) {
            try {
                final String numeratorStr = sizeStringList[0];

                numerator = Double.parseDouble(numeratorStr);
            } catch (NumberFormatException ex) {
                throw new AttributeException("Invalid size value: " + value);
            }
        } else {
            numerator = 1.0;
        }
        if (2 <= sizeStringList.length) {
            try {
                final String denominatorStr = sizeStringList[1];

                denominator = Double.parseDouble(denominatorStr);

                if (0.0 == denominator) {
                    throw new AttributeException("Size divided by 0: " + value);
                }
            } catch (NumberFormatException ex) {
                throw new AttributeException("Invalid size fraction: " + value);
            }
        } else {
            denominator = 1.0;
        }
    }

    public SizeAttributeRange(
        final SizeAttributeRange otherSizeAttributeRange
    ) {
        length = otherSizeAttributeRange.length;
        numerator = otherSizeAttributeRange.numerator;
        denominator = otherSizeAttributeRange.denominator;
    }

    /* Return the double value calculated from of the numerator and
       denominator. */
    public double getEffectiveSize() {
        if (0.0 != denominator) {
            return numerator / denominator;
        } else {
            // Probably covering up an error somewhere else.
            return numerator;
        }
    }

    public SizeAttributeRange appendTo(final StringBuilder sb) {
        sb.append(length);
        if ( (1.0 != numerator) || (1.0 != denominator) ) {
            sb.append("=").append(numerator);
            if (1.0 != denominator) {
                sb.append("/").append(denominator);
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
}
