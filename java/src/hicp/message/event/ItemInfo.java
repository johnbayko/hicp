package hicp.message.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class ItemInfo {
    private static final Logger LOGGER =
        Logger.getLogger( EventInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public String id = null;

    private TextFieldInfo _textFieldInfo = null;
    private SelectionInfo _selectionInfo = null;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;

    public ItemInfo() {
    } 

    public ItemInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        id = headerMap.getString(HeaderEnum.ID);
    }

    public ItemInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.ID, id);
        if (null != _textFieldInfo) {
            _textFieldInfo.updateHeaderMap(headerMap);
        }
        if (null != _selectionInfo) {
            _selectionInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public TextFieldInfo getTextFieldInfo() {
        if (null == _textFieldInfo) {
            _textFieldInfo = new TextFieldInfo(_headerMap);
        }
        return _textFieldInfo;
    }

    public SelectionInfo getSelectionInfo() {
        if (null == _selectionInfo) {
            _selectionInfo = new SelectionInfo(_headerMap);
        }
        return _selectionInfo;
    }
}
