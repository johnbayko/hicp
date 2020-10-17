package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import hicp.message.Message;

public abstract class Event
    extends Message
{
    public static String EVENT = "event";

    public Event(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
        writeHeader(out, EVENT, getName());

        out.flush();
    }
}

