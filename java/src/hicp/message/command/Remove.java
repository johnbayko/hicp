package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

public class Remove
    extends ItemCommand
{
    public Remove(final String name) {
        super(name);
    }

    public Remove(
        final String name,
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super(name);
        addHeaders(headerMap);
    }
}
