package hicp_client.gui;

import java.awt.Component;
import javax.swing.SwingUtilities;

import hicp.message.command.CommandInfo;

public abstract class Item {
    public final String idString;
    public final String component;

    /** What this is contained by. */
    protected ContainerItem _parent = null;

    /**
        Non-GUI thread.
     */
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

    public final Item add(final CommandInfo addCmd) {
        SwingUtilities.invokeLater(
            new RunAdd(addCmd)
        );
        return this;
    }

    class RunAdd
        implements Runnable
    {
        protected final CommandInfo _addCmd;

        public RunAdd(final CommandInfo addCmd) {
            _addCmd = addCmd;
        }

        public void run() {
            addInvoked(_addCmd);
        }
    }

    /**
        GUI thread.
     */
    protected abstract Item addInvoked(CommandInfo commandInfo);

    public final Item modify(final CommandInfo modifyCmd) {
        SwingUtilities.invokeLater(
            new RunModify(modifyCmd)
        );
        return this;
    }

    class RunModify
        implements Runnable
    {
        protected final CommandInfo _modifyCmd;

        public RunModify(final CommandInfo modifyCmd) {
            _modifyCmd = modifyCmd;
        }

        public void run() {
            modifyInvoked(_modifyCmd);
        }
    }

    /**
        GUI thread.
     */
    protected abstract Item modifyInvoked(CommandInfo commandInfo);

    /**
        GUI thread.
     */
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

