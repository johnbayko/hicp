package hicp_client.gui.selection;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.CommandInfo;
import hicp.message.command.GUISelectionInfo;
import hicp.message.event.EventInfo;
import hicp_client.gui.Item;
import hicp_client.gui.Positionable;
import hicp_client.gui.PositionInfo;
import hicp_client.text.TextLibrary;

public class ScrollItem
    extends Item
    implements Positionable
{
    private static final Logger LOGGER =
        Logger.getLogger( ScrollItem.class.getName() );
//LOGGER.log(Level.FINE, "");  // debug

    protected final PositionInfo _positionInfo;

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    private SelectionListModel _listModel = null;
    private SelectionItemSelection _listSelectionModel = null;

    protected Component _component;

    class SelectionListModel
        extends AbstractListModel<ItemText>
        implements ItemText.ChangeListener
    {
        // Empty list by default.
        private List<ItemText> _selectionItemList = new ArrayList<>();

        // Also need to map by item ID.
        private Map<String, ItemText> _selectionItemMap = new HashMap<>();

        public SelectionListModel(
            final List<GUISelectionInfo.Item> items
        ) {
            updateItems(items);
        }

        public void updateItems(final List<GUISelectionInfo.Item> items) {
            // When items change, selection no longer applies so must be
            // cleared.
            // Might not have been set yet, check for null first.
            if (null != _listSelectionModel) {
                _listSelectionModel.clearSelection();
            }

            final int oldSize = _selectionItemList.size();
            _selectionItemList = new ArrayList<>(items.size());
            _selectionItemMap = new HashMap<>();

            for (final var itemInfo : items) {
                // Index from 0 to size - 1, next index will be size.
                final int newIdx = _selectionItemList.size();

                final ItemText itemText =
                    new ItemText(this, _textLibrary, newIdx, itemInfo);

                _selectionItemList.add(itemText);
                _selectionItemMap.put(itemText.id, itemText);
            }
            final int newSize = _selectionItemList.size();
            final int size = Math.max(oldSize, newSize);

            fireContentsChanged(this, 0, size-1);
        }

        public ItemText getElementAt(final int index) {
            return _selectionItemList.get(index);
        }

        public int getSize() {
            return _selectionItemList.size();
        }

        public ItemText getElementForId(final String itemId) {
            return _selectionItemMap.get(itemId);
        }

        // Inform JList that this item changed.
        public void itemChanged(final ItemText si) {
            final int idx = si.idx;

            fireContentsChanged(this, idx, idx);
        }

        public void fireContentsChanged() {
            fireContentsChanged(this, 0, _selectionItemList.size()-1);
        }
    }

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
            final SelectionItemSelection sm =
                (SelectionItemSelection)list.getSelectionModel();
            setEnabled(
                (GUISelectionInfo.EventsEnum.ENABLED == sm.getEvents())
              && value.isEnabled()
            );
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

    public static class SelectionRange {
        public final int index0;
        public final int index1;
        public SelectionRange(final int newIndex0, final int newIndex1) {
            index0 = newIndex0;
            index1 = newIndex1;
        }
    }

    /**
        Manage selection so list can be disabled, or individual item selection
        can be disabled. Overrides were based on logging which public methods
        were called, I suspect that's implementation dependent and could change
        with different Java version or system platforms, but I can't see a way
        around that.
     */
    class SelectionItemSelection
        extends DefaultListSelectionModel
    {
        private boolean _shouldSendChangedEvent = true;

        private GUISelectionInfo.EventsEnum events =
            GUISelectionInfo.EventsEnum.ENABLED;

        public SelectionItemSelection(
            final GUISelectionInfo.EventsEnum newEvents,
            final List<String> newSelected
        ) {
            setEvents(newEvents);
            updateSelected(newSelected);
        }

        public SelectionItemSelection setEvents(
            final GUISelectionInfo.EventsEnum newEvents
        ) {
            if (null == newEvents) {
                return this;
            }
            events = newEvents;
            return this;
        }

        public GUISelectionInfo.EventsEnum getEvents() {
            return events;
        }

        public SelectionItemSelection updateSelected(
            final List<String> selected
        ) {
            if (null == selected) {
                // Selection not being updated.
                return this;
            }
            _shouldSendChangedEvent = false;
            clearSelection();

            final int selectedLen = selected.size();
            if (0 == selectedLen) {
                // Empty selection, nothing to add.
                return this;
            }
            // selected[0] must exist, no check needed below.

            // Find idx for each selected id.
            final int[] selectedIdxList = new int[selectedLen];
            for (int scanIdx = 0;
                scanIdx < selectedLen;
                scanIdx++)
            {
                final var selection = selected.get(scanIdx);
                final ItemText si = _listModel.getElementForId(selection);
                // Possible for si to be null here - log that?

                selectedIdxList[scanIdx] = si.idx;
            }

            // Go through array, add selection interval when indexes are
            // discontinuous or end of array.
            int prevIdx = selectedIdxList[0];
            int startIdx = prevIdx;
            for (int scanIdx = 1;
                scanIdx < selectedIdxList.length;
                scanIdx++)
            {
                final int expectedIdx = prevIdx + 1;
                final int selectedIdx = selectedIdxList[scanIdx];

                if (selectedIdx != expectedIdx) {
                    addSelectionInterval(startIdx, prevIdx);
                    startIdx = selectedIdx;
                }
                prevIdx = selectedIdx;
            }
            // Final interval (even if intervals were added in the loop, there
            // will always be an interval at the end).
            final int selectedIdxLast = selectedIdxList.length - 1;
            final int endIdx = selectedIdxList[selectedIdxLast];

            addSelectionInterval(startIdx, endIdx);

            _shouldSendChangedEvent = true;
            return this;
        }

        /**
            Take a range for elements in a list model, and return a list of
            ranges where all elements are enabled. Works for ascending or
            decending order.
         */
        protected List<SelectionRange> enabledRanges(int index0, int index1) {
            final List<SelectionRange> enabledRanges = new LinkedList<>();

            // Indexes can go up or down. Set scan direction here.
            final int step = (index0 > index1) ? -1 : 1;
            final int stopIdx = index1 + step;

            int rangeStart = index0;
            boolean prevIsEnabled = false;

            for (int scanIdx = index0; scanIdx != stopIdx;scanIdx += step) {
                // Find range start or end - that is, current and previous item
                // enabled value changes.
                final ItemText si = _listModel.getElementAt(scanIdx);
                final boolean isEnabled = si.isEnabled();

                if (prevIsEnabled && !isEnabled) {
                    // Change from enabled to disbled means end of a range, add
                    // to list.
                    enabledRanges
                        .add(new SelectionRange(rangeStart, scanIdx-step));
                } else if (!prevIsEnabled && isEnabled) {
                    // Change from disabled to enabled means start of a range.
                    rangeStart = scanIdx;
                }
                prevIsEnabled = isEnabled;
            }
            if (prevIsEnabled) {
                // Last item was enabled, ended with an enabled range, add to
                // list.
                enabledRanges.add(new SelectionRange(rangeStart, index1));
            }
            return enabledRanges;
        }

        @Override
        public void setSelectionInterval(int index0, int index1) {
            if (GUISelectionInfo.EventsEnum.ENABLED == events) {
                // First call should be super.setSelectionInterval(),
                // all others should be super.addSelectionInterval().
                boolean isFirstRange = true;

                for (final SelectionRange r : enabledRanges(index0, index1)) {
                    if (isFirstRange) {
                        // Set.
                        super.setSelectionInterval(r.index0, r.index1);
                        isFirstRange = false;
                    } else {
                        // Add.
                        super.addSelectionInterval(r.index0, r.index1);
                    }
                }
            }
        }
        @Override
        public void addSelectionInterval(int index0, int index1) {
            if (GUISelectionInfo.EventsEnum.ENABLED == events) {
                for (final SelectionRange r : enabledRanges(index0, index1)) {
                    super.addSelectionInterval(r.index0, r.index1);
                }
            }
        }
        @Override
        public void removeSelectionInterval(int index0, int index1) {
            if ( (GUISelectionInfo.EventsEnum.ENABLED == events)
              || (GUISelectionInfo.EventsEnum.UNSELECT == events) )
            {
                for (final SelectionRange r : enabledRanges(index0, index1)) {
                    super.removeSelectionInterval(r.index0, r.index1);
                }
            }
        }
        // Leaving these here in case they need to be overridden in other
        // versions or implementations.
//        @Override
//        public void insertIndexInterval(int index, int length, boolean before) {
//            super.insertIndexInterval(index, length, before);
//        }
//        @Override
//        public void removeIndexInterval(int index0, int index1) {
//            super.removeIndexInterval(index0, index1);
//        }
//        @Override
//        public void setAnchorSelectionIndex(int anchorIndex) {
//            super.setAnchorSelectionIndex(anchorIndex);
//        }
//        @Override
//        public void moveLeadSelectionIndex(int leadIndex) {
//            super.moveLeadSelectionIndex(leadIndex);
//        }
//        @Override
//        public void setLeadSelectionIndex(int leadIndex) {
//            super.setLeadSelectionIndex(leadIndex);
//        }
//        @Override
//        public void clearSelection() {
//            super.clearSelection();
//        }

        public boolean shouldSendChangedEvent() {
            return _shouldSendChangedEvent;
        }
    }


    public ScrollItem(
        CommandInfo commandInfo,
        TextLibrary textLibrary,
        MessageExchange messageExchange
    ) {
        super(commandInfo);
        _positionInfo = new PositionInfo(commandInfo);

        _textLibrary = textLibrary;
        _messageExchange = messageExchange;
    }

    protected Item add(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        final JList<ItemText> newList = new JList<>();

        _listModel = new SelectionListModel(guiSelectionInfo.items);
        newList.setModel(_listModel);

        newList.setCellRenderer(new SelectionItemRenderer());

        _listSelectionModel =
            new SelectionItemSelection(
                guiSelectionInfo.events, guiSelectionInfo.selected
            );
        newList.setSelectionModel(_listSelectionModel);

        switch (guiSelectionInfo.mode) {
          case SINGLE:
            newList
                .setSelectionMode(
                    ListSelectionModel.SINGLE_SELECTION
                );
            break;
          case MULTIPLE:
            newList
                .setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                );
            break;
        }

        newList
            .addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(final ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            // Wait until last event to make new event
                            // message
                            return;
                        }
                        final JList source = (JList)e.getSource();

                        // Is this updated internally? Then don't send an event.
                        final SelectionItemSelection sm =
                            (SelectionItemSelection)source.getSelectionModel();
                        if (!sm.shouldSendChangedEvent()) {
                            return;
                        }

                        final int[] selectedIndices =
                            source.getSelectedIndices();

                        // Convert indexes to IDs.
                        final List<String> selected = new LinkedList<>();

                        for (int idx = 0;
                            idx < selectedIndices.length;
                            idx++
                        ) {
                            final int selectedIdx = selectedIndices[idx];
                            final ItemText si =
                                _listModel.getElementAt(selectedIdx);
                            selected.add(si.id);
                        }

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

        if (guiSelectionInfo.hasHeight()) {
            final int height = guiSelectionInfo.getHeight();

            final int minHeight = 2;
            final int useHeight = (height < minHeight) ? minHeight : height;

            newList.setVisibleRowCount(useHeight);
        }

        _component = new JScrollPane(newList);

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

    protected Item setEvents(final GUISelectionInfo.EventsEnum events) {
        if (events != _listSelectionModel.getEvents()) {
            _listSelectionModel.setEvents(events);
            // Make JList redisplay items as enabled/disabled.
            _listModel.fireContentsChanged();
        }
        return this;
    }

    protected Item modify(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        // See what's changed.
        if (null != guiSelectionInfo.items) {
            _listModel.updateItems(guiSelectionInfo.items);
        }
        if (null != guiSelectionInfo.selected) {
            _listSelectionModel.updateSelected(guiSelectionInfo.selected);
        }
        if (null != guiSelectionInfo.events) {
            setEvents(guiSelectionInfo.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

