package hicp.message.command;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

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

    public Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        // TODO make independent from Modify.
        super.addHeaders(headerMap);

        for (final HeaderEnum h : headerMap.keySet()) {
            final HICPHeader v = headerMap.get(h);
            switch (h) {
              case HEIGHT:
                height = v.value.getString();
                break;
              case MODE:
                mode = v.value.getString();
                break;
              case PRESENTATION:
                presentation = v.value.getString();
                break;
              case WIDTH:
                width = v.value.getString();
                break;
            }
        }
        return this;
    }
}
