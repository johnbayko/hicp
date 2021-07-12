package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.message.HeaderEnum;

public abstract class GUIEvent
    extends Event
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIEvent.class.getName() );

    public String id = null;

    public GUIEvent(final EventEnum event) {
        super(event);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common header.
        super.write(out);

        if (null != id) {
            writeHeader(out, HeaderEnum.ID.name, id);
        }
    }
}
