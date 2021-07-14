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


    public static final TextFieldInfo DEFAULT_TEXT_FIELD_INFO =
        new TextFieldInfo();
    private TextFieldInfo _textFieldInfo = DEFAULT_TEXT_FIELD_INFO;

    public static final SelectionInfo DEFAULT_SELECTION_INFO =
        new SelectionInfo();
    private SelectionInfo _selectionInfo = DEFAULT_SELECTION_INFO;


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
        if (DEFAULT_TEXT_FIELD_INFO != _textFieldInfo) {
            _textFieldInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_SELECTION_INFO != _selectionInfo) {
            _selectionInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public TextFieldInfo getTextFieldInfo() {
        if (DEFAULT_TEXT_FIELD_INFO == _textFieldInfo) {
            _textFieldInfo = new TextFieldInfo(_headerMap);
        }
        return _textFieldInfo;
    }

    public ItemInfo setTextFieldInfo(final TextFieldInfo i) {
        _textFieldInfo = i;
        return this;
    }

    public SelectionInfo getSelectionInfo() {
        if (DEFAULT_SELECTION_INFO == _selectionInfo) {
            _selectionInfo = new SelectionInfo(_headerMap);
        }
        return _selectionInfo;
    }

    public ItemInfo setSelectionInfo(final SelectionInfo i) {
        _selectionInfo = i;
        return this;
    }
}
