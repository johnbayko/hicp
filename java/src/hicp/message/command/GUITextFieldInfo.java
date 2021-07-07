package hicp.message.command;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.TextAttributes;

public class GUITextFieldInfo {
    public String content = null;
    public TextAttributes attributes = null;

    public GUITextFieldInfo() {
    }

    public GUITextFieldInfo(final HeaderMap headerMap) {
        content = headerMap.getString(HeaderEnum.CONTENT);
        {
            final String attributesStr =
                headerMap.getString(HeaderEnum.ATTRIBUTES);
            if (null != attributesStr) {
                attributes = new TextAttributes(attributesStr, content.length());
            }
        }
    }

    public GUITextFieldInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.CONTENT, content);

        // Warning: It's the caller's responsibility to ensure the attribute
        // object matches the content string.
        if (null != attributes) {
            headerMap.putString(HeaderEnum.ATTRIBUTES, attributes.toString());
        }

        return this;
    }
}

