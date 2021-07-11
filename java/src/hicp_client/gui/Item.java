package hicp_client.gui;

import java.awt.Component;
import javax.swing.SwingUtilities;

import hicp.message.Message;
import hicp.message.command.CommandInfo;
import hicp.message.command.ContainedGUIInfo;
import hicp.message.command.GUIInfo;
import hicp.message.command.ItemInfo;

public abstract class Item
{
    public final String idString;
    public final String component;
    public int horizontalPosition = 0;
    public int verticalPosition = 0;
    public int horizontalSize = 0;
    public int verticalSize = 0;

    /** What this is contained by. */
    protected ContainerItem _parent = null;

    /**
        Non-GUI thread.
     */
    public Item(Message m) {
        final CommandInfo commandInfo = m.getCommandInfo();
        final ItemInfo itemInfo = commandInfo.getItemInfo();
        final GUIInfo guiInfo = itemInfo.getGUIInfo();
        final ContainedGUIInfo containedGUIInfo = guiInfo.getContainedGUIInfo();

        idString = itemInfo.id;
        component = guiInfo.component.name;
        horizontalPosition = containedGUIInfo.position.horizontal;
        verticalPosition = containedGUIInfo.position.vertical;
        horizontalSize = containedGUIInfo.size.horizontal;
        verticalSize = containedGUIInfo.size.vertical;
    }

    public Item() {
        idString = null;
        component = null;
    }

    public final Item add(Message addCmd) {
        SwingUtilities.invokeLater(
            new RunAdd(addCmd)
        );
        return this;
    }

    class RunAdd
        implements Runnable
    {
        protected final Message _addCmd;

        public RunAdd(Message addCmd) {
            _addCmd = addCmd;
        }

        public void run() {
            addInvoked(_addCmd);
        }
    }

    /**
        GUI thread.
     */
    protected abstract Item addInvoked(Message addCmd);

    public final Item modify(Message modifyCmd) {
        SwingUtilities.invokeLater(
            new RunModify(modifyCmd)
        );
        return this;
    }

    class RunModify
        implements Runnable
    {
        protected final Message _modifyCmd;

        public RunModify(Message modifyCmd) {
            _modifyCmd = modifyCmd;
        }

        public void run() {
            modifyInvoked(_modifyCmd);
        }
    }

    /**
        GUI thread.
     */
    protected abstract Item modifyInvoked(Message modifyCmd);

    /**
        GUI thread.
     */
    public Item setParent(ContainerItem parent) {
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
    }
}

