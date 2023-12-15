package hicp.message.command;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.AttributeListInfo;
import hicp.message.HeaderEnum;
import hicp.message.Message;

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

    public ContentInfo contentInfo = null;
    public int width = 7;  // Arbitrary default
    public boolean hasWidth = false;
    private AttributeListInfo attributeListInfo = null;
    public EventsEnum events = null;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;

    public GUITextFieldInfo() {
    }

    public GUITextFieldInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;
        {
            final boolean hasContent = headerMap.has(HeaderEnum.CONTENT);
            if (hasContent) {
                try {
                    contentInfo = new ContentInfo(headerMap);
                } catch (ParseException pe) {
                    // Leave it as null, no valid content action.
                }
            }
        }
        {
            final boolean hasWidth = headerMap.has(HeaderEnum.WIDTH);
            if (hasWidth) {
                width = headerMap.getInt(HeaderEnum.WIDTH, width);
            }
        }
        {
            final boolean hasAttributes = headerMap.has(HeaderEnum.ATTRIBUTES);
            if (hasAttributes) {
                attributeListInfo = new AttributeListInfo(_headerMap);
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
        if (null != contentInfo) {
            contentInfo.updateHeaderMap(headerMap);
        }
        headerMap.putInt(HeaderEnum.WIDTH, width);

        if (null != attributeListInfo) {
            attributeListInfo.updateHeaderMap(headerMap);
        }
        headerMap.putString(HeaderEnum.EVENTS, events.name);

        return this;
    }

    public AttributeListInfo getAttributeListInfo() {
        if (null == attributeListInfo) {
            attributeListInfo = new AttributeListInfo(_headerMap);
        }
        return attributeListInfo;
    }
}

