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
        if (null == id) {
            // Nothing to remove.
            return this;
        }
        final TextItem textItem = _textItemMap.get(id);

        if (null == textItem) {
            // Nothing found to remove.
            return this;
        }

        if (textItem.hasTextListeners()) {
            // Is used by a GUI item, set this to "".
            if (false == "".equals(textItem.getText())) {
                textItem.setText("");
            }
            // TODO: when listeners are removed, this will remain - add flag to
            // clean up when no listners?
        } else {
            // Can be safely removed.
            _textItemMap.remove(id);
        }
        return this;
    }

    public TextLibrary addModify(hicp.message.command.AddModify addModifyCmd) {
        // Must have id and text fields.
        if ((null == addModifyCmd.id) || (null == addModifyCmd.text)) {
            LOGGER.log(Level.INFO, "Add text missing id or text");
            return this;
        }
        try {
            TextItem textItem = _textItemMap.get(addModifyCmd.id);

            if (null != textItem) {
                textItem.setText(addModifyCmd.text);
            } else {
                final int id = Integer.parseInt(addModifyCmd.id);
                textItem = new TextItem(id, addModifyCmd.id, addModifyCmd.text);
                _textItemMap.put(addModifyCmd.id, textItem);
            }
        } catch (NumberFormatException ex) {
            // Not an integer ID, ignore message.
            return this;
        }
        return this;
    }
}
