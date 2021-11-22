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

    protected Item modifyInvoked(final Message modifyCmd) {
        super.modifyInvoked(modifyCmd);
        return this;
    }
}

