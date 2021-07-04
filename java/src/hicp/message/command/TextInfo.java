package hicp.message.command;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class TextInfo {
    public String text = null;

    public TextInfo() {
    }

    public TextInfo(final HeaderMap headerMap) {
        text = headerMap.getString(HeaderEnum.TEXT);
    }

    public TextInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.TEXT, text);

        return this;
    }
}
