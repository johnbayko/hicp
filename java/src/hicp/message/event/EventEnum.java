package hicp.message.event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum EventEnum
{
    AUTHENTICATE("authenticate"),
    CHANGED("changed"),
    CLOSE("close"),
    CLICK("click"),
    CONNECT("connect");

    public final String name;

    private static final Map<String, EventEnum> messageNameMap =
        Arrays.stream(EventEnum.values())
            .collect(
                Collectors.toMap(
                    eventEnum -> eventEnum.name,
                    eventEnum -> eventEnum
                )
            );

    EventEnum(final String newMessageName) {
        name = newMessageName;
    }

    public static EventEnum getEnum(String name) {
        return messageNameMap.get(name);
    }
}

