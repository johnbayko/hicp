package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

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
}
