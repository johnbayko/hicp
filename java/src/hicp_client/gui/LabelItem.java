package hicp_client.gui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextItemAdapter;

public class LabelItem
    extends Item
    implements TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( LabelItem.class.getName() );

    protected TextItemAdapter _textItemAdapter;

    protected JLabel _component;

    public LabelItem(final Message m) {
        super(m);
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    /**
        GUI thread.
     */
    protected Item addInvoked(final Message addCmd) {
        final var commandInfo = addCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiLabelInfo = guiInfo.getGUILabelInfo();

        _component = new JLabel();

        // Label string.
        if (null != guiLabelInfo.text) {
            _textItemAdapter.setTextIdInvoked(guiLabelInfo.text);
        }
        return this;
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

    protected Item modifyInvoked(final Message modifyCmd) {
        final var commandInfo = modifyCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiLabelInfo = guiInfo.getGUILabelInfo();

        // See what's changed.

        // New text item?
        if (null != guiLabelInfo.text) {
            _textItemAdapter.setTextIdInvoked(guiLabelInfo.text);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

