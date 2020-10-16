package hicp.message.command;

import java.util.HashMap;
import java.util.Map;

import hicp.message.Message;

public abstract class Command
    extends Message
{
    public static String COMMAND = "command";

    public Command(String name) {
        super(name);
    }
}

