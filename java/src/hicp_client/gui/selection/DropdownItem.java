package hicp_client.gui.selection;

import java.awt.Component;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.GUISelectionInfo;
import hicp_client.gui.Item;
import hicp_client.text.TextLibrary;

public class DropdownItem
    extends Item
{
    private static final Logger LOGGER =
        Logger.getLogger( ScrollItem.class.getName() );

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    private DropdownModel _dropdownModel = null;

    protected JComboBox<String> _component;

    class DropdownModel
        extends DefaultComboBoxModel<String>
    {
        // GUI thread (addInvoked()).
        public DropdownModel(
            final List<GUISelectionInfo.Item> items
        ) {
            updateItems(items);
        }

        // GUI thread (addInvoked(), modifyInvoked()).
        public void updateItems(final List<GUISelectionInfo.Item> items) {
            for (final var itemInfo : items) {
                addElement("id: " + itemInfo.id);
            }
        }
    }

    public DropdownItem(
        Message m,
        TextLibrary textLibrary,
        MessageExchange messageExchange
    ) {
        super(m);

        _textLibrary = textLibrary;
        _messageExchange = messageExchange;
    }

    protected Item addInvoked(final Message addCmd) {
        final var commandInfo = addCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        // Mode is ignored, only single is supported.
        _component = new JComboBox<>();

        _dropdownModel = new DropdownModel(guiSelectionInfo.items);
        _component.setModel(_dropdownModel);

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

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected Item modifyInvoked(final Message modifyCmd) {
        // See what's changed.
        // Changed parent ID is handled by Controller.
        return this;
    }
}

