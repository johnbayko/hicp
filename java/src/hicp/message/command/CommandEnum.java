package hicp.message.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.message.Message;

public enum CommandEnum
{
    AUTHENTICATE(1, "authenticate") {
        public Message newMessage(
            final String newMessageName, final int newMessageId
        ) {
            return new Authenticate(newMessageName, newMessageId);
        }
    },

    ADD(2, "add") {
        public Message newMessage(
            final String newMessageName, final int newMessageId
        ) {
            return new Add(newMessageName, newMessageId);
        }
    },

    MODIFY(3, "modify") {
        public Message newMessage(
            final String newMessageName, final int newMessageId
        ) {
            return new Modify(newMessageName, newMessageId);
        }
    },

    REMOVE(4, "remove") {
        public Message newMessage(
            final String newMessageName, final int newMessageId
        ) {
            return new Remove(newMessageName, newMessageId);
        }
    },

    DISCONNECT(5, "disconnect") {
        public Message newMessage(
            final String newMessageName, final int newMessageId
        ) {
            return new Disconnect(newMessageName, newMessageId);
        }
    };


    public final int messageId;
    public final String messageName;

    public abstract Message newMessage(
        final String newMessageName, final int newMessageId
    );

    private static final Map<String, CommandEnum> messageNameMap =
        Arrays.stream(CommandEnum.values())
            .collect(
                Collectors.toMap(
                    commandEnum -> commandEnum.messageName,
                    commandEnum -> commandEnum
                )
            );

    CommandEnum(
        final int newMessageId,
        final String newMessageName
    ) {
        messageId = newMessageId;
        messageName = newMessageName;
    }

    public static Message getMessage(String messageName) {
        final var m = messageNameMap.get(messageName);
        if (null == m) {
            return null;
        }
        return m.newMessage(m.messageName, m.messageId);
    }
}

