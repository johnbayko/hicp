package hicp.message;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HICPReader;

public abstract class Message {
    private static final Logger LOGGER =
        Logger.getLogger( Message.class.getName() );

    protected final static String TRUE = "true";
    protected final static String FALSE = "false";

    protected final String _name;

    public abstract void write(Writer out) throws IOException;

    public abstract void read(HICPReader in) throws IOException;

    public Message(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    protected void writeHeader(Writer out, String name, String value)
        throws IOException
    {
        out.write(name);
        if (-1 == value.indexOf("\r\n")) {
            // Value can be sent on a single line.
            out.write(": ");
            out.write(value);
            out.write("\r\n");
        } else {
            // Value has a CR LF, then send in multiple lines.
            out.write(":: boundary=\r\n--\r\n");
            // Escape each occurrence by splitting string with "\r\n--",
            // write out each with ESC prior to "\r\n--".
            final String[] value_array = value.split("\r\n--", -1);
            for (int value_idx = 0;
                value_idx < value_array.length;
                value_idx++
            ) {
                out.write(value_array[value_idx]);
                out.write("\033\r\n--");
            }
            // Write out terminator sequence and extra "\r\n" as block
            // terminator.
            out.write("\r\n--\r\n");
        }
    }

    protected void writeEndOfMessage(Writer out)
        throws IOException
    {
        out.write("\r\n");
        out.flush();
    }

    // Utility
    protected void log(Exception ex) {
        LOGGER.log(Level.WARNING, ex.toString());
    } 

    protected void log(String msg) {
        LOGGER.log(Level.FINE, msg);
    }

}
