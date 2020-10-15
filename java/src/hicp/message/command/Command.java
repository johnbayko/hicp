package hicp.message.command;

import java.util.HashMap;
import java.util.Map;

import hicp.message.Message;

public abstract class Command
    extends Message
{
    public static String COMMAND = "command";

    public Command(String name, int id) {
        super(name, id);
    }

    public static Message getMessage(String name) {
        return CommandEnum.getMessage(name);
    }
}

