package hicp_client.gui.selection;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.GUISelectionInfo;
import hicp.message.event.EventInfo;
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

    protected JComboBox<ItemText> _component;

    // TODO move to ItemText if duplicate.
    static class SelectionItemRenderer
        extends JLabel
        implements ListCellRenderer<ItemText>
    {
        public Component getListCellRendererComponent(
            JList<? extends ItemText> list,
            ItemText value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            setText(value.getText());
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
//            final SelectionItemSelection sm =
//                (SelectionItemSelection)list.getSelectionModel();
//            setEnabled(
//                (GUISelectionInfo.EventsEnum.ENABLED == sm.getEvents())
//              && value.isEnabled()
//            );
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

    class DropdownModel
        extends DefaultComboBoxModel<ItemText>
    {
        // GUI thread (addInvoked()).
        public DropdownModel(
            final List<GUISelectionInfo.Item> items
        ) {
            updateItems(items);
        }

        // GUI thread (addInvoked(), modifyInvoked()).
        public void updateItems(final List<GUISelectionInfo.Item> items) {
            int idx = 0;

            for (final var itemInfo : items) {
                final ItemText itemText =
                    new ItemText(null, _textLibrary, idx, itemInfo);

                addElement(itemText);
                idx++;
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

        _component.setRenderer(new SelectionItemRenderer());

        _component
            .addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final JComboBox<ItemText> component =
                            (JComboBox<ItemText>)e.getSource();

                        final ItemText i =
                            (ItemText)component.getSelectedItem();

                        final List<String> selected = List.of(i.id);

                        final var changedEvent =
                            new Message(EventInfo.Event.CHANGED);
                        final var eventInfo = changedEvent.getEventInfo();
                        final var itemInfo = eventInfo.getItemInfo();
                        final var selectionInfo = itemInfo.getSelectionInfo();

                        itemInfo.id = idString;
                        selectionInfo.selected = selected;

                        _messageExchange.send(changedEvent);
                    }
                }
            );

        // TODO Update selection, or send changed event for default selection.

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

