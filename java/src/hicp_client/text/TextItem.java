package hicp_client.text;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextItem {
    private static final Logger LOGGER =
        Logger.getLogger( TextItem.class.getName() );

    // TODO is this used?
    public final String id;

    protected String _text = "";

    /** List of text listeners. I expect only a few listeners will ever
        be added to any text item, but they may be added and removed
        often as windows open and close. This will only be scanned
        linearly, so use linked list to make removal efficient. */
    protected List<TextListener> _textListenerList = new LinkedList<>();

    protected TextEvent _textEvent = new TextEvent(this);

    public TextItem(String newID, String text) {
        id = newID;
        setText(text);
    }

    public void setText(String text) {
        _text = text;
        fireTextChanged();
    }

    public String getText() {
        return _text;
    }

    public void addTextListener(TextListener l) {
        synchronized (_textListenerList) {
            _textListenerList.add(l);
        }
    }

    public void removeTextListener(TextListener l) {
        synchronized (_textListenerList) {
            _textListenerList.remove(l);
        }
    }

    public boolean hasTextListeners() {
        return (0 != _textListenerList.size());
    }

    protected void fireTextChanged() {
        synchronized (_textListenerList) {
            final Iterator it = _textListenerList.iterator();

            while (it.hasNext()) {
                TextListener l = (TextListener)it.next();
                try {
                    l.textChanged(_textEvent);
                } catch (Exception ex) {
                    // Don't try to update again.
                    removeTextListener(l);
                }
            }
        }
    }
}
