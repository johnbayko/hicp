package hicp.message;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;

public abstract class Message {
    private static final Logger LOGGER =
        Logger.getLogger( Message.class.getName() );

    // Useful regexes for parsing header strings.
    public static final Pattern LINE_SPLITTER =
        Pattern.compile("\r\n", Pattern.LITERAL);

    public static final Pattern COLON_SPLITTER =
        Pattern.compile("\\s*:\\s*");
    public static final int ID_IDX = 0;
    public static final int INFO_IDX = 1;

    public static final Pattern COMMA_SPLITTER =
        Pattern.compile("\\s*,\\s*");

    public static final Pattern KEY_VALUE_SPLITTER =
        Pattern.compile("\\s*=\\s*");
    public static final int KEY_IDX = 0;
    public static final int VALUE_IDX = 1;

    public final static String TRUE = "true";
    public final static String FALSE = "false";

    protected final String _name;

    public abstract void write(Writer out) throws IOException;

    /**
        Add header values to appropriate message structures, such as
        strings, integers, internal calsses, lists, etc., for use by other code
        without needing to know header value formats.
     */
    public abstract Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    );

    public Message(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    // TODO should be HICPWriter for this.
    // Implement a getHeaders() method to pass to writer.
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
            final String esc_value = value.replace("\r\n--", "\033\r\n--");
            out.write(esc_value);

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
