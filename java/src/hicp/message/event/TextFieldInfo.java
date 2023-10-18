package hicp.message.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.TextAttributes;

public class TextFieldInfo {
    private static final Logger LOGGER =
        Logger.getLogger( EventInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public String content = null;
    public TextAttributes attributes = null;

    public TextFieldInfo() {
    } 

    public TextFieldInfo(final HeaderMap headerMap) {
        content = headerMap.getString(HeaderEnum.CONTENT);
        {
            final String attributesStr =
                headerMap.getString(HeaderEnum.ATTRIBUTES);
            if (null != attributesStr) {
                attributes = new TextAttributes(attributesStr);
            }
        }
    }

    public TextFieldInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.CONTENT, content);
        if (null != attributes) {
            headerMap.putString(HeaderEnum.ATTRIBUTES, attributes.toString());
        }
        return this;
    }
}


