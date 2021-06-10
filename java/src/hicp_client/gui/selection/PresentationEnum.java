package hicp_client.gui.selection;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PresentationEnum {
    SCROLL("scroll"),
    TOGGLE("toggle"),
    DROPDOWN("dropdown");

    public final String name;

    private static final Map<String, PresentationEnum> enumMap =
        Arrays.stream(PresentationEnum.values())
            .collect(
                Collectors.toMap(
                    e -> e.name,
                    e -> e
                )
            );

    PresentationEnum(final String newName) {
        name = newName;
    }

    public static PresentationEnum getEnum(final String name) {
        return enumMap.getOrDefault(name, SCROLL);
    }
}

