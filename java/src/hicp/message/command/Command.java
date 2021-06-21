package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class Command
    extends Message
{
    public static String COMMAND = "command";

    private CommandEnum _command = null;

    public Command(String name) {
        super(name);
    }

    public Command addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);
        {
            final String commandStr = getHeaderString(HeaderEnum.COMMAND);
            // getEnum() handles null, may return null.
            _command = CommandEnum.getEnum(commandStr);
        }
        return this;
    }

    public CommandEnum getCommand() {
        return _command;
    }
}

