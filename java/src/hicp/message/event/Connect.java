package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class Connect
    extends Event
{
    public String application = null;

    public Connect() {
        super(EventEnum.CONNECT);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common header.
        super.write(out);

        if (null != application) {
            writeHeader(out, HeaderEnum.APPLICATION.name, application);
        }

        writeEndOfMessage(out);
    }

    public Connect addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);

        application = headerMap.getString(HeaderEnum.APPLICATION);
        return this;
    }

    public HeaderMap getHeaders() {
        final HeaderMap headerMap = super.getHeaders();

        headerMap.putString(HeaderEnum.APPLICATION, application);

        return headerMap;
    }
}
