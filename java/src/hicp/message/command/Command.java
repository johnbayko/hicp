package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPHeaderValue;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class Command
    extends Message
{
    private CommandEnum _command = null;

    public Command(String name) {
        super(name);
    }

    public Command(String name, final CommandEnum command) {
        super(name);
        _command = command;
    }

    public Command addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);

        // getEnum() handles null, may return null.
        _command =
            CommandEnum.getEnum(
                getHeaderString(HeaderEnum.COMMAND)
            );

        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        if (null != _command) {
            final HICPHeader h =
                new HICPHeader(
                    HeaderEnum.COMMAND,
                    new HICPHeaderValue(_command.name)
                );
            headerMap.put(HeaderEnum.COMMAND, h);
        }
        return headerMap;
    }

    public CommandEnum getCommand() {
        return _command;
    }
}

