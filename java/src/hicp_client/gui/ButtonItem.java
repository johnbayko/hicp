package hicp_client.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.CommandInfo;
import hicp.message.command.GUIButtonInfo;
import hicp.message.event.EventInfo;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextItemAdapter;

public class ButtonItem
    extends Item
    implements Positionable, TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( ButtonItem.class.getName() );

    protected final PositionInfo _positionInfo;

    protected final MessageExchange _messageExchange;

    protected TextItemAdapter _textItemAdapter;

    protected JButton _component;

    public ButtonItem(
        final CommandInfo commandInfo,
        final MessageExchange messageExchange
    ) {
        super(commandInfo);
        _positionInfo = new PositionInfo(commandInfo);

        _messageExchange = messageExchange;
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    protected Item add(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiButtonInfo = guiInfo.getGUIButtonInfo();

        _component = new JButton();

        _component.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Send a click event with this object's ID.
                    final var event = new Message(EventInfo.Event.CLICK);
                    final var eventInfo = event.getEventInfo();
                    final var itemInfo = eventInfo.getItemInfo();

                    itemInfo.id = idString;

                    _messageExchange.send(event);
                }
            }
        );

        // Button text.
        if (null != guiButtonInfo.text) {
            _textItemAdapter.setTextId(guiButtonInfo.text);
        }
        // Button enable/disable.
        setEvents(guiButtonInfo.events);
        return this;
    }

    public Component getComponent() {
        return _component;
    }

    public PositionInfo getPositionInfo() {
        return _positionInfo;
    }

    public int getGridBagAnchor() {
        return java.awt.GridBagConstraints.CENTER;
    }

    public int getGridBagFill() {
        return java.awt.GridBagConstraints.NONE;
    }

    public void setText(String text) {
        _component.setText(text);
    }

    public void removeAdapter() {
        _textItemAdapter.removeAdapter();
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected Item setEvents(final GUIButtonInfo.EventsEnum events) {
        final boolean enabled =
            (null != events)
                ? (events == GUIButtonInfo.EventsEnum.ENABLED)
                : true; // Default is enabled.

        if (_component.isEnabled() != enabled) {
            _component.setEnabled(enabled);
        }
        return this;
    }

    protected Item modify(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiButtonInfo = guiInfo.getGUIButtonInfo();

        // See what's changed.

        // New text item?
        if (null != guiButtonInfo.text) {
            _textItemAdapter.setTextId(guiButtonInfo.text);
        }
        if (null != guiButtonInfo.events) {
            setEvents(guiButtonInfo.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

