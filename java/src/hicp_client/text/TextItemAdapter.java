package hicp_client.text;

import javax.swing.SwingUtilities;

public class TextItemAdapter
    implements TextListener
{
    private final TextLibrary _textLibrary;

    private TextItemAdapterListener _listener = null;
    private TextItem _textItem = null;

    public TextItemAdapter(TextLibrary newTextLibrary) {
        _textLibrary = newTextLibrary;
    }

    public TextItemAdapter setAdapter(TextItemAdapterListener l) {
        _listener = l;
        _textItem = null;  // Should already be null.

        return this;
    }

    public TextItemAdapter setTextId(String textId) {
        if (null != _textItem) {
            _textItem.removeTextListener(this);
        }
        _textItem = _textLibrary.get(textId);

        if (null != _textItem) {
            _textItem.addTextListener(this);
            _listener.setText(_textItem.getText());
        }
        return this;
    }

    public TextItemAdapter removeAdapter() {
        if (null != _textItem) {
            _textItem.removeTextListener(this);
            _textItem = null;
        }
        _listener = null;

        return this;
    }

    public void textChanged(TextEvent e) {
        TextItem ti = (TextItem)e.getSource();
        if (ti != _textItem) {
            // No idea how this could happen. Ignore it.
            return;
        }
        _listener.setText(_textItem.getText());
    }
}
