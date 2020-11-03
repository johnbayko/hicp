package hicp_client;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Event;

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

    public static GUIItem newGUIItem(
        Add addCmd,
        TextItem textItem,
        MessageExchange messageExchange
    ) {
        try {
            // Make sure it's a real integer - not used.
            final int id = Integer.parseInt(addCmd.id);

            if (Add.BUTTON.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUIButtonItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.LABEL.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUILabelItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.PANEL.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUIPanelItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.TEXTFIELD.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUITextFieldItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.WINDOW.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUIWindowItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else {
                // Unrecognized category.
                LOGGER.log(Level.FINE, "Add to unrecognized category: " + addCmd.category);
                return null;
            }
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINE, "ID field not an integer.");

            // Not an integer ID, ignore message.
            return null;
        }
    }

    /**
        Non-GUI thread.
     */
    public GUIItem(Add addCmd/*, Logger logger*/) {
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

// Utility
    protected void log(String msg) {
        LOGGER.log(Level.FINE, msg);
    }
}

