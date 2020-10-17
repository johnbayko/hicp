package hicp.message.event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum EventEnum
{
    CONNECT("connect") {
        public Event newEvent(final String newMessageName) {
            return new Connect(newMessageName);
        }
    },

    AUTHENTICATE("authenticate") {
        public Event newEvent(final String newMessageName) {
            return new Authenticate(newMessageName);
        }
    },

    CHANGED("changed") {
        public Event newEvent(final String newMessageName) {
            return new Changed(newMessageName);
        }
    },

    CLOSE("close") {
        public Event newEvent(final String newMessageName) {
            return new Close(newMessageName);
        }
    },

    CLICK("click") {
        public Event newEvent(final String newMessageName) {
            return new Click(newMessageName);
        }
    };


    public final String messageName;

    private static final Map<String, EventEnum> messageNameMap =
        Arrays.stream(EventEnum.values())
            .collect(
                Collectors.toMap(
                    eventEnum -> eventEnum.messageName,
                    eventEnum -> eventEnum
                )
            );


    EventEnum(final String newMessageName) {
        messageName = newMessageName;
    }


    public abstract Event newEvent(final String newMessageName);

    public Event newEvent() {
        return newEvent(messageName);
    }


    public static EventEnum getEnum(String messageName) {
        return messageNameMap.get(messageName);
    }
}

