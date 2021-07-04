package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class ItemCommand
    extends Command
{
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

    // TODO Make these public, writable
    protected CategoryEnum _category = null;

    // Used as a Map key, not as an int, so keep as string.
    protected String _id;


    public ItemCommand(final String name) {
        super(name);
    }

    public ItemCommand(
        final String name,
        final HeaderMap headerMap
    ) {
        super(name);

        addHeaders(headerMap);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public ItemCommand addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);

        _category =
            CategoryEnum.getEnum(
                headerMap.getString(HeaderEnum.CATEGORY)
            );
        _id = headerMap.getString(HeaderEnum.ID);

        return this;
    }

    public HeaderMap getHeaders() {
        final HeaderMap headerMap = super.getHeaders();

        headerMap.putString(HeaderEnum.CATEGORY, _category.name);
        headerMap.putString(HeaderEnum.ID, _id);

        return headerMap;
    }

    public CategoryEnum getCategory() {
        return _category;
    }

    public String getId() {
        return _id;
    }
}
