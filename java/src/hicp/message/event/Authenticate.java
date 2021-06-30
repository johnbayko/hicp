package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;
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

    public Authenticate() {
        super(EventEnum.AUTHENTICATE.messageName, EventEnum.AUTHENTICATE);
    }

    public Authenticate(String name) {
        super(name);
    }

    public Authenticate(
        final String name,
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super(name);
        addHeaders(headerMap);
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

    public Authenticate addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);

        user = getHeaderString(HeaderEnum.USER);
        // Method is single string for event
        method = getHeaderString(HeaderEnum.METHOD);
        password = getHeaderString(HeaderEnum.PASSWORD);

        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        addHeaderString(headerMap, HeaderEnum.USER, user);
        addHeaderString(headerMap, HeaderEnum.METHOD, method);
        addHeaderString(headerMap, HeaderEnum.PASSWORD, password);

        return headerMap;
    }
}
