package hicp_client;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
log("GUIContainerItem done _itemList.add()");  // debug
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
log("GUIContainerItem.RunModify checking for text direction");  // debug
            if ( (null != _modifyCmd.firstTextDirection)
              || (null != _modifyCmd.secondTextDirection)
            ) {
log("about to set text direction");  // debug
                setTextDirectionInvoked(
                    _modifyCmd.firstTextDirection,
                    _modifyCmd.secondTextDirection
                );
            }
else {
log("no text direction");  // debug
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
log("GUIContainerItem.getFirstTextDirection() " + _firstTextDirection);  // debug
            return _firstTextDirection;
        } else {
            if (null != _parent) {
log("GUIContainerItem.getFirstTextDirection() none, get from parent");  // debug
                return _parent.getFirstTextDirection();
            } else {
log("GUIContainerItem.getFirstTextDirection() none, no parent, return null");  // debug
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
if (null != firstTextDirection) {
log("GUIContainerItem.getHorizontalTextDirection() firstTextDirection "
    + firstTextDirection.toString()
);  // debug
} else {  // debug
log("GUIContainerItem.getHorizontalTextDirection() firstTextDirection null");  // debug
}
        if ( (TextDirection.LEFT == firstTextDirection)
          || (TextDirection.RIGHT == firstTextDirection)
        ) {
            return firstTextDirection;
        }
        {
            final TextDirection secondTextDirection = getSecondTextDirection();
if (null != secondTextDirection) {
log("GUIContainerItem.getHorizontalTextDirection() getSecondTextDirection() "
    + getSecondTextDirection().toString()
);  // debug
} else {
log("GUIContainerItem.getHorizontalTextDirection() getSecondTextDirection() null");  // debug
}
            return getSecondTextDirection();
        }
    }

    public void dispose() {
log("GUIContainerItem.dispose() entered");  // debug
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

