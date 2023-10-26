package hicp_client.gui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.CommandInfo;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextItemAdapter;

public class LabelItem
    extends Item
    implements Positionable, TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( LabelItem.class.getName() );

    protected final PositionInfo _positionInfo;
    protected TextItemAdapter _textItemAdapter;

    protected JLabel _component;

    public LabelItem(final CommandInfo commandInfo) {
        super(commandInfo);
        _positionInfo = new PositionInfo(commandInfo);
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    protected Item add(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiLabelInfo = guiInfo.getGUILabelInfo();

        _component = new JLabel();

        // Label string.
        if (null != guiLabelInfo.text) {
            _textItemAdapter.setTextId(guiLabelInfo.text);
        }
        return this;
    }

    public Component getComponent() {
        return _component;
    }

    public PositionInfo getPositionInfo() {
        return _positionInfo;
    }

    public int getGridBagAnchor() {
        if (hicp.TextDirection.RIGHT == _parent.getHorizontalTextDirection()) {
            return java.awt.GridBagConstraints.WEST;
        } else {
            return java.awt.GridBagConstraints.EAST;
        }
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

    protected Item modify(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiLabelInfo = guiInfo.getGUILabelInfo();

        // See what's changed.

        // New text item?
        if (null != guiLabelInfo.text) {
            _textItemAdapter.setTextId(guiLabelInfo.text);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

