package hicp.message;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hicp.HICPHeader;
import hicp.HICPReader;
import hicp.message.HeaderEnum;

public abstract class Message {
    private static final Logger LOGGER =
        Logger.getLogger( Message.class.getName() );

    // TODO these might be more useful in hicp_client package where they're
    // actually used.
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

    protected Map<HeaderEnum, HICPHeader> _headerMap;

    public abstract void write(Writer out) throws IOException;

    public Message( String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    /**
        Add header values to appropriate message structures, such as
        strings, integers, internal calsses, lists, etc., for use by other code
        without needing to know header value formats.
     */
    public Message addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        _headerMap = headerMap;
        return this;
    }

    /**
        Create a map of headers containing the values of this message.
     */
    public Map<HeaderEnum, HICPHeader> getHeaders() {
        return new HashMap<>();
    }


    // General header access and string parsing utilities.

    public HICPHeader getHeader(final HeaderEnum e) {
        if (null != _headerMap) {
            return _headerMap.get(e);
        }
        return null;
    }

    public String getHeaderString(final HeaderEnum e) {
        final HICPHeader h = getHeader(e);
        if (null == h) {
            return null;
        }
        return h.value.getString();
    }

    /**
        Convert comma separated string to Set.
     */
    public static Set<String> getStringSet(final String s) {
        // split("") will create a 1 element array of [""], treat that as
        // empty.
        if ((null == s) || "".equals(s)) {
            // Empty set.
            return Set.of();
        }
        final String[] sSplit = COMMA_SPLITTER.split(s);
        return Set.of(sSplit);
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
