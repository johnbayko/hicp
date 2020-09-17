package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPReader;
import hicp.message.Message;

public class Changed
    extends GUIEvent
{
    public static String CONTENT = "content";

    public String content = null;

    public Changed(String name, int id) {
        super(name, id);
    }

    public Message newMessage() {
        return new Changed(getName(), getID());
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        if (null != content) {
            writeHeader(out, CONTENT, content);
log(getName() + " send content \"" + content + "\"");  // debug
        }

        writeEndOfMessage(out);
    }
}
