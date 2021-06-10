package hicp_client.gui.selection;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ModeEnum {
    SINGLE("single"),
    MULTIPLE("multiple");

    public final String name;

    private static final Map<String, ModeEnum> enumMap =
        Arrays.stream(ModeEnum.values())
            .collect(
                Collectors.toMap(
                    e -> e.name,
                    e -> e
                )
            );

    ModeEnum(final String newName) {
        name = newName;
    }

    public static ModeEnum getEnum(final String name) {
        return enumMap.getOrDefault(name, MULTIPLE);
    }
}
