package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class Authenticate
    extends Event
{
    public String user = null;
    public String method = null;
    public String password = null;

    public Authenticate() {
        super(EventInfo.Event.AUTHENTICATE);
    }

    public Authenticate(
        final HeaderMap headerMap
    ) {
        super(EventInfo.Event.AUTHENTICATE);
        addHeaders(headerMap);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common header.
        super.write(out);

        if (null != user) {
            writeHeader(out, HeaderEnum.USER.name, user);
        }

        if (null != method) {
            writeHeader(out, HeaderEnum.METHOD.name, method);
        }

        if (null != password) {
            writeHeader(out, HeaderEnum.PASSWORD.name, password);
        }

        writeEndOfMessage(out);
    }

    public Authenticate addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);

        user = headerMap.getString(HeaderEnum.USER);

        // Method is single string for event
        method = headerMap.getString(HeaderEnum.METHOD);
        password = headerMap.getString(HeaderEnum.PASSWORD);

        return this;
    }

    public HeaderMap getHeaders() {
        final HeaderMap headerMap = super.getHeaders();

        headerMap.putString(HeaderEnum.USER, user);
        headerMap.putString(HeaderEnum.METHOD, method);
        headerMap.putString(HeaderEnum.PASSWORD, password);

        return headerMap;
    }
}
