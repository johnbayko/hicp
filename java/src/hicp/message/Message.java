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
//LOGGER.log(Level.FINE, " " + );  // debug

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

    protected CommandInfo _commandInfo = null;
    protected EventInfo _eventInfo = null;

    public static final HeaderMap DEFAULT_HEADER_MAP = new HeaderMap();
    protected HeaderMap _headerMap = DEFAULT_HEADER_MAP;


    public Message(final CommandInfo.Command command) {
        _commandInfo = new CommandInfo(command);

        // To make isCommand() work.
        _headerMap =
            new HeaderMap()
            .putString(HeaderEnum.COMMAND, command.name);
    }

    public Message(final EventInfo.Event event) {
        _eventInfo = new EventInfo(event);

        // To make isEvent() work.
        _headerMap =
            new HeaderMap()
            .putString(HeaderEnum.EVENT, event.name);
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
        final HeaderMap headerMap = new HeaderMap();

        if (null != _commandInfo) {
            _commandInfo.updateHeaderMap(headerMap);
        }
        if (null != _eventInfo) {
            _eventInfo.updateHeaderMap(headerMap);
        }
        return headerMap;
    }

    public boolean isCommand() {
        if (null == _headerMap.getHeader(HeaderEnum.COMMAND)) {
            return false;
        }
        return true;
    }

    public CommandInfo getCommandInfo() {
        if (null == _commandInfo) {
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
        if (null == _eventInfo) {
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
}
