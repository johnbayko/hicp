package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.TextDirection;
import hicp.message.TextAttributes;
import hicp.message.Message;

public abstract class AddModify
    extends AddModifyRemove
{
    public final static String COMPONENT = "component";

    public final static String BUTTON = "button";
    public final static String LABEL = "label";
    public final static String PANEL = "panel";
    public final static String TEXTAREA = "textarea";
    public final static String TEXTFIELD = "textfield";
    public final static String WINDOW = "window";

    public final static String ATTRIBUTES = "attributes";
    public final static String CONTENT = "content";
    public final static String PARENT = "parent";
    public final static String POSITION = "position";
    public final static String SIZE = "size";
    public final static String TEXT_DIRECTION = "text-direction";
    public final static String VISIBLE = "visible";

    public String attributes = null;
    public TextAttributes textAttributes = null;

    public String content = null;
    public String component = null;
    public String parent = null;
    public String text = null;
    public boolean visible = false;

    public TextDirection firstTextDirection = null;
    public TextDirection secondTextDirection = null;

    public int horizontalPosition = 0;
    public int verticalPosition = 0;

    public int horizontalSize = 0;
    public int verticalSize = 0;

    protected static final Pattern commaSplitter =
        Pattern.compile("\\s,\\s");

    public AddModify(String name, int id) {
        super(name, id);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public void read(HICPReader in)
        throws IOException
    {
        try {
readLoop:   for (;;) {
                final HICPHeader hicpHeader = in.readHeader();
                if (null == hicpHeader.name) {
                    break readLoop;
                }
                readField(hicpHeader);
            }
        } catch (NullPointerException ex) {
            // Unexpected end of input - not really an error, so just
            // quietly return with whatever was read.
        }
    }

    protected boolean readField(HICPHeader hicpHeader) {
        if (super.readField(hicpHeader)) {
            return true;
        }

//log("AddModify.readField() field " + hicpHeader.name);  // debug
        // Extract recognized fields.
        if (TEXT.equals(hicpHeader.name)) {
            text = hicpHeader.value.getString();
            return true;
        } else if (ATTRIBUTES.equals(hicpHeader.name)) {
            attributes = hicpHeader.value.getString();
            textAttributes = new TextAttributes(attributes);
            return true;
        } else if (CONTENT.equals(hicpHeader.name)) {
            content = hicpHeader.value.getString();
            return true;
        } else if (COMPONENT.equals(hicpHeader.name)) {
            component = hicpHeader.value.getString();
            return true;
        } else if (PARENT.equals(hicpHeader.name)) {
            parent = hicpHeader.value.getString();
            return true;
        } else if (POSITION.equals(hicpHeader.name)) {
            // Only two values are needed, but if there are more,
            // shouldn't be appended to the second value by split(), so
            // split into three - any extra will be separated into third
            // String that's ignored.
            final String[] positions =
                commaSplitter.split(hicpHeader.value.getString(), 3);

            if (0 < positions.length) {
                try {
                    horizontalPosition = Integer.parseInt(positions[0]);
                } catch (NumberFormatException ex) {
                    horizontalPosition = 0;
                }
            }
            if (1 < positions.length) {
                try {
                    verticalPosition = Integer.parseInt(positions[1]);
                } catch (NumberFormatException ex) {
                    verticalPosition = 0;
                }
            }
            return true;
        } else if (SIZE.equals(hicpHeader.name)) {
            // Much like POSITION.
            final String[] sizes =
                commaSplitter.split(hicpHeader.value.getString(), 3);

            if (0 < sizes.length) {
                try {
                    horizontalSize = Integer.parseInt(sizes[0]);
                } catch (NumberFormatException ex) {
                    horizontalSize = 0;
                }
            }
            if (1 < sizes.length) {
                try {
                    verticalSize = Integer.parseInt(sizes[1]);
                } catch (NumberFormatException ex) {
                    verticalSize = 0;
                }
            }
            return true;
        } else if (TEXT_DIRECTION.equals(hicpHeader.name)) {
            // Only two values are needed, but if there are more,
            // shouldn't be appended to the second value by split(), so
            // split into three - any extra will be separated into third
            // String that's ignored.
            final String[] directions =
                commaSplitter.split(hicpHeader.value.getString(), 3);

            if (0 < directions.length) {
                firstTextDirection =
                    TextDirection.getTextDirection(directions[0]);
            }
            if (1 < directions.length) {
                secondTextDirection =
                    TextDirection.getTextDirection(directions[1]);
            }
            return true;
        } else if (VISIBLE.equals(hicpHeader.name)) {
            visible = Message.TRUE.equals(hicpHeader.value.getString());
            return true;
        }

        return false;
    }

    public void clear() {
        super.clear();

        component = null;
        parent = null;
        text = null;
        visible = false;
    }
}
