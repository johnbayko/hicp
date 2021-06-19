package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class Close
    extends GUIEvent
{
    public Close(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        writeEndOfMessage(out);
    }

    public Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        return this;
    }
}
