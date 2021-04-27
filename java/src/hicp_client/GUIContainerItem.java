package hicp_client;

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
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Event;

public abstract class GUIContainerItem
    extends GUIItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIContainerItem.class.getName() );

    protected TextDirection _firstTextDirection = null;
    protected TextDirection _secondTextDirection = null;

    /** List of items this contains, if it's able to contain items. */
    protected List<GUIItem> _itemList = new LinkedList<>();

    /**
        Non-GUI thread.
     */
    public GUIContainerItem(final Add addCmd) {
        super(addCmd);
    }

    public GUIContainerItem() {
        super();
    }

    public GUIItem add(GUIItem guiItem) {
        synchronized (_itemList) {
            _itemList.add(guiItem);
        }
        return this;
    }

    // Socket thread.
    protected GUIItem remove(GUIItem guiItem) {
        synchronized (_itemList) {
            _itemList.remove(guiItem);
        }
        return this;
    }

    /**
        GUI thread.
     */
    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        if ( (null != modifyCmd.firstTextDirection)
          || (null != modifyCmd.secondTextDirection)
        ) {
            setTextDirectionInvoked(
                modifyCmd.firstTextDirection,
                modifyCmd.secondTextDirection
            );
        }
        return this;
    }

    /*
        Text direction can't be set to null, so don't change if
        parameter is null.
     */
    public GUIItem setTextDirectionInvoked(
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
                GUIItem item = (GUIItem)itemIterator.next();

                // Item will tell this object to remove it.
                item.dispose();
            }
            _itemList.clear();
        }
    }
}

