package hicp_client.gui;

import java.awt.Component;
import javax.swing.SwingUtilities;

import hicp.message.Message;

public abstract class Item {
    public final String idString;
    public final String component;

    /** What this is contained by. */
    protected ContainerItem _parent = null;

    /**
        Non-GUI thread.
     */
    public Item(Message m) {
        final var commandInfo = m.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();

        idString = itemInfo.id;

        component = guiInfo.component.name;
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

