package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.TextDirection;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class AddModify
    extends ItemCommand
{
    private static final Logger LOGGER =
        Logger.getLogger( AddModify.class.getName() );

    public AddModify(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public AddModify addHeaders(
        final HeaderMap headerMap
    ) {
        // TODO make independent from Remove.
        super.addHeaders(headerMap);

        return this;
    }
}
