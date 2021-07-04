package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class ItemInfo {
    public static enum CategoryEnum {
        GUI("gui"),
        TEXT("text");

        public final String name;

        private static final Map<String, CategoryEnum> enumMap =
            Arrays.stream(CategoryEnum.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        CategoryEnum(final String forName) {
            name = forName;
        }

        public static CategoryEnum getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public CategoryEnum category;
    public String id;


    public ItemInfo(final HeaderMap headerMap) {
        category =
            CategoryEnum.getEnum(
                headerMap.getString(HeaderEnum.CATEGORY)
            );
        id = headerMap.getString(HeaderEnum.ID);
    }

    public ItemInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.CATEGORY, category.name);
        headerMap.putString(HeaderEnum.ID, id);

        return this;
    }
}
