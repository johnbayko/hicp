package hicp.message.command;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class Add
    extends AddModify
{
    private static final Logger LOGGER =
        Logger.getLogger( Add.class.getName() );

    public final static String HEIGHT = "height";
    public final static String MODE = "mode";
    public final static String PRESENTATION = "presentation";
    public final static String WIDTH = "width";

    public String height = null;
    public String mode = null;
    public String presentation = null;
    public String width = null;
    // TODO fill rest of these from modify - make them independent.

    public Add(final String name) {
        super(name);
    }

    public Add addHeaders(
        final HeaderMap headerMap
    ) {
        // TODO make independent from Modify.
        super.addHeaders(headerMap);

        height = headerMap.getString(HeaderEnum.HEIGHT);
        mode = headerMap.getString(HeaderEnum.MODE);
        presentation = headerMap.getString(HeaderEnum.PRESENTATION);
        width = headerMap.getString(HeaderEnum.WIDTH);

        return this;
    }
}
