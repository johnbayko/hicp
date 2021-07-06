package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class ContainedGUIInfo {
    public static class LayoutPair {
        public final int horizontal;
        public final int vertical;

        public LayoutPair() {
            horizontal = 0;
            vertical = 0;
        }

        public static int parseIfExists(
            final String[] intStrArray,
            final int intToParse
        ) {
            if (intToParse >= intStrArray.length) {
                return 0;
            }
            try {
                return Integer.parseInt(intStrArray[intToParse]);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        public LayoutPair(final String intPairStr) {
            // Only two values are needed, but if there are more,
            // shouldn't be appended to the second value by split(), so
            // split into three - any extra will be separated into third
            // String that's ignored.
            final String[] intStrArray =
                Message.COMMA_SPLITTER.split(intPairStr, 3);

            horizontal = parseIfExists(intStrArray, 0);
            vertical = parseIfExists(intStrArray, 1);
        }

        public LayoutPair(final int forHorizontal, final int forVertical) {
            horizontal = forHorizontal;
            vertical = forVertical;
        }

        @Override
        public String toString() {
            return horizontal + ", " + vertical;
        }

        // Missing equals() and hashcode().
    }

    // Null safe LayoutPair factory method.
    private static LayoutPair newIntPair(final String intPairStr) {
        if (null == intPairStr) {
            return DEFAULT_LAYOUT_PAIR;
        }
        return new LayoutPair(intPairStr);
    }

    public static final LayoutPair DEFAULT_LAYOUT_PAIR = new LayoutPair();

    public String parent = null;
    public LayoutPair position = DEFAULT_LAYOUT_PAIR;
    public LayoutPair size = DEFAULT_LAYOUT_PAIR;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;


    public ContainedGUIInfo() {
    }

    public ContainedGUIInfo(final HeaderMap headerMap) {
        parent = headerMap.getString(HeaderEnum.PARENT);
        position = newIntPair(headerMap.getString(HeaderEnum.POSITION));
        size = newIntPair(headerMap.getString(HeaderEnum.SIZE));
    }

    public ContainedGUIInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.PARENT, parent);
        headerMap.putString(HeaderEnum.POSITION, position.toString());
        headerMap.putString(HeaderEnum.SIZE, size.toString());

        return this;
    }
}
