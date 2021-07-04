package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
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
        final HeaderMap headerMap
    ) {
        // TODO make independent from Remove.
        super.addHeaders(headerMap);

        component = headerMap.getString(HeaderEnum.COMPONENT);
        attributes = headerMap.getString(HeaderEnum.ATTRIBUTES);
        content = headerMap.getString(HeaderEnum.CONTENT);
        items = headerMap.getString(HeaderEnum.ITEMS);
        parent = headerMap.getString(HeaderEnum.PARENT);
        {
            final String positionsStr =
                headerMap.getString(HeaderEnum.POSITION);
            if (null != positionsStr) {
                // Only two values are needed, but if there are more,
                // shouldn't be appended to the second value by split(), so
                // split into three - any extra will be separated into third
                // String that's ignored.
                final String[] positions =
                    COMMA_SPLITTER.split(positionsStr, 3);

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
            }
        }
        {
            final String selectedStr = 
                headerMap.getString(HeaderEnum.SELECTED);
            if (null != selectedStr) {
                // List of integers, but actually strings (can't do math on
                // them).
                selected = COMMA_SPLITTER.split(selectedStr);

                // split("") will create a 1 element array of [""], treat that
                // as null.
                if ((1 == selected.length) && ("".equals(selected[0]))) {
                    selected = null;
                }
            }
        }
        {
            final String sizesStr = 
                headerMap.getString(HeaderEnum.SIZE);
            if (null != sizesStr) {
                // Much like POSITION.
                final String[] sizes = COMMA_SPLITTER.split(sizesStr, 3);

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
            }
        }
        text = headerMap.getString(HeaderEnum.TEXT);
        {
            final String directionsStr = 
                headerMap.getString(HeaderEnum.TEXT_DIRECTION);
            if (null != directionsStr) {
                // Only two values are needed, but if there are more,
                // shouldn't be appended to the second value by split(), so
                // split into three - any extra will be separated into third
                // String that's ignored.
                final String[] directions =
                    COMMA_SPLITTER.split(directionsStr, 3);

                if (0 < directions.length) {
                    firstTextDirection =
                        TextDirection.getTextDirection(directions[0]);
                }
                if (1 < directions.length) {
                    secondTextDirection =
                        TextDirection.getTextDirection(directions[1]);
                }
            }
        }
        visible = headerMap.getIsMatch(HeaderEnum.VISIBLE, Message.TRUE);
        events = headerMap.getString(HeaderEnum.EVENTS);

        return this;
    }
}
