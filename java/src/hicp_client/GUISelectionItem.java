package hicp_client;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;

public class GUISelectionItem
    extends GUIItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected JLabel _component;  // debug

    public GUISelectionItem(
        Add addCmd,
        TextItem textItem,  // Doesn't apply to text fields.
        MessageExchange messageExchange
    ) {
        super(addCmd);

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
            _component = new JLabel("Selection list");  // debug

            // Label string.
            if (null != _textItem) {
                setTextItemInvoked(_textItem);
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
        Called in non-GUI thread.
     */
    protected GUIItem setText(String text) {
        SwingUtilities.invokeLater(
            new RunSetText(text)
        );

        return this;
    }

    class RunSetText
        implements Runnable
    {
        protected final String _text;

        RunSetText(String text) {
            _text = text;
        }

        public void run() {
            setTextInvoked(_text);
        }
    }

    /**
        Called in GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        LOGGER.log(Level.FINE, "setTextInvoked(\"" + text + "\")");  // debug

        return this;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected GUIItem setEventsInvoked(final String eventsValue) {
        // TODO all of this.

        return this;
    }

    public GUIItem modify(Modify modifyCmd, TextItem textItem) {
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
            // See what's changed.
            if (null != _modifyCmd.events) {
                setEventsInvoked(_modifyCmd.events);
            }
            // Changed parent ID is handled by Controller.
        }
    }
}

