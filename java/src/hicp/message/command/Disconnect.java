package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class Disconnect
    extends Command
{
    public Disconnect(final String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
        // No fields to write.
    }

    public void clear() {
        // No fields to clear.
    }

    public Message parseHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        // No fields to read.
        return this;
    }
}
