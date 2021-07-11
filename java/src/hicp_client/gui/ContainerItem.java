package hicp_client.gui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.Message;
//import hicp.message.command.Add;
import hicp.message.command.LayoutGUIInfo;
import hicp.message.command.Modify;
import hicp.message.event.Event;

public abstract class ContainerItem
    extends Item
{
    private static final Logger LOGGER =
        Logger.getLogger( ContainerItem.class.getName() );

    protected TextDirection _firstTextDirection = null;
    protected TextDirection _secondTextDirection = null;

    /** List of items this contains, if it's able to contain items. */
    protected List<Item> _itemList = new LinkedList<>();

    /**
        Non-GUI thread.
     */
    public ContainerItem(final Message addCmd) {
        super(addCmd);
    }

    public ContainerItem() {
        super();
    }

    public Item add(Item guiItem) {
        synchronized (_itemList) {
            _itemList.add(guiItem);
        }
        return this;
    }

    // Socket thread.
    protected Item remove(Item guiItem) {
        synchronized (_itemList) {
            _itemList.remove(guiItem);
        }
        return this;
    }

    /**
        GUI thread.
     */
    protected Item modifyInvoked(final Modify modifyCmd) {
        final var commandInfo = modifyCmd.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var layoutGUIInfo = guiInfo.getLayoutGUIInfo();

        if ( (null != layoutGUIInfo.textDirection.first)
          || (null != layoutGUIInfo.textDirection.second)
        ) {
            setTextDirectionInvoked(
                layoutGUIInfo.textDirection.first,
                layoutGUIInfo.textDirection.second
            );
        }
        return this;
    }

    /*
        Text direction can't be set to null, so don't change if
        parameter is null.
     */
    public Item setTextDirectionInvoked(
        TextDirection firstTextDirection,
        TextDirection secondTextDirection
    ) {
        // These don't modify any displayed components, so don't need to
        // be changed in the GUI event thread.
        if (null != firstTextDirection) {
            _firstTextDirection = firstTextDirection;
        }
        if (null != secondTextDirection) {
            _secondTextDirection = secondTextDirection;
        }

        return this;
    }

    public TextDirection getFirstTextDirection() {
        if (null != _firstTextDirection) {
            return _firstTextDirection;
        } else {
            if (null != _parent) {
                return _parent.getFirstTextDirection();
            } else {
                return null;
            }
        }
    }

    public TextDirection getSecondTextDirection() {
        if (null != _secondTextDirection) {
            return _secondTextDirection;
        } else {
            if (null != _parent) {
                return _parent.getSecondTextDirection();
            } else {
                return null;
            }
        }
    }

    public TextDirection getHorizontalTextDirection() {
        final TextDirection firstTextDirection = getFirstTextDirection();
        if ( (TextDirection.LEFT == firstTextDirection)
          || (TextDirection.RIGHT == firstTextDirection)
        ) {
            return firstTextDirection;
        }
        {
            final TextDirection secondTextDirection = getSecondTextDirection();
            return getSecondTextDirection();
        }
    }

    public void dispose() {
        super.dispose();

        synchronized (_itemList) {
            final Iterator itemIterator = _itemList.iterator();
            while (itemIterator.hasNext()) {
                Item item = (Item)itemIterator.next();

                // Item will tell this object to remove it.
                item.dispose();
            }
            _itemList.clear();
        }
    }
}

