package hicp_client.gui;

import java.awt.Component;

import hicp.TextDirection;
import hicp.message.command.CommandInfo;

public class RootItem
    extends ContainerItem
{
    public RootItem() {
        super();
        _firstTextDirection = TextDirection.RIGHT;
        _secondTextDirection = TextDirection.DOWN;
    }

    protected Item add(final CommandInfo commandInfo) {
        return this;
    }

    protected Item modify(final CommandInfo commandInfo) {
        super.modify(commandInfo);
        return this;
    }
}

