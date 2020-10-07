package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HICPHeader;
import hicp.HICPReader;
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

    public AddModifyRemove(String name, int id) {
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
        // Extract recognized fields.
        if (CATEGORY.equals(hicpHeader.name)) {
            category = hicpHeader.value.getString();
            return true;
        } else if (ID.equals(hicpHeader.name)) {
            id = hicpHeader.value.getString();
            return true;
        }
        return false;
    }

    public void clear() {
        category = null;
        id = null;
    }
}
