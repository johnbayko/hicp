package hicp_client.gui;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.Message;

public class PanelItem
    extends LayoutItem
{
    private static final Logger LOGGER =
        Logger.getLogger( PanelItem.class.getName() );

    // Should be used only from GUI thread.
    protected JPanel _component;

    public PanelItem(final Message m) {
        super(m);
    }

    /**
        GUI thread.
     */
    protected Item addInvoked(final Message addCmd) {
        _component = new JPanel();

        _component.setLayout(new GridBagLayout());

        return super.addInvoked(addCmd);
    }

    protected Item remove(Item guiItem) {
        super.remove(guiItem);

        // Run an event to remove guiItem's component from this item's
        // component.
        SwingUtilities.invokeLater(new RunRemove(guiItem));

        return this;
    }

    class RunRemove
        extends LayoutItem.RunRemove
    {
        public RunRemove(Item guiItem)
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

    protected Component getComponent() {
        return _component;
    }

    protected int getGridBagAnchor() {
        return java.awt.GridBagConstraints.CENTER;
    }

    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.BOTH;
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

        // Run an event to add guiItem's component to this item's
        // component.
        SwingUtilities.invokeLater(new RunAdd(guiItem));

        return this;
    }

    class RunAdd
        extends LayoutItem.RunAdd
    {
        public RunAdd(Item guiItem)
        {
            super(guiItem);
        }

        public void run()
        {
            if ( (POSITION_LIMIT <= horizontalPosition)
              && (POSITION_LIMIT <= verticalPosition)
              && (0 > horizontalPosition)
              && (0 > verticalPosition) )
            {
                // Exceeds max horizontal or vertical.
                return;
            }

            super.run();
        }
    }

    protected Item modifyInvoked(final Message modifyCmd) {
        super.modifyInvoked(modifyCmd);
        // See what's changed.

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

