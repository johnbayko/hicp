package hicp.message;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum HeaderEnum {
    COMMAND("command"),  // Always first for commands.
    EVENT("event"),  // Always first for events.

    APPLICATION("application"),
    ATTRIBUTES("attributes"),
    CATEGORY("category"),
    COMPONENT("component"),
    CONTENT("content"),
    EVENTS("events"),
    HEIGHT("height"),
    ID("id"),
    ITEMS("items"),
    METHOD("method"),
    MODE("mode"),
    PARENT("parent"),
    PASSWORD("password"),
    POSITION("position"),
    PRESENTATION("presentation"),
    SELECTED("selected"),
    SIZE("size"),
    TEXT("text"),
    TEXT_DIRECTION("text-direction"),
    USER("user"),
    VISIBLE("visible"),
    WIDTH("width");

    public final String name;

    private static final Map<String, HeaderEnum> enumMap =
        Arrays.stream(HeaderEnum.values())
            .collect(
                Collectors.toMap(
                    e -> e.name,
                    e -> e
                )
            );

    HeaderEnum(final String newName) {
        name = newName;
    }

    public static HeaderEnum getEnum(final String name) {
        return enumMap.get(name);  // null if no match.
    }
}

