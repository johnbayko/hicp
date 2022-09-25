package hicp.message.command;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class GUIPanelInfo {
    public String text = null;

    public GUIPanelInfo() {
    }

    public GUIPanelInfo(final HeaderMap headerMap) {
        text = headerMap.getString(HeaderEnum.TEXT);
    }

    public GUIPanelInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.TEXT, text);

        return this;
    }
}

