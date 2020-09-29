package hicp.message.event;

import java.io.IOException;
import java.io.Writer;

import hicp.HICPReader;
import hicp.message.Message;
import hicp.message.TextAttributes;

public class Changed
    extends GUIEvent
{
    public static String CONTENT = "content";
    public static String ATTRIBUTES = "attributes";

    public String content = null;
    public TextAttributes attributes = null;

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
        if (null != attributes) {
            writeHeader(out, ATTRIBUTES, attributes.toString());
log(getName() + " send content \"" + content + "\"");  // debug
        }

        writeEndOfMessage(out);
    }
}
