package hicp.message.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.AttributeListInfo;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class TextFieldInfo {
    private static final Logger LOGGER =
        Logger.getLogger( EventInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public String content = null;
    private AttributeListInfo attributeListInfo = null;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;

    public TextFieldInfo() {
    } 

    public TextFieldInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        content = headerMap.getString(HeaderEnum.CONTENT);
        {
            final boolean hasAttributes = headerMap.has(HeaderEnum.ATTRIBUTES);
            if (hasAttributes) {
                attributeListInfo = new AttributeListInfo(_headerMap);
            }
        }
    }

    public TextFieldInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.CONTENT, content);
        if (null != attributeListInfo) {
            attributeListInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public TextFieldInfo setAttributeListInfo(
        final AttributeListInfo newAttributeListInfo
    ) {
        attributeListInfo = newAttributeListInfo;
        return this;
    }

    public AttributeListInfo getAttributeListInfo() {
        if (null == attributeListInfo) {
            attributeListInfo = new AttributeListInfo(_headerMap);
        }
        return attributeListInfo;
    }
}
