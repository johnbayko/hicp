package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import hicp.message.Message;

public abstract class Event
    extends Message
{
    public static String EVENT = "event";

    public final static int CONNECT_ID = 1;
    public final static int AUTHENTICATE_ID = 2;
    public final static int CHANGED_ID = 3;
    public final static int CLOSE_ID = 4;
    public final static int CLICK_ID = 5;

    /*
        All these are used by GUI thread, except for the following,
        used by the input stream thread:
         - Authenticate
     */
    public final static Connect CONNECT =
        new Connect("connect", CONNECT_ID);

    public final static Authenticate AUTHENTICATE =
        new Authenticate("authenticate", AUTHENTICATE_ID);

    public final static Changed CHANGED =
        new Changed("changed", CHANGED_ID);

    public final static Close CLOSE =
        new Close("close", CLOSE_ID);

    public final static Click CLICK =
        new Click("click", CLICK_ID);

    protected static Map<String, Event> _messageMap;

    public Event(String name, int id) {
        super(name, id);
    }

    public static Message getMessage(String name) {
        if (null == name) {
            return null;
        }

        if (null == _messageMap) {
            _messageMap = new HashMap<>();
            _messageMap.put(CONNECT.getName(), CONNECT);
            _messageMap.put(AUTHENTICATE.getName(), AUTHENTICATE);
            _messageMap.put(CHANGED.getName(), CHANGED);
            _messageMap.put(CLOSE.getName(), CLOSE);
            _messageMap.put(CLICK.getName(), CLICK);
        }

        // If not in map, will return null.
        return (Message)_messageMap.get(name);
    }

    public void write(Writer out)
        throws IOException
    {
        writeHeader(out, EVENT, getName());

        out.flush();
    }
}

