package hicp.message.command;

import hicp.HeaderMap;

public class Modify
    extends AddModify
{
    public Modify(final String name) {
        super(name);
    }

    public Modify addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }
}
