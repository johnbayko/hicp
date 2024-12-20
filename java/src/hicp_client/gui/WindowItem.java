package hicp_client.gui;

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
import hicp.message.Message;
import hicp.message.command.CommandInfo;
import hicp.message.event.EventInfo;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextItemAdapter;

public class WindowItem
    extends LayoutItem
    implements TextItemAdapterListener
{
    private static final Logger LOGGER =
        Logger.getLogger( WindowItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected TextItemAdapter _textItemAdapter;

    protected JFrame _component;
    protected JPanel _panel;

    public WindowItem(
        final CommandInfo commandInfo,
        final MessageExchange messageExchange
    ) {
        super(commandInfo);
        _messageExchange = messageExchange;
    }

    public void setAdapter(TextItemAdapter tia) {
        _textItemAdapter = tia;
        _textItemAdapter.setAdapter(this);
    }

    protected Item add(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiWindowInfo = guiInfo.getGUIWindowInfo();

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
                    final var event = new Message(EventInfo.Event.CLOSE);
                    final var eventInfo = event.getEventInfo();
                    final var itemInfo = eventInfo.getItemInfo();

                    itemInfo.id = idString;

                    _messageExchange.send(event);
                }
            }
        );

        // Frame title.
        if (null != guiWindowInfo.text) {
            _textItemAdapter.setTextId(guiWindowInfo.text);
        } else {
            // No text for title bar, make up something.
            _component.setTitle("Window " + itemInfo.id); 
        }

        // Visible.
        if (guiWindowInfo.visible) {
            // Default is false, change only if true.
            _component.setVisible(true);
        }

        return super.add(commandInfo);
    }

    protected Item remove(Item guiItem) {
        super.remove(guiItem);

        if (guiItem instanceof Positionable) {
            // Run an event to remove guiItem's component from this item's
            // component.
            super.removePositionable((Positionable)guiItem);

            // If visible, resize.
            if (_component.isVisible()) {
                _component.pack();
                _component.setSize(_component.getPreferredSize());
            }
        }
        return this;
    }

    protected void removeComponent(Component component) {
        _panel.remove(component);
    }

    protected void addComponent(
        Component component, GridBagConstraints gridBagConstraints
    ) {
        _panel.add(component, gridBagConstraints);
    }

    public void setText(String text) {
        _component.setTitle(text);
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
        _component.dispose();
        _component = null;
    }

    public Item add(Item guiItem) {
        super.add(guiItem);

        if (guiItem instanceof Positionable) {
            // Run an event to add guiItem's component to this item's
            // component.
            super.addPositionable((Positionable)guiItem);

            // If visible, resize.
            if (_component.isVisible()) {
                _component.pack();
                _component.setSize(_component.getPreferredSize());
            }
        }
        return this;
    }

    protected Item modify(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiWindowInfo = guiInfo.getGUIWindowInfo();

        super.modify(commandInfo);
        // See what's changed.

        // New text item?
        if (null != guiWindowInfo.text) {
            _textItemAdapter.setTextId(guiWindowInfo.text);
        }

        // Visible?
        if (guiWindowInfo.visible != _component.isVisible()) {
            // Make sure correct size for children.
            _component.pack();
            _component.setSize(_component.getPreferredSize());

            _component.setVisible(guiWindowInfo.visible);
        }
        return this;
    }

    protected Item applyTextDirection() {
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

