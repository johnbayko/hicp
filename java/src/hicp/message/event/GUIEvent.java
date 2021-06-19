package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPReader;

public abstract class GUIEvent
    extends Event
{
    public final static String ID = "id";

    public String id = null;

    public GUIEvent(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common header.
        super.write(out);

        if (null != id) {
            writeHeader(out, ID, id);
        }
    }
}
