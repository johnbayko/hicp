package hicp_client;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Event;

public class GUIRootItem
    extends GUIContainerItem
{
    /**
        Non-GUI thread.
     */
    public GUIRootItem(final TextLibrary textLibrary) {
        super(textLibrary);
        _firstTextDirection = TextDirection.RIGHT;
        _secondTextDirection = TextDirection.DOWN;
    }

    protected GUIItem addInvoked(final Add addCmd) {
        return this;
    }

    /**
        Set text, must be called from GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        // Not used at the moment.
        return this;
    }

    // Root has no component.
    protected Component getComponent() {
        return null;
    }

    // Doesn't really make sense for the root item, but needs to be
    // implemented.
    protected int getGridBagAnchor() {
        return java.awt.GridBagConstraints.CENTER;
    }

    // Doesn't really make sense for the root item, but needs to be
    // implemented.
    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.NONE;
    }

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        super.modifyInvoked(modifyCmd);
        return this;
    }
}

