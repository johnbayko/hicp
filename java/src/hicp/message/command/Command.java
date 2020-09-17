package hicp.message.command;

import java.util.HashMap;
import java.util.Map;

import hicp.message.Message;

public abstract class Command
    extends Message
{
    public static String COMMAND = "command";

    public final static int AUTHENTICATE_ID = 1;
    public final static int ADD_ID = 2;
    public final static int MODIFY_ID = 3;
    public final static int REMOVE_ID = 4;
    public final static int DISCONNECT_ID = 5;

    public final static Authenticate AUTHENTICATE =
        new Authenticate("authenticate", AUTHENTICATE_ID);
    public final static Add ADD =
        new Add("add", ADD_ID);
    public final static Modify MODIFY =
        new Modify("modify", MODIFY_ID);
    public final static Remove REMOVE =
        new Remove("remove", REMOVE_ID);
    public final static Disconnect DISCONNECT =
        new Disconnect("disconnect", DISCONNECT_ID);

    protected static Map<String, Command> _messageMap;

    public Command(String name, int id) {
        super(name, id);
    }

    public static Message getMessage(String name) {
        if (null == name) {
            return null;
        }

        if (null == _messageMap) {
            _messageMap = new HashMap<>();
            _messageMap.put(AUTHENTICATE.getName(), AUTHENTICATE);
            _messageMap.put(ADD.getName(), ADD);
            _messageMap.put(MODIFY.getName(), MODIFY);
            _messageMap.put(REMOVE.getName(), REMOVE);
            _messageMap.put(DISCONNECT.getName(), DISCONNECT);
        }

        // If not in map, will return null.
        return (Message)_messageMap.get(name);
    }
}

