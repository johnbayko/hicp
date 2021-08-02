package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.TextDirection;
import hicp.HeaderMap;
import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class LayoutGUIInfo {
    public static class DirectionPair {
        public final TextDirection first;
        public final TextDirection second;

        public DirectionPair() {
            first = null;
            second = null;
        }

        public static TextDirection getIfExists(
            final String[] directionStrArray,
            final int idx
        ) {
            if (idx >= directionStrArray.length) {
                return null;
            }
            return TextDirection.getEnum(directionStrArray[idx]);
        }

        public DirectionPair(final String directionPairStr) {
            // Only two values are needed, but if there are more,
            // shouldn't be appended to the second value by split(), so
            // split into three - any extra will be separated into third
            // String that's ignored.
            final String[] directionStrArray =
                Message.splitWith(Message.COMMA_SPLITTER, directionPairStr, 3);

            first = getIfExists(directionStrArray, 0);
            second = getIfExists(directionStrArray, 1);
        }

        public DirectionPair(
            final TextDirection newFirst,
            final TextDirection newSecond
        ) {
            first = newFirst;
            second = newSecond;
        }

        @Override
        public String toString() {
            return first.name + ", " + second.name;
        }

        // Missing equals() and hashcode().
    }

    // Null safe DirectionPair factory method.
    private static DirectionPair newDirectionPair(
        final String directionPairStr
    ) {
        if (null == directionPairStr) {
            return DEFAULT_TEXT_DIRECTION;
        }
        return new DirectionPair(directionPairStr);
    }

    public static final DirectionPair DEFAULT_TEXT_DIRECTION =
        new DirectionPair();

    public DirectionPair textDirection = DEFAULT_TEXT_DIRECTION;


    public LayoutGUIInfo() {
    }

    public LayoutGUIInfo(final HeaderMap headerMap) {
        textDirection =
            newDirectionPair(headerMap.getString(HeaderEnum.TEXT_DIRECTION));
    }

    public LayoutGUIInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap
            .putString(HeaderEnum.TEXT_DIRECTION, textDirection.toString());

        return this;
    }
}

