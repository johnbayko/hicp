package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

public class Modify
    extends AddModify
{
    public Modify(final String name) {
        super(name);
    }

    public Modify addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }
}
