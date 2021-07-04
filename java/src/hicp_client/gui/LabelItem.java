package hicp_client.gui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
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

    public LabelItem(final Add addCmd) {
        super(addCmd);
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    /**
        GUI thread.
     */
    protected Item addInvoked(final Add addCmd) {
        _component = new JLabel();

        // Label string.
        if (null != addCmd.text) {
            _textItemAdapter.setTextIdInvoked(addCmd.text);
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

    protected Item modifyInvoked(final Modify modifyCmd) {
        // See what's changed.

        // New text item?
        if (null != modifyCmd.text) {
            _textItemAdapter.setTextIdInvoked(modifyCmd.text);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

