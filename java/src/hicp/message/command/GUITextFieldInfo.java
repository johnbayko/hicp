package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.TextAttributes;

public class GUITextFieldInfo {
    public static enum EventsEnum {
         ENABLED("enabled"),
         DISABLED("disabled");

        public final String name;

        private static final Map<String, EventsEnum> enumMap =
            Arrays.stream(EventsEnum.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        EventsEnum(final String forName) {
            name = forName;
        }

        public static EventsEnum getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public String content = null;
    public int width = 7;  // Arbitrary default
    public boolean hasWidth = false;
    public TextAttributes attributes = null;
    public EventsEnum events = null;

    public GUITextFieldInfo() {
    }

    public GUITextFieldInfo(final HeaderMap headerMap) {
        content = headerMap.getString(HeaderEnum.CONTENT);
        {
            final boolean hasWidth = headerMap.has(HeaderEnum.WIDTH);
            if (hasWidth) {
                width = headerMap.getInt(HeaderEnum.WIDTH, width);
            }
        }
        {
            final String attributesStr =
                headerMap.getString(HeaderEnum.ATTRIBUTES);
            if (null != attributesStr) {
                attributes = new TextAttributes(attributesStr, content.length());
            }
        }
        events =
            EventsEnum.getEnum(
                headerMap.getString(HeaderEnum.EVENTS)
            );
    }

    public GUITextFieldInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.CONTENT, content);
        headerMap.putInt(HeaderEnum.WIDTH, width);

        // Warning: It's the caller's responsibility to ensure the attribute
        // object matches the content string.
        if (null != attributes) {
            headerMap.putString(HeaderEnum.ATTRIBUTES, attributes.toString());
        }
        headerMap.putString(HeaderEnum.EVENTS, events.name);

        return this;
    }
}

