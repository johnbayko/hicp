package hicp_client.text;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import hicp.message.Message;

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

    public TextLibrary update(final Message m) {
        // Must have id and text fields.
        final var commandInfo = m.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var textInfo = itemInfo.getTextInfo();

        final String id = itemInfo.id;
        final String text = textInfo.text;

        if ((null == id) || (null == text)) {
            LOGGER.log(Level.INFO, "Add text missing id or text");
            return this;
        }

        TextItem textItem = _textItemMap.get(id);
        if (null != textItem) {
            textItem.setText(text);
        } else {
            textItem = new TextItem(id, text);
            _textItemMap.put(id, textItem);
        }
        return this;
    }
}
