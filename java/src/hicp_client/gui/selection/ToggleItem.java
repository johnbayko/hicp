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

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    protected Component _component;

    protected ButtonGroup _buttonGroup = null;
    protected JRadioButton _noneButton = null;

    protected List<SelectionItem> _selectionItemList = new LinkedList<>();

    class SelectionItem
        implements TextListener
    {
        public final String id;

        public final JRadioButton component;

        public SelectionItem(
            final GUISelectionInfo.Item itemInfo,
            final boolean isSelected
        ) {
            id = itemInfo.id;

            final JRadioButton newComponent = new JRadioButton();
            _buttonGroup.add(newComponent);

            // This will be added to _selectionItemList after construction but
            // before being added to the container component, so the listener
            // will never be triggered until after then, meaning it's okay for
            // the listener to look for this item in that list.
            newComponent.addActionListener(
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
                    }
                }
            );

            final TextItem textItem = _textLibrary.get(itemInfo.textId);
            newComponent.setText(textItem.getText());
            newComponent.setSelected(isSelected);
            textItem.addTextListener(new TextListenerInvoker(this));

            // TODO enabled



            component = newComponent;
        }

        // GUI thread.
        public void textChanged(TextEvent e) {
            TextItem ti = (TextItem)e.getSource();
            component.setText(ti.getText());
        }

        public boolean isSelected() {
            return component.isSelected();
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

            final JPanel newPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;

            // For single selection there should only be one selection, but in
            // case multiple are sent in error, this will use the last one as
            // the selection.
            final Set<String> selectionSet =
                (null != guiSelectionInfo.selected)
                    ? new HashSet<>(guiSelectionInfo.selected)
                    : Set.of();  // Nothing selected, empty (unused) set.

            for (final var item : guiSelectionInfo.items) {
                final boolean isSelected = selectionSet.contains(item.id);

                final SelectionItem si =
                    new SelectionItem(item, isSelected);

                _selectionItemList.add(si);

                newPanel.add(si.component, c);
                c.gridy++;
            }

            _component = newPanel;
            break;
          case MULTIPLE:
            _component = new JLabel("checkbox selection list");  // debug
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
        // Changed parent ID is handled by Controller.
        return this;
    }
}
