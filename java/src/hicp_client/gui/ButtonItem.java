package hicp_client.gui;

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
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextItemAdapter;

public class ButtonItem
    extends Item
    implements TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( ButtonItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected TextItemAdapter _textItemAdapter;

    protected JButton _component;

    public ButtonItem(
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

    protected Item addInvoked(Add addCmd) {
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
            _textItemAdapter.setTextIdInvoked(addCmd.text);
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

    protected Item setEventsInvoked(final String eventsValue) {
        final boolean enabled = eventsValue.equals(Add.ENABLED);

        if (_component.isEnabled() != enabled) {
            _component.setEnabled(enabled);
        }
        return this;
    }

    protected Item modifyInvoked(final Modify modifyCmd) {
        // See what's changed.

        // New text item?
        if (null != modifyCmd.text) {
            _textItemAdapter.setTextIdInvoked(modifyCmd.text);
        }
        if (null != modifyCmd.events) {
            setEventsInvoked(modifyCmd.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

