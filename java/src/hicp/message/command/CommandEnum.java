package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

// TODO move inside command info class.
public enum CommandEnum
{
    AUTHENTICATE("authenticate"),
    ADD("add"),
    MODIFY("modify"),
    REMOVE("remove"),
    DISCONNECT("disconnect");

    public final String name;

    private static final Map<String, CommandEnum> enumMap =
        Arrays.stream(CommandEnum.values())
            .collect(
                Collectors.toMap(
                    e -> e.name,
                    e -> e
                )
            );


    CommandEnum(final String forName) {
        name = forName;
    }

    public static CommandEnum getEnum(String name) {
        return enumMap.get(name);
    }
}
