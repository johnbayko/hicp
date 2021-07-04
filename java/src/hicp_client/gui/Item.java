package hicp_client.gui;

import java.awt.Component;
import javax.swing.SwingUtilities;

import hicp.message.command.Add;
import hicp.message.command.CommandInfo;
import hicp.message.command.GUIInfo;
import hicp.message.command.ItemInfo;
import hicp.message.command.Modify;

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
    public Item(Add addCmd) {
        final CommandInfo commandInfo = addCmd.getCommandInfo();
        final ItemInfo itemInfo = commandInfo.getItemInfo();
        final GUIInfo guiInfo = itemInfo.getGUIInfo();

        idString = itemInfo.id;
        component = guiInfo.component.name;
        horizontalPosition = addCmd.horizontalPosition;
        verticalPosition = addCmd.verticalPosition;
        horizontalSize = addCmd.horizontalSize;
        verticalSize = addCmd.verticalSize;
    }

    public Item() {
        idString = null;
        component = null;
    }

    public final Item add(Add addCmd) {
        SwingUtilities.invokeLater(
            new RunAdd(addCmd)
        );
        return this;
    }

    class RunAdd
        implements Runnable
    {
        protected final Add _addCmd;

        public RunAdd(Add addCmd) {
            _addCmd = addCmd;
        }

        public void run() {
            addInvoked(_addCmd);
        }
    }

    /**
        GUI thread.
     */
    protected abstract Item addInvoked(Add addCmd);

    public final Item modify(Modify modifyCmd) {
        SwingUtilities.invokeLater(
            new RunModify(modifyCmd)
        );
        return this;
    }

    class RunModify
        implements Runnable
    {
        protected final Modify _modifyCmd;

        public RunModify(Modify modifyCmd) {
            _modifyCmd = modifyCmd;
        }

        public void run() {
            modifyInvoked(_modifyCmd);
        }
    }

    /**
        GUI thread.
     */
    protected abstract Item modifyInvoked(Modify modifyCmd);

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

