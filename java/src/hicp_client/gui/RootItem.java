package hicp_client.gui;

import java.awt.Component;

import hicp.TextDirection;
import hicp.message.Message;

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

    protected Item addInvoked(final Message m) {
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

    protected Item modifyInvoked(final Message modifyCmd) {
        super.modifyInvoked(modifyCmd);
        return this;
    }
}

