package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.TextAttributes;

public class Changed
    extends GUIEvent
{
    private static final Logger LOGGER =
        Logger.getLogger( Changed.class.getName() );

    public String content = null;
    public TextAttributes attributes = null;
    public String[] selected = {};

    public Changed() {
        super(EventInfo.Event.CHANGED);
    }

    public void write(Writer out)
        throws IOException
    {
        // Write common headers.
        super.write(out);

        if (null != content) {
            writeHeader(out, HeaderEnum.CONTENT.name, content);
        }
        if (null != attributes) {
            final String attributesStr = attributes.toString();
            if (0 < attributesStr.length()) {
                writeHeader(out, HeaderEnum.ATTRIBUTES.name, attributes.toString());
            }
        }
        if (null != selected) {
            String selectedStr =
                Arrays.stream(selected)
                    .collect(Collectors.joining(", "));
            writeHeader(out, HeaderEnum.SELECTED.name, selectedStr);
        }

        writeEndOfMessage(out);
    }

    public Changed addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }
}
