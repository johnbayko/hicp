package hicp_client.gui;

import java.awt.Component;

import hicp.TextDirection;
import hicp.message.command.CommandInfo;

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

    protected Item addInvoked(final CommandInfo commandInfo) {
        return this;
    }

    protected Item modifyInvoked(final CommandInfo commandInfo) {
        super.modifyInvoked(commandInfo);
        return this;
    }
}

