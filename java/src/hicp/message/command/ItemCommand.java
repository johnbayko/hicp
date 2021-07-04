package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class ItemCommand
    extends Command
{
    public ItemCommand(final String name) {
        super(name);
    }

    public ItemCommand(
        final String name,
        final HeaderMap headerMap
    ) {
        super(name);

        addHeaders(headerMap);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public ItemCommand addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }

    public HeaderMap getHeaders() {
        final HeaderMap headerMap = super.getHeaders();
        return headerMap;
    }
}
