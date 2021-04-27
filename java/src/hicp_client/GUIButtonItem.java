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
    extends GUIItem
    implements TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIButtonItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected TextItemAdapter _textItemAdapter;

    protected JButton _component;

    public GUIButtonItem(
        final Add addCmd,
        final MessageExchange messageExchange
    ) {
        super(addCmd);

        _messageExchange = messageExchange;
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    protected GUIItem addInvoked(Add addCmd) {
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
        if (null != addCmd.text) {
            _textItemAdapter.setTextId(addCmd.text);
        }
        // Button enable/disable.
        {
            // Default is enable.
            final String eventsValue =
                (null != addCmd.events) ? addCmd.events : Add.ENABLED;

            setEventsInvoked(eventsValue);
        }
        return this;
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
    public void setTextInvoked(String text) {
        _component.setText(text);
    }

    public void removeAdapter() {
        _textItemAdapter.removeAdapter();
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
            _textItemAdapter.setTextId(modifyCmd.text);
        }
        if (null != modifyCmd.events) {
            setEventsInvoked(modifyCmd.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

