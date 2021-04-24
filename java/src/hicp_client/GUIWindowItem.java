package hicp_client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Close;
import hicp.message.event.Event;
import hicp.message.event.EventEnum;

public class GUIWindowItem
    extends GUILayoutItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIWindowItem.class.getName() );

    // Should be used only from GUI thread.
    protected JFrame _component;
    protected JPanel _panel;

    public GUIWindowItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        super(addCmd, textLibrary, messageExchange);

        SwingUtilities.invokeLater(
            new RunNew(addCmd)
        );
    }

    class RunNew
        extends GUILayoutItem.RunNew
    {
        public RunNew(Add addCmd)
        {
            super(addCmd);
        }

        public void run()
        {
            _component = new JFrame();

            _component.getContentPane().setLayout(new BorderLayout());

            _panel = new JPanel(new GridBagLayout());

            _panel.addComponentListener(
                new ComponentAdapter() {
                    public void componentResized(ComponentEvent e) {
                        if(_component.isVisible()) {
                            final Dimension size = _component.getSize();
                            final Dimension preferredSize =
                                _component.getPreferredSize();
                            boolean resize = false;
                            if (size.width < preferredSize.width) {
                                size.width = preferredSize.width;
                                resize = true;
                            }
                            if (size.height < preferredSize.height) {
                                size.height = preferredSize.height;
                                resize = true;
                            }
                            if (resize) {
                                _component.setSize(size);
                            }
                        }
                    }
                }
            );

            _component
                .getContentPane()
                .add(new JScrollPane(_panel), BorderLayout.CENTER);

            _component.setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE
            );

            _component.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        // Send a close event with this object's ID.
                        final Close closeEvent =
                            (Close)EventEnum.CLOSE.newEvent();

                        closeEvent.id = idString;

                        _messageExchange.send(closeEvent);
                    }
                }
            );

            // Frame title.
            if (null != _addCmd.text) {
                setTextIdInvoked(_addCmd.text);
            } else {
                // No text for title bar, make up something.
                _component.setTitle("Window " + _addCmd.id); 
            }

            // Visible.
            if (_addCmd.visible) {
                // Default is false, change only if true.
                _component.setVisible(true);
            }

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

            // If visible, resize.
            if (_component.isVisible()) {
                _component.pack();
                _component.setSize(_component.getPreferredSize());
            }
        }
    }

    protected void removeComponentInvoked(Component component) {
        _panel.remove(component);
    }

    protected void addComponentInvoked(
        Component component, GridBagConstraints gridBagConstraints
    ) {
        _panel.add(component, gridBagConstraints);
    }

    protected Component getComponent() {
        return _component;
    }

    // These don't really make sense for a window, but needs to be
    // implemented.
    protected int getGridBagAnchor() {
        return java.awt.GridBagConstraints.CENTER;
    }

    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.NONE;
    }

    /**
        Called in GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        _component.setTitle(text);

        return this;
    }

    public void dispose() {
        // GUIContainerItem will remove any items added to this.
        super.dispose();

        if (null == _component) {
            return;
        }

        // Remove this from its parent.
//        if (null != _parent) {
//log("GUIWindowItem.dispose() about to _parent.remove()");  // debug
//            _parent.remove(this);
//        }

        // Dispose of this object.
        SwingUtilities.invokeLater(
            new RunDispose(_component)
        );
        _component = null;
    }

    class RunDispose
        implements Runnable
    {
        protected JFrame _component;

        public RunDispose(JFrame component) {
            _component = component;
        }

        public void run() {
            _component.dispose();
        }
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

            // If visible, resize.
            if (_component.isVisible()) {
                _component.pack();
                _component.setSize(_component.getPreferredSize());
            }
        }
    }

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        super.modifyInvoked(modifyCmd);
        // See what's changed.

        // New text item?
        if (null != modifyCmd.text) {
            setTextIdInvoked(modifyCmd.text);
        }

        // Visible?
        if (modifyCmd.visible != _component.isVisible()) {
            // Make sure correct size for children.
            _component.pack();
            _component.setSize(_component.getPreferredSize());

            _component.setVisible(modifyCmd.visible);
        }
        return this;
    }

    protected GUIItem applyTextDirectionInvoked() {
        // Set component orientation for component.
        // Only need horizontal orientation - vertial orientation
        // only applies to text/labels.
        if (TextDirection.RIGHT == getHorizontalTextDirection()) {
            _component
                .getContentPane()
                .setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        } else {
            _component
                .getContentPane()
                .setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }
        return this;
    }
}

