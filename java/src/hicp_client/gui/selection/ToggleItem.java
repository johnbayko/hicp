package hicp_client.gui.selection;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.GUISelectionInfo;
import hicp.message.event.EventInfo;
import hicp_client.gui.Item;
import hicp_client.gui.Positionable;
import hicp_client.gui.PositionInfo;
import hicp_client.text.TextEvent;
import hicp_client.text.TextItem;
import hicp_client.text.TextLibrary;
import hicp_client.text.TextListener;
import hicp_client.text.TextListenerInvoker;

public class ToggleItem
    extends Item
    implements Positionable
{
    private static final Logger LOGGER =
        Logger.getLogger( ScrollItem.class.getName() );
//LOGGER.log(Level.FINE, "");  // debug
//LOGGER.log(Level.FINE, " " + );  // debug

    protected final PositionInfo _positionInfo;

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    protected JPanel _component;

    protected ButtonGroup _buttonGroup = null;
    protected JRadioButton _noneButton = null;

    protected List<SelectionItem> _selectionItemList = null;

    private boolean _hasHeight = false;
    private int _height = 0;

    private boolean _hasWidth = false;
    private int _width = 0;

    protected GUISelectionInfo.Mode _mode =
        GUISelectionInfo.Mode.MULTIPLE;

    protected GUISelectionInfo.EventsEnum _selectionEvents =
        GUISelectionInfo.EventsEnum.ENABLED;

    class SelectionItem
        implements TextListener
    {
        public final String id;

        public final JToggleButton component;

        private final GUISelectionInfo.EventsEnum events;

        public SelectionItem(
            final GUISelectionInfo.Item itemInfo,
            final boolean isSelected
        ) {
            id = itemInfo.id;
            events =
                (null != itemInfo.events)
                    ? itemInfo.events
                    : GUISelectionInfo.EventsEnum.ENABLED;

            // Component type based on selection mode.
            switch (_mode) {
              case SINGLE:
                component = new JRadioButton();
                _buttonGroup.add(component);
                break;
              case MULTIPLE:
                component = new JCheckBox();
                break;
              default:
                // Should not happen because there are no other values.
                // Will throw exception later.
                component = null;
                break;
            }

            // This will be added to _selectionItemList after construction but
            // before being added to the container component, so the listener
            // will never be triggered until after then, meaning it's okay for
            // the listener to look for this item in that list.
            component.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        // Create and send a selection changed event.
                        final var changedEvent =
                            new Message(EventInfo.Event.CHANGED);
                        final var eventInfo = changedEvent.getEventInfo();
                        final var itemInfo = eventInfo.getItemInfo();
                        final var selectionInfo = itemInfo.getSelectionInfo();

                        itemInfo.id = idString;

                        final List<String> selected = new LinkedList<>();
                        for (final SelectionItem si : _selectionItemList) {
                            if (si.isSelected()) {
                                selected.add(si.id);
                            }
                        }
                        selectionInfo.selected = selected;

                        _messageExchange.send(changedEvent);

                        // Update component enabled status because deselecting
                        // might change it.
                        component.setEnabled(isEnabled());
                    }
                }
            );
            {
                final TextItem textItem = _textLibrary.get(itemInfo.textId);
                component.setText(textItem.getText());
                textItem.addTextListener(new TextListenerInvoker(this));
            }
            {
                final boolean checkIsEnabled = isEnabled();
                component.setEnabled(checkIsEnabled);
                if (checkIsEnabled && isSelected) {
                    component.setSelected(true);
                }
            }
        }

        // GUI thread.
        public void textChanged(TextEvent e) {
            TextItem ti = (TextItem)e.getSource();
            component.setText(ti.getText());
        }

        public boolean isSelected() {
            return component.isSelected();
        }

        public SelectionItem updateEnabled() {
            component.setEnabled(isEnabled());
            return this;
        }

        public boolean isEnabled() {
            // Most restrictive.
            switch (_selectionEvents) {
              case ENABLED:
                return (GUISelectionInfo.EventsEnum.ENABLED == events);
              case DISABLED:
                return false;
              case UNSELECT:
                if (GUISelectionInfo.EventsEnum.ENABLED == events)  {
                    // Enabled if selected, disabled if unselected.
                    return isSelected();
                } else {
                    // Disabled.
                    return false;
                }
            }
            // Shouldn't get here, return default.
            return true;
        }
    }

    public ToggleItem(
        Message m,
        TextLibrary textLibrary,
        MessageExchange messageExchange
    ) {
        super(m);
        _positionInfo = new PositionInfo(m);

        _textLibrary = textLibrary;
        _messageExchange = messageExchange;
    }

    // GUI thread (addInvoked(), modifyInvoked()).
    public ToggleItem updateItems(
        final GUISelectionInfo guiSelectionInfo
    ) {
        // If any items exist and are selected, they will be deselected, so
        // send empty selection event in that case.
        if (null != _selectionItemList) {
            boolean foundSelected = false;
            for (final var si : _selectionItemList) {
                if (si.isSelected()) {
                    foundSelected = true;
                    break;
                }
            }
            if (foundSelected) {
                // Create and send a selection changed event.
                final var changedEvent = new Message(EventInfo.Event.CHANGED);
                final var eventInfo = changedEvent.getEventInfo();
                final var itemInfo = eventInfo.getItemInfo();
                final var selectionInfo = itemInfo.getSelectionInfo();

                itemInfo.id = idString;

                // Empty selection list - won't send anything if it's null.
                selectionInfo.selected = new LinkedList<>();

                _messageExchange.send(changedEvent);
            }
        }
        _selectionItemList = new LinkedList<>();

        final GridBagConstraints c = new GridBagConstraints();
        // TODO: This should all follow the text direction of the container
        // it's added to.
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;

        // For single selection there should only be one selection, but
        // in case multiple are sent in error, this will use the last
        // one as the selection.
        final Set<String> selectionSet =
            (null != guiSelectionInfo.selected)
                ? new HashSet<>(guiSelectionInfo.selected)
                : Set.of();  // Nothing selected, empty (unused) set.

        // If width or height are specified, give priority to width.
        // E.g for 9 items, if height=4 and width=2, make 2 columns (5 and 4).
        // If height = 4 and no width, make 3 columns (4, 4, and 1).
        final int yLimit;
        if (_hasWidth) {
            // Calculate limit from width and num items.
            final int numItems = guiSelectionInfo.items.size();

            yLimit = (numItems + (_width - 1)) / _width;
        } else if (_hasHeight) {
            // Use height as the limit.
            yLimit = _height;
        } else {
            // No limit.
            yLimit = guiSelectionInfo.items.size();
        }

        for (final var item : guiSelectionInfo.items) {
            final boolean isSelected = selectionSet.contains(item.id);

            final SelectionItem si = new SelectionItem(item, isSelected);

            _selectionItemList.add(si);
            _component.add(si.component, c);

            c.gridy++;
            if (yLimit <= c.gridy) {
                c.gridy = 0;
                c.gridx++;
            }
        }
        _component.revalidate();  // debug
        _component.repaint();  // debug

        return this;
    }

    // GUI thread (modifyInvoked()).
    public ToggleItem updateSelected(
        final List<String> selected
    ) {
        final Set<String> selectionSet =
            (null != selected)
                ? new HashSet<>(selected)
                : Set.of();  // Nothing selected, empty (unused) set.

        for (final SelectionItem si : _selectionItemList) {
            final boolean isSelected = selectionSet.contains(si.id);

            si.component.setSelected(isSelected);
        }
        return this;
    }

    // GUI thread (addInvoked(), modifyInvoked()).
    protected Item updateEvents(final GUISelectionInfo.EventsEnum events) {
        _selectionEvents = events;

        for (final SelectionItem si : _selectionItemList) {
            si.updateEnabled();
        }
        return this;
    }

    protected Item addInvoked(final Message addCmd) {
        final var commandInfo = addCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        switch (guiSelectionInfo.mode) {
          case SINGLE:
            // Single selection needs a button group and undisplayed "none"
            // button.
            _buttonGroup = new ButtonGroup();
            _noneButton = new JRadioButton();
            _noneButton.setSelected(true);  // By default, only one selected.
            _buttonGroup.add(_noneButton);

            _component = new JPanel(new GridBagLayout());

            break;
          case MULTIPLE:
            _component = new JPanel(new GridBagLayout());
            break;
        }

        // These are only set when adding, not when modifying, so save them for
        // any future modify command.
        _hasHeight = guiSelectionInfo.hasHeight();
        _height = guiSelectionInfo.getHeight();
        _hasWidth = guiSelectionInfo.hasWidth();
        _width = guiSelectionInfo.getWidth();

        if (null != guiSelectionInfo.events) {
            updateEvents(guiSelectionInfo.events);
        }
        if (null != guiSelectionInfo.mode) {
            _mode = guiSelectionInfo.mode;
        }
        updateItems(guiSelectionInfo);

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

    protected Item modifyInvoked(final Message modifyCmd) {
        // See what's changed.
        final var commandInfo = modifyCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        // See what's changed.
        if (null != guiSelectionInfo.items) {
            // Remove all old items in _selectionItemList, add these new items.
            // Any selection is implicitly cleared (is there an event?
            // probably, check that).
            for (final SelectionItem si : _selectionItemList) {
                _component.remove(si.component);
            }
            updateItems(guiSelectionInfo);
        // Update items sets selected status, don't do that again,
        // so "else if" here.
        } else if (null != guiSelectionInfo.selected) {
            updateSelected(guiSelectionInfo.selected);
        }
        if (null != guiSelectionInfo.events) {
            updateEvents(guiSelectionInfo.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}
