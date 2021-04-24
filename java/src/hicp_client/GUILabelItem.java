package hicp_client;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;

public class GUILabelItem
    extends GUISingleTextItem
{
    protected final MessageExchange _messageExchange;

    protected JLabel _component;

    public GUILabelItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        super(addCmd, textLibrary);

        _messageExchange = messageExchange;

        SwingUtilities.invokeLater(
            new RunNew(addCmd)
        );
    }

    class RunNew
        implements Runnable
    {
        protected final Add _addCmd;

        public RunNew(Add addCmd)
        {
            _addCmd = addCmd;
        }

        public void run()
        {
            _component = new JLabel();

            // Label string.
            if (null != _addCmd.text) {
                setTextIdInvoked(_addCmd.text);
            }
        }
    }

    protected Component getComponent() {
        return _component;
    }

    protected int getGridBagAnchor() {
        if (hicp.TextDirection.RIGHT == _parent.getHorizontalTextDirection()) {
            return java.awt.GridBagConstraints.WEST;
        } else {
            return java.awt.GridBagConstraints.EAST;
        }
    }

    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.NONE;
    }

    /**
        Called in GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        _component.setText(text);

        return this;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        // See what's changed.

        // New text item?
        if (null != modifyCmd.text) {
            setTextIdInvoked(modifyCmd.text);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

