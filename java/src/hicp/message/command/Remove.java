package hicp.message.command;

import hicp.HeaderMap;

public class Remove
    extends ItemCommand
{
    public Remove(final String name) {
        super(name);
    }

    public Remove(
        final String name,
        final HeaderMap headerMap
    ) {
        super(name);
        addHeaders(headerMap);
    }
}
