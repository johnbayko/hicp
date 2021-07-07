package hicp.message.command;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class GUILabelInfo {
    public String text = null;

    public GUILabelInfo() {
    }

    public GUILabelInfo(final HeaderMap headerMap) {
        text = headerMap.getString(HeaderEnum.TEXT);
    }

    public GUILabelInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.TEXT, text);

        return this;
    }
}

