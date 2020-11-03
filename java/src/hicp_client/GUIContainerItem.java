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
    public GUIContainerItem(Add addCmd) {
        super(addCmd);
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

    public GUIItem modify(Modify modifyCmd, TextItem textItem) {
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
            if ( (null != _modifyCmd.firstTextDirection)
              || (null != _modifyCmd.secondTextDirection)
            ) {
                setTextDirectionInvoked(
                    _modifyCmd.firstTextDirection,
                    _modifyCmd.secondTextDirection
                );
            }
        }
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
log("GUIContainerItem.dispose() entered");  // debug
        super.dispose();

        synchronized (_itemList) {
            final Iterator itemIterator = _itemList.iterator();
log("GUIContainerItem.dispose() got _itemList.iterator()");  // debug
            while (itemIterator.hasNext()) {
//apple.awt.EventQueueExceptionHandler Caught Throwable : java.util.ConcurrentModificationException
                GUIItem item = (GUIItem)itemIterator.next();

                // Item will tell this object to remove it.
log("about to dispose item " + item.component);  // debug
                item.dispose();
log("done dispose item " + item.component);  // debug
            }
            _itemList.clear();
        }
log("GUIContainerItem.dispose() done");  // debug
    }
}

