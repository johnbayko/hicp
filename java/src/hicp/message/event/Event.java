package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class Event
    extends Message
{
    private static final Logger LOGGER =
        Logger.getLogger( Event.class.getName() );

    private EventInfo.Event _event = null;

    public Event(final EventInfo.Event event) {
        _event = event;
    }

    public Event addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);

        // getEnum() handles null, may return null.
        _event =
            EventInfo.Event.getEnum(
                headerMap.getString(HeaderEnum.EVENT)
            );

        return this;
    }

    public HeaderMap getHeaders() {
        final HeaderMap headerMap = super.getHeaders();

        if (null != _event) {
            headerMap.putString(HeaderEnum.EVENT, _event.name);
        }
        return headerMap;
    }

    public EventInfo.Event getEvent() {
        return _event;
    }

    public void write(Writer out)
        throws IOException
    {
        writeHeader(out, HeaderEnum.EVENT.name, _event.name);

        out.flush();
    }
}

