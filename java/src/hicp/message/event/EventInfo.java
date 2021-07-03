package hicp.message.event;

import java.util.Map;

import hicp.HICPHeader;
import hicp.HICPHeaderValue;
import hicp.message.HeaderEnum;

public class EventInfo {
    public final EventEnum event;

    public EventInfo(final Map<HeaderEnum, HICPHeader> headerMap) {
        event =
            EventEnum.getEnum(
                headerMap.get(HeaderEnum.EVENT).value.getString()
            );
    }

    public EventInfo updateHeaderMap(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        if (null != event) {
            final HICPHeader h =
                new HICPHeader(
                    HeaderEnum.EVENT,
                    new HICPHeaderValue(event.messageName)
                );
            headerMap.put(HeaderEnum.EVENT, h);
        }
        return this;
    }
}

