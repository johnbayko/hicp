package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPReader;
import hicp.message.Message;

public class Connect
    extends Event
{
    public final static String APPLICATION = "application";

    public String application = null;

    public Connect(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Connect(getName(), getID());
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common header.
        super.write(out);

        if (null != application) {
            writeHeader(out, APPLICATION, application);
        }

        writeEndOfMessage(out);
    }

    public void read(HICPReader in) {
    }

    public void clear() {
        application = null;
    }
}
