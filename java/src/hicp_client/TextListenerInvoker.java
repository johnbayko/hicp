package hicp_client;

import javax.swing.SwingUtilities;

/**
    Shift call to textChanged() to GUI event dispatch thread.
 */
public class TextListenerInvoker
    implements TextListener, Runnable
{
    private final TextListener _textListener;

    private TextEvent _textEvent = null;

    public TextListenerInvoker(TextListener l) {
        _textListener = l;
    }

    public void textChanged(TextEvent e) {
        _textEvent = e;
        SwingUtilities.invokeLater(this);
    }

    public void run() {
        if (null != _textEvent) {
            _textListener.textChanged(_textEvent);
        }
    }
}
