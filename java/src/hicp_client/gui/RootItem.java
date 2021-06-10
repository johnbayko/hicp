package hicp_client.gui;

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

public class RootItem
    extends ContainerItem
{
    /**
        Non-GUI thread.
     */
    public RootItem() {
        super();
        _firstTextDirection = TextDirection.RIGHT;
        _secondTextDirection = TextDirection.DOWN;
    }

    protected Item addInvoked(final Add addCmd) {
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

    protected Item modifyInvoked(final Modify modifyCmd) {
        super.modifyInvoked(modifyCmd);
        return this;
    }
}

