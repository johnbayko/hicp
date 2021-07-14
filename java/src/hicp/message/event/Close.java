package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HeaderMap;

public class Close
    extends GUIEvent
{
    public Close() {
        super(EventInfo.Event.CLOSE);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        writeEndOfMessage(out);
    }

    public Close addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }
}
