package hicp_client.gui.selection;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum EventsEnum {
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

    EventsEnum(final String newName) {
        name = newName;
    }

    public static EventsEnum getEnum(final String name) {
        return enumMap.getOrDefault(name, ENABLED);
    }
}

