package hicp_client.gui.selection;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashSet;
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

            final Set<String> selectionSet =
                (null != guiSelectionInfo.selected)
                    ? new HashSet<>(guiSelectionInfo.selected)
                    : new TreeSet<>();  // Nothing selected, empty (unused) set.
            // Note I think TreeSet allocates nothing when empty, so is a
            // better unused set than HashSet.

            for (final var item : guiSelectionInfo.items) {
                final boolean isSelected = selectionSet.contains(item.id);

                final SelectionItem si =
                    new SelectionItem(item, isSelected);

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
