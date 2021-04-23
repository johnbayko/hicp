package hicp_client;

import javax.swing.SwingUtilities;

import hicp.message.command.Add;

public abstract class GUISingleTextItem
    extends GUIItem
    implements TextListener
{
    /** Source of this thing's text. */
    protected TextLibrary _textLibrary = null;
    protected TextItem _textItem = null;

    public GUISingleTextItem(
        final Add addCmd,
        final TextLibrary textLibrary
    ) {
        super(addCmd);
        _textLibrary = textLibrary;
    }

    /**
        GUI thread.
     */
    protected GUISingleTextItem setTextIdInvoked(final String textId) {
        if (null != _textItem) {
            _textItem.removeTextListener(this);
        }
        _textItem = _textLibrary.get(textId);

        if (null != _textItem) {
            setTextInvoked(_textItem.getText());

            _textItem.addTextListener(this);
        }
        return this;
    }

    /**
        Set text, must be called from GUI thread.
     */
    protected abstract GUIItem setTextInvoked(String text);

    public void dispose() {
        super.dispose();
        if (null != _textItem) {
            _textItem.removeTextListener(this);
        }
    }

// TextListener
    /**
        Called from input thread, not GUI thread.
     */
    public void textChanged(TextEvent textEvent) {
        TextItem ti = (TextItem)textEvent.getSource();
        if (ti != _textItem) {
            // No idea how this could happen. Ignore it.
            return;
        }

        SwingUtilities.invokeLater(
            new RunSetText(_textItem.getText())
        );
    }

    class RunSetText
        implements Runnable
    {
        protected final String _text;

        RunSetText(String text) {
            _text = text;
        }

        public void run() {
            setTextInvoked(_text);
        }
    }
}

