package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class AddModifyRemove
    extends Command
{
    private static final Logger LOGGER =
        Logger.getLogger( AddModifyRemove.class.getName() );

    public final static String CATEGORY = "category";
    public final static String ID = "id";

    public final static String GUI = "gui";
    public final static String TEXT = "text";

    public String category = null;
    public String id = null;

    public AddModifyRemove(String name) {
        super(name);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        for (final HeaderEnum h : headerMap.keySet()) {
            final HICPHeader v = headerMap.get(h);
            switch (h) {
              case CATEGORY:
                category = v.value.getString();
                break;
              case ID:
                id = v.value.getString();
                break;
            }
        }
        return this;
    }
}
