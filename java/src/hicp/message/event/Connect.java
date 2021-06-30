package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class Connect
    extends Event
{
    public final static String APPLICATION = "application";

    public String application = null;

    public Connect() {
        super(EventEnum.CONNECT.messageName, EventEnum.CONNECT);
    }

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

    public Connect addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);

        application = getHeaderString(HeaderEnum.APPLICATION);
        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        addHeaderString(headerMap, HeaderEnum.APPLICATION, application);

        return headerMap;
    }
}
