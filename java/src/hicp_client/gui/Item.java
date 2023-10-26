package hicp_client.gui;

import java.awt.Component;
import javax.swing.SwingUtilities;

import hicp.message.command.CommandInfo;

public abstract class Item {
    public final String idString;
    public final String component;

    /** What this is contained by. */
    protected ContainerItem _parent = null;

    public Item(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();

        idString = itemInfo.id;

        component = guiInfo.component.name;
    }

    public Item() {
        idString = null;
        component = null;
    }

    protected abstract Item add(CommandInfo commandInfo);

    protected abstract Item modify(CommandInfo commandInfo);

    public Item setParent(ContainerItem parent) {
        _parent = parent;
        if (null == _parent) {
            // Makes no sense (parent can only be set once, if set to null then
            // it will never be used), but could happen.
            return this;
        }
        _parent.add(this);

        return this;
    }

    /*
        The dispose method is called only when things are being shut
        down. Items should not remove themselves from parents in this
        case - the parents are being disposed of as well, and all will
        be cleaned up.
        TODO: Not necessarily? Check.
     */
    public void dispose() {
    }
}

