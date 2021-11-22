package hicp_client.gui.selection;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import hicp_client.gui.Positionable;
import hicp_client.gui.PositionInfo;
import hicp_client.text.TextLibrary;

public class DropdownItem
    extends Item
    implements Positionable
{
    private static final Logger LOGGER =
        Logger.getLogger( ScrollItem.class.getName() );
//LOGGER.log(Level.FINE, "");  // debug
//LOGGER.log(Level.FINE, " " + );  // debug

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    protected final PositionInfo _positionInfo;

    protected DropdownModel _dropdownModel = null;
    protected boolean _shouldSendChangedEvent = true;

    protected JComboBox<ItemText> _component;

    // TODO move to ItemText if duplicate.
    class SelectionItemRenderer
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
            if (null == value) {
                setText("");
                return this;
            }
            setText(value.getText());
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(value.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

    class DropdownModel
        extends DefaultComboBoxModel<ItemText>
    {
        // Also need to map by item ID.
        private Map<String, ItemText> _selectionItemMap = new HashMap<>();

        // GUI thread (addInvoked()).
        public DropdownModel(
            final List<GUISelectionInfo.Item> items
        ) {
            updateItems(items);
        }

        // GUI thread (addInvoked(), modifyInvoked()).
        public void updateItems(final List<GUISelectionInfo.Item> items) {
            removeAllElements();
            _selectionItemMap = new HashMap<>();

            if (null == items) {
                // No items to add.
                return;
            }
            int idx = 0;
            for (final var itemInfo : items) {
                final ItemText itemText =
                    new ItemText(null, _textLibrary, idx, itemInfo);

                addElement(itemText);
                _selectionItemMap.put(itemText.id, itemText);

                idx++;
            }
        }

        public ItemText getElementForId(final String itemId) {
            return _selectionItemMap.get(itemId);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            final var itemText = (ItemText)anItem;
            if (itemText.isEnabled()) {
                super.setSelectedItem(anItem);
            }
        }
    }

    public DropdownItem(
        Message m,
        TextLibrary textLibrary,
        MessageExchange messageExchange
    ) {
        super(m);
        _positionInfo = new PositionInfo(m);

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
                        if (!_shouldSendChangedEvent) {
                            // Being adjusted for some other reason.
                            return;
                        }

                        // There is no way around this, so suppress the warning.
                        @SuppressWarnings("unchecked")
                        final JComboBox<ItemText> component =
                            (JComboBox<ItemText>)e.getSource();

                        final ItemText i =
                            (ItemText)component.getSelectedItem();

                        sendChangedEvent(i);
                    }
                }
            );

        // Update selection, or send changed event for default selection.
        if (null != guiSelectionInfo.selected) {
            updateSelectedInvoked(guiSelectionInfo.selected);
        } else {
            // Nothing specified, inform app what the default selection is.
            final ItemText i = (ItemText)_component.getSelectedItem();

            sendChangedEvent(i);
        }

        if (guiSelectionInfo.hasHeight()) {
            final int height = guiSelectionInfo.getHeight();

            final int minHeight = 2;
            final int useHeight = (height < minHeight) ? minHeight : height;

            _component.setMaximumRowCount(useHeight);
        }

        return this;
    }

    // GUI Thread (addInvoked(), modifyInvoked())
    protected Item updateSelectedInvoked(
        final List<String> selected
    ) {
        if ((null == selected) || (0 == selected.size())) {
            // Selection not being updated.
            return this;
        }
        // If there are multiple selections, only one will be chosen, so send
        // an event indicating this change. If only one selection is sent, no
        // event is needed.
        _shouldSendChangedEvent = (1 < selected.size());

        // Only select the first.
        final String selectedId = selected.get(0);
        final ItemText si = _dropdownModel.getElementForId(selectedId);
        if (null == si) {
            // App thinks there's an element that's not there, skip.
            return this;
        }
        _component.setSelectedIndex(si.idx);

        _shouldSendChangedEvent = true;
        return this;
    }

    protected Item sendChangedEvent(final ItemText item) {
        final List<String> selected =
            (null != item)
                ? List.of(item.id)
                : List.of();

        final var changedEvent = new Message(EventInfo.Event.CHANGED);
        final var eventInfo = changedEvent.getEventInfo();
        final var itemInfo = eventInfo.getItemInfo();
        final var selectionInfo = itemInfo.getSelectionInfo();

        itemInfo.id = idString;
        selectionInfo.selected = selected;

        _messageExchange.send(changedEvent);
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

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected Item setEventsInvoked(final GUISelectionInfo.EventsEnum events) {
        final boolean isEnabled =
            (GUISelectionInfo.EventsEnum.ENABLED == events);
        if (isEnabled != _component.isEnabled()) {
            // Changed, update.
            _component.setEnabled(isEnabled);
        }
        return this;
    }

    protected Item modifyInvoked(final Message modifyCmd) {
        final var commandInfo = modifyCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        // See what's changed.
        if (null != guiSelectionInfo.items) {
            _dropdownModel.updateItems(guiSelectionInfo.items);
        }
        if (null != guiSelectionInfo.selected) {
            updateSelectedInvoked(guiSelectionInfo.selected);
        }
        if (null != guiSelectionInfo.events) {
            setEventsInvoked(guiSelectionInfo.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

