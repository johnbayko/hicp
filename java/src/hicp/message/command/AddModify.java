package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.TextDirection;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class AddModify
    extends ItemCommand
{
    private static final Logger LOGGER =
        Logger.getLogger( AddModify.class.getName() );

    public final static String ITEMS = "items";
    public final static String POSITION = "position";
    public final static String SELECTED = "selected";
    public final static String SIZE = "size";
    public final static String TEXT_DIRECTION = "text-direction";

    // Values for events header.
    public final static String ENABLED = "enabled";
    public final static String DISABLED = "disabled";
    public final static String SERVER = "server";

    public String items = null;
    public String[] selected = null;

    public TextDirection firstTextDirection = null;
    public TextDirection secondTextDirection = null;

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

        items = headerMap.getString(HeaderEnum.ITEMS);
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

        return this;
    }
}
