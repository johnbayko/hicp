package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPReader;
import hicp.message.Message;

public class Click
    extends GUIEvent
{
    public Click(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Click(getName(), getID());
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        writeEndOfMessage(out);
    }
}
