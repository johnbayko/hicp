package hicp_client;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

public class TextLibrary {
    private static final Logger LOGGER =
        Logger.getLogger( TextLibrary.class.getName() );

    private Map<String, TextItem> _textItemMap = new HashMap<>();

    public TextLibrary put(final String id, final TextItem textItem) {
        if ((null != id) && (null != textItem)) {
            _textItemMap.put(id, textItem);
        }
        return this;
    }
    public TextItem get(final String id) {
        return (null != id)
            ? _textItemMap.get(id)
            : null;
    }
    public TextLibrary remove(final String id) {
        if (null != id) {
            _textItemMap.remove(id);
        }
        return this;
    }
    public TextLibrary addModify(hicp.message.command.AddModify addModifyCmd) {
        // TODO fill in from Controller.
        return this;
    }
}
