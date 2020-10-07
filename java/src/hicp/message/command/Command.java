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

    public final static String AUTHENTICATE_STR = "authenticate";
    public final static String ADD_STR = "add";
    public final static String MODIFY_STR = "modify";
    public final static String REMOVE_STR = "remove";
    public final static String DISCONNECT_STR = "disconnect";

    public Command(String name, int id) {
        super(name, id);
    }

    public static Message getMessage(String name) {
        if (null == name) {
            return null;
        }
        switch (name) {
          case AUTHENTICATE_STR:
            return new Authenticate();
          case ADD_STR:
            return new Add();
          case MODIFY_STR:
            return new Modify();
          case REMOVE_STR:
            return new Remove();
          case DISCONNECT_STR:
            return new Disconnect();
          default:
            return null;
        }
    }
}

