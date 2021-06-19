package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.Message;
import hicp.message.HeaderEnum;
import hicp.message.TextAttributes;

public class Changed
    extends GUIEvent
{
    private static final Logger LOGGER =
        Logger.getLogger( Changed.class.getName() );

    public static String CONTENT = "content";
    public static String ATTRIBUTES = "attributes";
    public static String SELECTED = "selected";

    public String content = null;
    public TextAttributes attributes = null;
    public String[] selected = {};

    public Changed(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        if (null != content) {
            writeHeader(out, CONTENT, content);
        }
        if (null != attributes) {
            final String attributesStr = attributes.toString();
            if (0 < attributesStr.length()) {
                writeHeader(out, ATTRIBUTES, attributes.toString());
            }
        }
        if (null != selected) {
            String selectedStr =
                Arrays.stream(selected)
                    .collect(Collectors.joining(", "));
            writeHeader(out, SELECTED, selectedStr);
        }

        writeEndOfMessage(out);
    }

    public Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        return this;
    }
}
