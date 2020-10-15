package hicp.message.command;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.Message;

public class Disconnect
    extends Command
{
    public Disconnect(final String name, final int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Disconnect(_name, _id);
    }

    public void write(Writer out)
        throws IOException
    {
        // No fields to write.
    }

    public void read(HICPReader in)
        throws IOException
    {
        // No fields to read.
    }

    public void clear() {
        // No fields to clear.
    }
}
