package hicp_client;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.command.Add;
import hicp.message.command.Modify;

public abstract class GUIItem
    implements TextListener
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIItem.class.getName() );

    public final String idString;
    public final String component;
    public int horizontalPosition = 0;
    public int verticalPosition = 0;
    public int horizontalSize = 0;
    public int verticalSize = 0;

    /** Source of this thing's text. */
    protected TextItem _textItem = null;

    /** What this is contained by. */
    protected GUIContainerItem _parent = null;

    /**
        Non-GUI thread.
     */
    public GUIItem(Add addCmd) {
        // Placeholder items used for construction won't have commands.
        if (null != addCmd) {
            idString = addCmd.id;
            component = addCmd.component;
            horizontalPosition = addCmd.horizontalPosition;
            verticalPosition = addCmd.verticalPosition;
            horizontalSize = addCmd.horizontalSize;
            verticalSize = addCmd.verticalSize;
        } else {
            idString = null;
            component = null;
        }
    }

    /**
        GUI thread.
     */
    protected GUIItem setTextItemInvoked(TextItem textItem) {
        if (null != _textItem) {
            _textItem.removeTextListener(this);
        }

        _textItem = textItem;
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

    /**
        Set text, called from non-GUI thread.
     */
    protected abstract GUIItem setText(String text);

    public GUIItem setParent(GUIContainerItem parent) {
        _parent = parent;
        _parent.add(this);

        return this;
    }

    protected abstract Component getComponent();

    protected abstract int getGridBagAnchor();

    protected abstract int getGridBagFill();

    /*
        The dispose method is called only when things are being shut
        down. Items should not remove themselves from parents in this
        case - the parents are being disposed of as well, and all will
        be cleaned up.
        TODO: Not necessarily? Check.
     */
    public void dispose() {
        if (null != _textItem) {
            _textItem.removeTextListener(this);
        }
    }

    public abstract GUIItem modify(Modify modifyCmd, TextItem textItem);

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

        setText(_textItem.getText());
    }
}

