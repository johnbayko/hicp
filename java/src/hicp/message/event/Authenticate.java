package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPReader;
import hicp.message.Message;

public class Authenticate
    extends Event
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
        // Write common header.
        super.write(out);

        if (null != user) {
            writeHeader(out, USER, user);
        }

        if (null != method) {
            writeHeader(out, METHOD, method);
        }

        if (null != password) {
            writeHeader(out, PASSWORD, password);
        }

        writeEndOfMessage(out);
    }

    public void read(HICPReader in) {
    }

    public void clear() {
        user = null;
        method = null;
        password = null;
    }
}
