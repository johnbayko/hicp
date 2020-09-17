package hicp.message.command;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.Message;

public class Authenticate
    extends Command
{
    public final static String USER = "user";
    public final static String METHOD = "method";
    public final static String PASSWORD = "password";

    public String user = null;
    public String method = null;
    public String password = null;

    public Authenticate(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Authenticate(getName(), getID());
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public void read(HICPReader in)
        throws IOException
    {
        try {
readLoop:   for (;;) {
                final HICPHeader hicpHeader = in.readHeader();
                if (null == hicpHeader.name) {
                    break readLoop;
                }

                // Extract recognized fields.
                if (USER.equals(hicpHeader.name)) {
                    user = hicpHeader.value.getString();
                } else if (METHOD.equals(hicpHeader.name)) {
                    method = hicpHeader.value.getString();
                } else if (PASSWORD.equals(hicpHeader.name)) {
                    password = hicpHeader.value.getString();
                }
            }
        } catch (NullPointerException ex) {
            // Unexpected end of input - not really an error, so just
            // quietly return with whatever was read.
        }
    }

    public void clear() {
        user = null;
        method = null;
        password = null;
    }
}
