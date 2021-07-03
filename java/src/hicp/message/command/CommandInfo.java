package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPHeaderValue;
import hicp.message.HeaderEnum;

public class CommandInfo {
    // TODO Move command enum into here.

    public final CommandEnum command;

    public CommandInfo(final Map<HeaderEnum, HICPHeader> headerMap) {
        command =
            CommandEnum.getEnum(
                headerMap.get(HeaderEnum.COMMAND).value.getString()
            );
    }

    public CommandInfo updateHeaderMap(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        if (null != command) {
            final HICPHeader h =
                new HICPHeader(
                    HeaderEnum.COMMAND,
                    new HICPHeaderValue(command.name)
                );
            headerMap.put(HeaderEnum.COMMAND, h);
        }
        return this;
    }
}
