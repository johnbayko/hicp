package hicp_client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Click;
import hicp.message.event.EventEnum;

public class GUIButtonItem
    extends GUISingleTextItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIButtonItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected JButton _component;

    public GUIButtonItem(
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
            if (null != _addCmd.text) {
                setTextIdInvoked(_addCmd.text);
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

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        // See what's changed.

        // New text item?
        if (null != modifyCmd.text) {
            setTextIdInvoked(modifyCmd.text);
        }
        if (null != modifyCmd.events) {
            setEventsInvoked(modifyCmd.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

