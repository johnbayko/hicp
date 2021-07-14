package hicp.message.event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class EventInfo {
    public static enum Event
    {
        AUTHENTICATE("authenticate"),
        CHANGED("changed"),
        CLOSE("close"),
        CLICK("click"),
        CONNECT("connect");

        public final String name;

        private static final Map<String, Event> messageNameMap =
            Arrays.stream(Event.values())
                .collect(
                    Collectors.toMap(
                        eventEnum -> eventEnum.name,
                        eventEnum -> eventEnum
                    )
                );

        Event(final String newMessageName) {
            name = newMessageName;
        }

        public static Event getEnum(String name) {
            return messageNameMap.get(name);
        }
    }

    public Event event;

    public EventInfo() {
    } 

    public EventInfo(final Event newEvent) {
        event = newEvent;
    }

    public EventInfo(final HeaderMap headerMap) {
        event =
            Event.getEnum(
                headerMap.getString(HeaderEnum.EVENT)
            );
    }

    public EventInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        if (null != event) {
            headerMap.putString(HeaderEnum.EVENT, event.name);
        }
        return this;
    }
}

