package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

// Data class.
public class ItemInfo {
    public static enum CategoryEnum
    {
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

    private CategoryEnum _category = null;

    // Used as a Map key, not as an int, so keep as string.
    private String _id;

    public ItemInfo() {
    }

    public ItemInfo(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        addHeaders(headerMap);
    }

    public ItemInfo addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        for (final HeaderEnum h : headerMap.keySet()) {
            final HICPHeader v = headerMap.get(h);
            switch (h) {
              case CATEGORY:
               _category = CategoryEnum.getEnum(
                       v.value.getString()
                   );
                break;
              case ID:
                _id = v.value.getString();
                break;
            }
        }

        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        Message.addHeaderString(headerMap, HeaderEnum.CATEGORY, _category.name);
        Message.addHeaderString(headerMap, HeaderEnum.ID, _id);

        return headerMap;
    }

    public CategoryEnum getCategory() {
        return _category;
    }

    public String getId() {
        return _id;
    }
}
