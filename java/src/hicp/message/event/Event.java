package hicp.message.event;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPHeaderValue;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public abstract class Event
    extends Message
{
    public static String EVENT = "event";

    private EventEnum _event = null;

    public Event(String name) {
        super(name);
    }

    public Event(String name, final EventEnum event) {
        super(name);
        _event = event;
    }

    public Event addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);

        // getEnum() handles null, may return null.
        _event =
            EventEnum.getEnum(
                getHeaderString(HeaderEnum.EVENT)
            );

        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        if (null != _event) {
            final HICPHeader h =
                new HICPHeader(
                    HeaderEnum.EVENT,
                    new HICPHeaderValue(_event.messageName)
                );
            headerMap.put(HeaderEnum.COMMAND, h);
        }
        return headerMap;
    }

    public EventEnum getEvent() {
        return _event;
    }

    public void write(Writer out)
        throws IOException
    {
        writeHeader(out, EVENT, getName());

        out.flush();
    }
}

