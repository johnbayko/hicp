package hicp.message;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.command.CommandInfo;
import hicp.message.event.EventInfo;

public class Message {
    private static final Logger LOGGER =
        Logger.getLogger( Message.class.getName() );

    // TODO maybe HeaderEnum should go here?

    // Useful regexes for parsing header strings.
    public static final String EOL = "\r\n";
    public static final Pattern LINE_SPLITTER =
        Pattern.compile(EOL, Pattern.LITERAL);

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

    public static final CommandInfo DEFAULT_COMMAND_INFO = new CommandInfo();
    public static final EventInfo DEFAULT_EVENT_INFO = new EventInfo();

    protected CommandInfo _commandInfo = DEFAULT_COMMAND_INFO;
    protected EventInfo _eventInfo = DEFAULT_EVENT_INFO;

    public static final HeaderMap DEFAULT_HEADER_MAP = new HeaderMap();
    protected HeaderMap _headerMap = DEFAULT_HEADER_MAP;

    public void write(Writer out)
        throws IOException
    {
        // No fields to write.
    }


    public Message() {
    }

    public Message(final HeaderMap headerMap) {
        addHeaders(headerMap);
    }

    /**
        Add header values to appropriate message structures, such as
        strings, integers, internal calsses, lists, etc., for use by other code
        without needing to know header value formats.
     */
    public Message addHeaders(
        final HeaderMap headerMap
    ) {
        _headerMap = headerMap;
        return this;
    }

    /**
        Create a map of headers containing the values of this message.
     */
    public HeaderMap getHeaders() {
        return new HeaderMap();
    }

    public boolean isCommand() {
        if (null == _headerMap.getHeader(HeaderEnum.COMMAND)) {
            return false;
        }
        return true;
    }

    public CommandInfo getCommandInfo() {
        if (DEFAULT_COMMAND_INFO == _commandInfo) {
            if (isCommand()) {
                _commandInfo = new CommandInfo(_headerMap);
            }
        }
        return _commandInfo;
    }

    public boolean isEvent() {
        if (null == _headerMap.getHeader(HeaderEnum.EVENT)) {
            return false;
        }
        return true;
    }

    public EventInfo getEventInfo() {
        if (DEFAULT_EVENT_INFO == _eventInfo) {
            if (isEvent()) {
                _eventInfo = new EventInfo(_headerMap);
            }
        }
        return _eventInfo;
    }


    // General header access and string parsing utilities.
    /*
        Regex split() will split "" into [""], which is not wanted, so return
        [] in that case instead.
     */
    public static String[] splitWith(
        final Pattern splitter,
        final String strToSplit
    ) {
        return discardEmpty(splitter.split(strToSplit));
    }

    /*
        Regex split() will split "" into [""], which is not wanted, so return
        [] in that case instead.
     */
    public static String[] splitWith(
        final Pattern splitter,
        final String strToSplit,
        final int limit
    ) {
        return discardEmpty(splitter.split(strToSplit, limit));
    }

    /*
        Discard [""] by returning [] instead.
     */
    public static String[] discardEmpty(final String[] splitArray) {
        // Check for [""].
        if ((1 == splitArray.length) && "".equals(splitArray[0])) {
            return new String[0];
        }
        return splitArray;
    }


    // TODO should be HICPWriter for this.
    // Implement a getHeaders() method to pass to writer.
    protected void writeHeader(Writer out, String name, String value)
        throws IOException
    {
        out.write(name);
        if (-1 == value.indexOf(EOL)) {
            // Value can be sent on a single line.
            out.write(": ");
            out.write(value);
            out.write(EOL);
        } else {
            // Value has a CR LF, then send in multiple lines.
            out.write(":: boundary=\r\n--\r\n");

            // Escape boundary ("\r\n--"), and single ESC.
            final String esc_value =
                value.replace("\033", "\033\033");
                value.replace("\r\n--", "\033\r\n--");
            out.write(esc_value);

            // Write out terminator sequence and extra "\r\n" as block
            // terminator.
            out.write("\r\n--\r\n");
        }
    }

    protected void writeEndOfMessage(Writer out)
        throws IOException
    {
        out.write(EOL);
        out.flush();
    }
}
