package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

public class Remove
    extends AddModifyRemove
{
    public Remove(final String name) {
        super(name);
    }

    public Remove addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }
}
