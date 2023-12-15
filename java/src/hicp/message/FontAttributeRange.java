package hicp.message;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class FontAttributeRange {

    public enum FontName {
        DEFAULT(""),
        SERIF("serif"),
        SANS_SERIF("sans-serif"),
        SERIF_FIXED("serif-fixed"),
        SANS_SERIF_FIXED("sans-serif-fixed");

        public final String name;

        private static final Map<String, FontName> enumMap =
            Arrays.stream(FontName.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );

        FontName(final String forName) {
            this.name = forName;
        }

        public static FontName getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public final int length;

    public final String name;
    public final FontName fontName;

    public FontAttributeRange(
        final int newLength,
        final String value
    ) {
        length = newLength;
        name = value;

        if (null != value) {
            // May be null if not recognized.
            fontName = FontName.getEnum(value);
        } else {
            // Not specified.
            fontName = FontName.DEFAULT;
        }
    }

    public FontAttributeRange(
        final FontAttributeRange otherFontAttributeRange
    ) {
        length = otherFontAttributeRange.length;

        name = otherFontAttributeRange.name;
        fontName = otherFontAttributeRange.fontName;
    }

    public FontAttributeRange appendTo(final StringBuilder sb) {
        sb.append(length);
        if (FontName.DEFAULT != fontName) {
            sb.append("=").append(name);
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
