package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.Message;
import hicp.message.HeaderEnum;

public class Authenticate
    extends Event
{
    public final static String USER = "user";
    public final static String METHOD = "method";
    public final static String PASSWORD = "password";

    public String user = null;
    public String method = null;
    public String password = null;

    public Authenticate(String name) {
        super(name);
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

    public Message parseHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        return this;
    }
}
