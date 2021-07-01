package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.TextDirection;
import hicp.message.TextAttributes;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class AddModify
    extends ItemCommand
{
    private static final Logger LOGGER =
        Logger.getLogger( AddModify.class.getName() );

    public final static String COMPONENT = "component";

    public final static String BUTTON = "button";
    public final static String LABEL = "label";
    public final static String PANEL = "panel";
    public final static String SELECTION = "selection";
    public final static String TEXTPANEL = "textpanel";
    public final static String TEXTFIELD = "textfield";
    public final static String WINDOW = "window";

    public final static String ATTRIBUTES = "attributes";
    public final static String CONTENT = "content";
    public final static String ITEMS = "items";
    public final static String PARENT = "parent";
    public final static String POSITION = "position";
    public final static String SELECTED = "selected";
    public final static String SIZE = "size";
    public final static String TEXT_DIRECTION = "text-direction";
    public final static String VISIBLE = "visible";
    public final static String EVENTS = "events";

    // Values for events header.
    public final static String ENABLED = "enabled";
    public final static String DISABLED = "disabled";
    public final static String SERVER = "server";

    public String attributes = null;
    public TextAttributes textAttributes = null;

    public String content = null;
    public String items = null;
    public String component = null;
    public String parent = null;
    public String[] selected = null;
    public String text = null;
    public boolean visible = false;
    public String events = null;

    public TextDirection firstTextDirection = null;
    public TextDirection secondTextDirection = null;

    public int horizontalPosition = 0;
    public int verticalPosition = 0;

    public int horizontalSize = 0;
    public int verticalSize = 0;

    public AddModify(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public AddModify addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        // TODO make independent from Remove.
        super.addHeaders(headerMap);

        for (final HeaderEnum h : headerMap.keySet()) {
            final HICPHeader v = headerMap.get(h);
            switch (h) {
              case COMPONENT:
                component = v.value.getString();
                break;
              case ATTRIBUTES:
                attributes = v.value.getString();
                break;
              case CONTENT:
                content = v.value.getString();
                break;
              case ITEMS:
                items = v.value.getString();
                break;
              case PARENT:
                parent = v.value.getString();
                break;
              case POSITION:
                // TODO move actual parsing to lower level, keep this a string.

                // Only two values are needed, but if there are more,
                // shouldn't be appended to the second value by split(), so
                // split into three - any extra will be separated into third
                // String that's ignored.
                final String[] positions =
                    COMMA_SPLITTER.split(v.value.getString(), 3);

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
                break;
              case SELECTED:
                // TODO move actual parsing to lower level, keep this a string.

                // List of integers, but actually strings (can't do math on
                // them).
                selected = COMMA_SPLITTER.split(v.value.getString());

                // split("") will create a 1 element array of [""], treat that
                // as null.
                if ((1 == selected.length) && ("".equals(selected[0]))) {
                    selected = null;
                }
                break;
              case SIZE:
                // TODO move actual parsing to lower level, keep this a string.

                // Much like POSITION.
                final String[] sizes =
                    COMMA_SPLITTER.split(v.value.getString(), 3);

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
                break;
              case TEXT:
                text = v.value.getString();
                break;
              case TEXT_DIRECTION:
                // TODO move actual parsing to lower level, keep this a string.

                // Only two values are needed, but if there are more,
                // shouldn't be appended to the second value by split(), so
                // split into three - any extra will be separated into third
                // String that's ignored.
                final String[] directions =
                    COMMA_SPLITTER.split(v.value.getString(), 3);

                if (0 < directions.length) {
                    firstTextDirection =
                        TextDirection.getTextDirection(directions[0]);
                }
                if (1 < directions.length) {
                    secondTextDirection =
                        TextDirection.getTextDirection(directions[1]);
                }
                break;
              case VISIBLE:
                visible = Message.TRUE.equals(v.value.getString());
                break;
              case EVENTS:
                events = v.value.getString();
                break;
            }
        }
        return this;
    }
}
