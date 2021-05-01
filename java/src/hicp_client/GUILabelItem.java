package hicp_client;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;

public class GUILabelItem
    extends GUIItem
    implements TextItemAdapterListener
{
    protected TextItemAdapter _textItemAdapter;

    protected JLabel _component;

    public GUILabelItem(final Add addCmd) {
        super(addCmd);
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    /**
        GUI thread.
     */
    protected GUIItem addInvoked(final Add addCmd) {
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

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        // See what's changed.

        // New text item?
        if (null != modifyCmd.text) {
            _textItemAdapter.setTextIdInvoked(modifyCmd.text);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

