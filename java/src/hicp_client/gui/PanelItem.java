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
import hicp.message.Message;
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

    public PanelItem(final Message m) {
        super(m);
        _positionInfo = new PositionInfo(m);
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    /**
        GUI thread.
     */
    protected Item addInvoked(final Message addCmd) {
        final var commandInfo = addCmd.getCommandInfo();
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

            _textItemAdapter.setTextIdInvoked(guiPanelInfo.text);
        }
        return super.addInvoked(addCmd);
    }

    protected Item remove(Item guiItem) {
        super.remove(guiItem);

        if (guiItem instanceof Positionable) {
            // Run an event to remove guiItem's component from this item's
            // component.
            SwingUtilities.invokeLater(new RunRemove((Positionable)guiItem));
        }
        return this;
    }

    class RunRemove
        extends LayoutItem.RunRemove
    {
        public RunRemove(Positionable guiItem)
        {
            super(guiItem);
        }

        public void run()
        {
            super.run();
        }
    }

    protected void removeComponentInvoked(Component component) {
        _component.remove(component);
    }

    protected void addComponentInvoked(
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

    /**
        Called in GUI thread.
     */
    public void setTextInvoked(String text) {
        _border.setTitle(text);
    }

    public void removeAdapter() {
        _textItemAdapter.removeAdapter();
    }

    public void dispose() {
LOGGER.log(Level.FINE, "PanelItem.dispose() entered");  // debug
        // ContainerItem will remove any items added to this.
        super.dispose();
LOGGER.log(Level.FINE, "PanelItem.dispose() done super.dispose()");  // debug

        if (null == _component) {
LOGGER.log(Level.FINE, "Item has no component");  // debug
            return;
        }

        // Remove this from its parent.
//        if (null != _parent) {
//LOGGER.log(Level.FINE, "PanelItem.dispose() about to _parent.remove()");  // debug
//            _parent.remove(this);
//        }

        // Dispose of this object.
LOGGER.log(Level.FINE, "PanelItem.dispose() invokeLater(RunDispose)");  // debug
        _component = null;

LOGGER.log(Level.FINE, "PanelItem.dispose() done remove");  // debug
    }

    public Item add(Item guiItem) {
        super.add(guiItem);

        if (guiItem instanceof Positionable) {
            // Run an event to add guiItem's component to this item's
            // component.
            SwingUtilities.invokeLater(new RunAdd((Positionable)guiItem));
        }
        return this;
    }

    class RunAdd
        extends LayoutItem.RunAdd
    {
        public RunAdd(Positionable guiItem)
        {
            super(guiItem);
        }

        public void run()
        {
            super.run();
        }
    }

    protected Item modifyInvoked(final Message modifyCmd) {
        super.modifyInvoked(modifyCmd);

        final var commandInfo = modifyCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiPanelInfo = guiInfo.getGUIPanelInfo();

        // See what's changed.

        // New text item?
        if (null != guiPanelInfo.text) {
            _textItemAdapter.setTextIdInvoked(guiPanelInfo.text);
        }
        return this;
    }

    protected Item applyTextDirectionInvoked() {
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

