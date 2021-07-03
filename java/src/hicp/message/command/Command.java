package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPHeaderValue;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class Command
    extends Message
{
    public Command(String name) {
        super(name);
    }
}

