package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class GUIInfo {
    public static enum ComponentEnum {
         BUTTON("button"),
         LABEL("label"),
         PANEL("panel"),
         SELECTION("selection"),
         TEXTPANEL("textpanel"),
         TEXTFIELD("textfield"),
         WINDOW("window");

        public final String name;

        private static final Map<String, ComponentEnum> enumMap =
            Arrays.stream(ComponentEnum.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        ComponentEnum(final String forName) {
            name = forName;
        }

        public static ComponentEnum getEnum(String name) {
            return enumMap.get(name);
        }
    }

    // I don't like the name, should imply horizontal and vertical values.
    public static class IntPair {
        public final int horizontal;
        public final int vertical;

        public IntPair(final String intPairStr) {
            // Only two values are needed, but if there are more,
            // shouldn't be appended to the second value by split(), so
            // split into three - any extra will be separated into third
            // String that's ignored.
            final String[] positions =
                Message.COMMA_SPLITTER.split(intPairStr, 3);

            int gotHorizontal = 0;
            if (0 < positions.length) {
                try {
                    gotHorizontal = Integer.parseInt(positions[0]);
                } catch (NumberFormatException ex) {
                    gotHorizontal = 0;
                }
            }
            horizontal = gotHorizontal;

            int gotVertical = 0;
            if (1 < positions.length) {
                try {
                    gotVertical = Integer.parseInt(positions[1]);
                } catch (NumberFormatException ex) {
                    gotVertical = 0;
                }
            }
            vertical = gotVertical;
        }

        public IntPair(final int forHorizontal, final int forVertical) {
            horizontal = forHorizontal;
            vertical = forVertical;
        }

        @Override
        public String toString() {
            return horizontal + ", " + vertical;
        }

        // Missing equals() and hashcode().
    }

    // Null safe IntPair factory method.
    private static IntPair newIntPair(final String intPairStr) {
        if (null == intPairStr) {
            return null;
        }
        return new IntPair(intPairStr);
    }

    public ComponentEnum component = null;
    public String parent = null;
    public IntPair position = null;
    public IntPair size = null;


    public GUIInfo() {
    }

    public GUIInfo(final HeaderMap headerMap) {
        component =
            ComponentEnum.getEnum(headerMap.getString(HeaderEnum.COMPONENT));
        parent = headerMap.getString(HeaderEnum.PARENT);
        position = newIntPair(headerMap.getString(HeaderEnum.POSITION));
        size = newIntPair(headerMap.getString(HeaderEnum.SIZE));
    }

    public GUIInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.COMPONENT, component.name);
        headerMap.putString(HeaderEnum.PARENT, parent);
        headerMap.putString(HeaderEnum.POSITION, position.toString());
        headerMap.putString(HeaderEnum.SIZE, size.toString());

        return this;
    }
}
