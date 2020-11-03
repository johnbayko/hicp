package hicp_client;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Event;

public class GUILabelItem
    extends GUIItem
{
    protected final MessageExchange _messageExchange;

    protected JLabel _component;

    public GUILabelItem(
        Add addCmd,
        TextItem textItem,
        MessageExchange messageExchange
    ) {
        super(addCmd);

        _messageExchange = messageExchange;

        SwingUtilities.invokeLater(
            new RunNew(addCmd, textItem)
        );
    }

    class RunNew
        implements Runnable
    {
        protected final Add _addCmd;
        protected final TextItem _textItem;

        public RunNew(Add addCmd, TextItem textItem)
        {
            _addCmd = addCmd;
            _textItem = textItem;
        }

        public void run()
        {
            _component = new JLabel();

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
        _component.setText(text);

        return this;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    public GUIItem modify(Modify modifyCmd, TextItem textItem) {
        SwingUtilities.invokeLater(
            new RunModify(modifyCmd, textItem)
        );

        return this;
    }

    class RunModify
        implements Runnable
    {
        protected final Modify _modifyCmd;
        protected final TextItem _textItem;

        public RunModify(Modify modifyCmd, TextItem textItem) {
            _modifyCmd = modifyCmd;
            _textItem = textItem;
        }

        public void run() {
            // See what's changed.

            // New text item?
            if (null != _textItem) {
                setTextItemInvoked(_textItem);
            }

            // Changed parent ID is handled by Controller.
        }
    }
}

