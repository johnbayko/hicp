package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class GUISelectionInfo {
    public static enum EventsEnum {
        ENABLED("enabled"),
        DISABLED("disabled"),
        UNSELECT("unselect");

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

    public EventsEnum events = null;

    public GUISelectionInfo() {
    }

    public GUISelectionInfo(final HeaderMap headerMap) {
        events =
            EventsEnum.getEnum(
                headerMap.getString(HeaderEnum.EVENTS)
            );
    }

    public GUISelectionInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.EVENTS, events.name);

        return this;
    }
}


