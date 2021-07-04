package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;

public class Click
    extends GUIEvent
{
    private static final Logger LOGGER =
        Logger.getLogger( Click.class.getName() );

    public Click(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        writeEndOfMessage(out);
    }

    public Click addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }
}
