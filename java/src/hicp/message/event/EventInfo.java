package hicp.message.event;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.Message;
import hicp.message.HeaderEnum;

public class EventInfo {
    private static final Logger LOGGER =
        Logger.getLogger( EventInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public static enum Event
    {
        AUTHENTICATE("authenticate"),
        CHANGED("changed"),
        CLOSE("close"),
        CLICK("click"),
        CONNECT("connect");

        public final String name;

        private static final Map<String, Event> messageNameMap =
            Arrays.stream(Event.values())
                .collect(
                    Collectors.toMap(
                        eventEnum -> eventEnum.name,
                        eventEnum -> eventEnum
                    )
                );

        Event(final String newMessageName) {
            name = newMessageName;
        }

        public static Event getEnum(String name) {
            return messageNameMap.get(name);
        }
    }

    public Event event;

    private ItemInfo _itemInfo = null;
    private AuthenticateInfo _authenticateInfo = null;
    private ConnectInfo _connectInfo = null;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;

    public EventInfo() {
    } 

    public EventInfo(final Event newEvent) {
        event = newEvent;
    }

    public EventInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        event =
            Event.getEnum(
                headerMap.getString(HeaderEnum.EVENT)
            );
    }

    public EventInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        if (null != event) {
            headerMap.putString(HeaderEnum.EVENT, event.name);
        }
        if (null != _itemInfo) {
            _itemInfo.updateHeaderMap(headerMap);
        }
        if (null != _authenticateInfo) {
            _authenticateInfo.updateHeaderMap(headerMap);
        }
        if (null != _connectInfo) {
            _connectInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public ItemInfo getItemInfo() {
        if (null == _itemInfo) {
            _itemInfo = new ItemInfo(_headerMap);
        }
        return _itemInfo;
    }

    public AuthenticateInfo getAuthenticateInfo() {
        if (null == _authenticateInfo) {
            _authenticateInfo = new AuthenticateInfo(_headerMap);
        }
        return _authenticateInfo;
    }

    public ConnectInfo getConnectInfo() {
        if (null == _connectInfo) {
            _connectInfo = new ConnectInfo(_headerMap);
        }
        return _connectInfo;
    }
}
