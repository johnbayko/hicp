package hicp_client.text;

import javax.swing.SwingUtilities;

/**
    Shift call to textChanged() to GUI event dispatch thread.
    TODO All in GUI event thread, remove.
 */
public class TextListenerInvoker
    implements TextListener//, Runnable
{
    private final TextListener _textListener;

    public TextListenerInvoker(TextListener l) {
        _textListener = l;
    }

    public void textChanged(TextEvent e) {
        if (null != e) {
            _textListener.textChanged(e);
        }
    }
}
