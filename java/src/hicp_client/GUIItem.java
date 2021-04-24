package hicp_client;

import java.awt.Component;
import javax.swing.SwingUtilities;

import hicp.message.command.Add;
import hicp.message.command.Modify;

public abstract class GUIItem
{
    public final String idString;
    public final String component;
    public int horizontalPosition = 0;
    public int verticalPosition = 0;
    public int horizontalSize = 0;
    public int verticalSize = 0;

    /** What this is contained by. */
    protected GUIContainerItem _parent = null;

    /**
        Non-GUI thread.
     */
    public GUIItem(Add addCmd) {
        // Placeholder items used for construction won't have commands.
        if (null != addCmd) {
            idString = addCmd.id;
            component = addCmd.component;
            horizontalPosition = addCmd.horizontalPosition;
            verticalPosition = addCmd.verticalPosition;
            horizontalSize = addCmd.horizontalSize;
            verticalSize = addCmd.verticalSize;
        } else {
            idString = null;
            component = null;
        }
    }

    public final GUIItem modify(Modify modifyCmd) {
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
    protected abstract GUIItem modifyInvoked(Modify modifyCmd);

    /**
        GUI thread.
     */
    public GUIItem setParent(GUIContainerItem parent) {
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

