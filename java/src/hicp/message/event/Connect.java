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

    public Connect(String name) {
        super(name);
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
}
