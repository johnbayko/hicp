package hicp.message.event;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class EventInfo {
    public EventEnum event;

    public EventInfo() {
    } 

    public EventInfo(final HeaderMap headerMap) {
        event =
            EventEnum.getEnum(
                headerMap.getString(HeaderEnum.EVENT)
            );
    }

    public EventInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        if (null != event) {
            headerMap.putString(HeaderEnum.EVENT, event.name);
        }
        return this;
    }
}

