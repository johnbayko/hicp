package hicp_client;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Event;

public class GUIPanelItem
    extends GUILayoutItem
{
    // Should be used only from GUI thread.
    protected JPanel _component;

    public GUIPanelItem(
        Add addCmd,
        TextItem textItem,
        MessageExchange messageExchange
    ) {
        super(addCmd, textItem, messageExchange);

        SwingUtilities.invokeLater(
            new RunNew(addCmd, textItem)
        );
    }

    class RunNew
        extends GUILayoutItem.RunNew
    {
        protected final TextItem _textItem;

        public RunNew(Add addCmd, TextItem textItem)
        {
            super(addCmd);
            _textItem = textItem;
        }

        public void run()
        {
            _component = new JPanel();

            _component.setLayout(new GridBagLayout());

            super.run();
        }
    }

    protected GUIItem remove(GUIItem guiItem) {
        super.remove(guiItem);

        // Run an event to remove guiItem's component from this item's
        // component.
        SwingUtilities.invokeLater(new RunRemove(guiItem));

        return this;
    }

/*
    class RunRemove
        extends GUILayoutItem.RunRemove
    {
        public RunRemove(GUIItem guiItem)
        {
            super(guiItem);
        }

        public void run()
        {
            super.run();
        }
    }
*/

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

    /**
        Called in GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        return this;
    }

    public void dispose() {
log("GUIPanelItem.dispose() entered");  // debug
        // GUIContainerItem will remove any items added to this.
        super.dispose();
log("GUIPanelItem.dispose() done super.dispose()");  // debug

        if (null == _component) {
log("GUIItem has no component");  // debug
            return;
        }

        // Remove this from its parent.
//        if (null != _parent) {
//log("GUIPanelItem.dispose() about to _parent.remove()");  // debug
//            _parent.remove(this);
//        }

        // Dispose of this object.
log("GUIPanelItem.dispose() invokeLater(RunDispose)");  // debug
        _component = null;

log("GUIPanelItem.dispose() done remove");  // debug
    }

    public GUIItem add(GUIItem guiItem) {
        super.add(guiItem);

        // Run an event to add guiItem's component to this item's
        // component.
        SwingUtilities.invokeLater(new RunAdd(guiItem));

        return this;
    }

    class RunAdd
        extends GUILayoutItem.RunAdd
    {
        public RunAdd(GUIItem guiItem)
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

    public GUIItem modify(Modify modifyCmd, TextItem textItem) {
        super.modify(modifyCmd, textItem);

        SwingUtilities.invokeLater(
            new RunModify(modifyCmd, textItem)
        );

        return this;
    }

    class RunModify
        implements Runnable
    {
        protected final Modify _modifyCmd;
        protected final TextItem _textItem;

        public RunModify(Modify modifyCmd, TextItem textItem) {
            _modifyCmd = modifyCmd;
            _textItem = textItem;
        }

        public void run() {
            // See what's changed.

            // New text item?
            if (null != _textItem) {
                setTextItemInvoked(_textItem);
            }
        }
    }

    protected GUIItem applyTextDirectionInvoked() {
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

