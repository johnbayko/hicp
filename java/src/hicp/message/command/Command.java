package hicp.message.command;

import hicp.message.Message;

public abstract class Command
    extends Message
{
    public static String COMMAND = "command";

    public Command(String name) {
        super(name);
    }
}

