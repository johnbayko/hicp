package hicp_client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import hicp.message.event.Click;
import hicp.message.event.Event;
import hicp.message.event.EventEnum;

public class GUIButtonItem
    extends GUIItem
{
    protected final MessageExchange _messageExchange;

    protected JButton _component;

    public GUIButtonItem(
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
            _component = new JButton();

            _component.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        // Send a click event with this object's ID.
                        final Click clickEvent =
                            (Click)EventEnum.CLICK.newEvent();
            
                        clickEvent.id = idString;
        
                        _messageExchange.send(clickEvent);
                    }
                }
            );

            // Button string.
            if (null != _textItem) {
                setTextItemInvoked(_textItem);
            }
            // Button enable/disable.
            {
                // Default is enable.
                final String eventsValue =
                    (null != _addCmd.events) ? _addCmd.events : Add.ENABLED;

                setEventsInvoked(eventsValue);
            }
        }
    }

    protected Component getComponent() {
        return _component;
    }

    protected int getGridBagAnchor() {
        return java.awt.GridBagConstraints.CENTER;
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

    protected GUIItem setEventsInvoked(final String eventsValue) {
        final boolean enabled = eventsValue.equals(Add.ENABLED);

        if (_component.isEnabled() != enabled) {
            _component.setEnabled(enabled);
        }
        return this;
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
log("Modifying label, component is "
    + _component.getClass().getName()
);  // debug
            // See what's changed.

            // New text item?
            if (null != _textItem) {
                setTextItemInvoked(_textItem);
            }
            if (null != _modifyCmd.events) {
                setEventsInvoked(_modifyCmd.events);
            }
            // Changed parent ID is handled by Controller.
        }
    }
}

