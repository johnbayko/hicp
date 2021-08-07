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
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.GUISelectionInfo;
import hicp.message.event.EventInfo;
import hicp_client.gui.Item;
import hicp_client.text.TextEvent;
import hicp_client.text.TextItem;
import hicp_client.text.TextLibrary;
import hicp_client.text.TextListener;
import hicp_client.text.TextListenerInvoker;

public class ToggleItem
    extends Item
{
    private static final Logger LOGGER =
        Logger.getLogger( ScrollItem.class.getName() );
//LOGGER.log(Level.FINE, "");  // debug

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    protected JPanel _component;

    protected ButtonGroup _buttonGroup = null;
    protected JRadioButton _noneButton = null;

    protected List<SelectionItem> _selectionItemList = null;

    protected GUISelectionInfo.EventsEnum selectionEvents =
        GUISelectionInfo.EventsEnum.ENABLED;

    class SelectionItem
        implements TextListener
    {
        public final String id;

        public final JRadioButton component;

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

            component = new JRadioButton();
            _buttonGroup.add(component);

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
            final boolean checkIsEnabled = isEnabled();

            if (!checkIsEnabled && isSelected()) {
                // TODO: As far as I can tell, this is being called as it
                // should, but component is not being selected.
                component.setSelected(false);
            }
            component.setEnabled(checkIsEnabled);

            return this;
        }

        public boolean isEnabled() {
            // Most restrictive.
            switch (selectionEvents) {
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

        _textLibrary = textLibrary;
        _messageExchange = messageExchange;
    }

    // GUI thread (addInvoked(), modifyInvoked()).
    public ToggleItem updateItems(
        final GUISelectionInfo guiSelectionInfo
    ) {
        _selectionItemList = new LinkedList<>();

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;

        // For single selection there should only be one selection, but
        // in case multiple are sent in error, this will use the last
        // one as the selection.
        final Set<String> selectionSet =
            (null != guiSelectionInfo.selected)
                ? new HashSet<>(guiSelectionInfo.selected)
                : Set.of();  // Nothing selected, empty (unused) set.

        for (final var item : guiSelectionInfo.items) {
            final boolean isSelected = selectionSet.contains(item.id);

            final SelectionItem si = new SelectionItem(item, isSelected);

            _selectionItemList.add(si);

            _component.add(si.component, c);
            c.gridy++;
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

    // GUI thread (modifyInvoked()).
    protected Item updateEvents(final GUISelectionInfo.EventsEnum events) {
        selectionEvents = events;

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

            updateItems(guiSelectionInfo);
            break;
          case MULTIPLE:
            _component = new JPanel(new GridBagLayout());
            _component.add(new JLabel("checkbox selection list"));  // debug
            break;
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
