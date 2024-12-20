package hicp_client.gui;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.command.CommandInfo;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextItemAdapter;

public class PanelItem
    extends LayoutItem
    implements Positionable, TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( PanelItem.class.getName() );

    protected final PositionInfo _positionInfo;
    protected TextItemAdapter _textItemAdapter;

    // Should be used only from GUI thread.
    protected JPanel _component;
    protected TitledBorder _border;

    public PanelItem(final CommandInfo commandInfo) {
        super(commandInfo);
        _positionInfo = new PositionInfo(commandInfo);
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    protected Item add(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiPanelInfo = guiInfo.getGUIPanelInfo();

        _component = new JPanel();
        _component.setLayout(new GridBagLayout());

        // Panel string.
        if (null != guiPanelInfo.text) {
            // Text gets set by TextitemAdapter calling this.
            _border = javax.swing.BorderFactory.createTitledBorder("");
            _component.setBorder(_border);

            _textItemAdapter.setTextId(guiPanelInfo.text);
        }
        return super.add(commandInfo);
    }

    protected Item remove(Item guiItem) {
        super.remove(guiItem);

        if (guiItem instanceof Positionable) {
            // Run an event to remove guiItem's component from this item's
            // component.
            super.removePositionable((Positionable)guiItem);
        }
        return this;
    }

    protected void removeComponent(Component component) {
        _component.remove(component);
    }

    protected void addComponent(
        Component component, GridBagConstraints gridBagConstraints
    ) {
        _component.add(component, gridBagConstraints);
    }

    public Component getComponent() {
        return _component;
    }

    public PositionInfo getPositionInfo() {
        return _positionInfo;
    }

    public int getGridBagAnchor() {
        return java.awt.GridBagConstraints.CENTER;
    }

    public int getGridBagFill() {
        return java.awt.GridBagConstraints.BOTH;
    }

    public void setText(String text) {
        _border.setTitle(text);
        // Known bug, repaint after changing border text.
        _component.repaint();
    }

    public void removeAdapter() {
        _textItemAdapter.removeAdapter();
    }

    public void dispose() {
        // ContainerItem will remove any items added to this.
        super.dispose();

        if (null == _component) {
            return;
        }

        // Remove this from its parent.
//        if (null != _parent) {
//            _parent.remove(this);
//        }

        // Dispose of this object.
        _component = null;
    }

    public Item add(Item guiItem) {
        super.add(guiItem);

        if (guiItem instanceof Positionable) {
            // Run an event to add guiItem's component to this item's
            // component.
            super.addPositionable((Positionable)guiItem);
        }
        return this;
    }

    protected Item modify(final CommandInfo commandInfo) {
        super.modify(commandInfo);

        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiPanelInfo = guiInfo.getGUIPanelInfo();

        // See what's changed.

        // New text item?
        if (null != guiPanelInfo.text) {
            _textItemAdapter.setTextId(guiPanelInfo.text);
        }
        return this;
    }

    protected Item applyTextDirection() {
        // Set component orientation for component.
        // Only need horizontal orientation - vertial orientation
        // only applies to text/labels.
        if (TextDirection.RIGHT == getHorizontalTextDirection()) {
            _component
                .setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        } else {
            _component
                .setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
        return this;
    }
}

