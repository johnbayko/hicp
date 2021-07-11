package hicp;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum TextDirection {
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down");

    public final String name;

    private static final Map<String, TextDirection> enumMap =
        Arrays.stream(TextDirection.values())
            .collect(
                Collectors.toMap(
                    e -> e.name,
                    e -> e
                )
            );

    TextDirection(final String forName) {
        name = forName;
    }

    public static TextDirection getEnum(String name) {
        return enumMap.get(name);
    }
}
