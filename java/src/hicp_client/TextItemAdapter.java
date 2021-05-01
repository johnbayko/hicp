package hicp_client;

import javax.swing.SwingUtilities;

public class TextItemAdapter
    implements TextListener
{
    private final TextLibrary _textLibrary;

    private TextItemAdapterListener _listener = null;
    private TextItem _textItem = null;

    private Runnable _textInvoker = new Runnable() {
        public void run() {
            _listener.setTextInvoked(_textItem.getText());
        }
    };


    public TextItemAdapter(TextLibrary newTextLibrary) {
        _textLibrary = newTextLibrary;
    }

    public TextItemAdapter setAdapter(TextItemAdapterListener l) {
        _listener = l;
        _textItem = null;  // Should already be null.

        return this;
    }

    public TextItemAdapter setTextIdInvoked(String textId) {
        if (null != _textItem) {
            _textItem.removeTextListener(this);
        }
        _textItem = _textLibrary.get(textId);

        if (null != _textItem) {
            _textItem.addTextListener(this);
            _listener.setTextInvoked(_textItem.getText());
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
        SwingUtilities.invokeLater(_textInvoker);
    }
}
