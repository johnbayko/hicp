package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum CommandEnum
{
    AUTHENTICATE("authenticate") {
        public Command newCommand(final String newMessageName) {
            return new Authenticate(newMessageName);
        }
    },

    ADD("add") {
        public Command newCommand(final String newMessageName) {
            return new Add(newMessageName);
        }
    },

    MODIFY("modify") {
        public Command newCommand(final String newMessageName) {
            return new Modify(newMessageName);
        }
    },

    REMOVE("remove") {
        public Command newCommand(final String newMessageName) {
            return new Remove(newMessageName);
        }
    },

    DISCONNECT("disconnect") {
        public Command newCommand(final String newMessageName) {
            return new Disconnect(newMessageName);
        }
    };


    public final String messageName;

    private static final Map<String, CommandEnum> messageNameMap =
        Arrays.stream(CommandEnum.values())
            .collect(
                Collectors.toMap(
                    commandEnum -> commandEnum.messageName,
                    commandEnum -> commandEnum
                )
            );


    CommandEnum(final String newMessageName) {
        messageName = newMessageName;
    }


    public abstract Command newCommand(final String newMessageName);

    public Command newCommand() {
        return newCommand(messageName);
    }


    public static CommandEnum getEnum(String messageName) {
        return messageNameMap.get(messageName);
    }
}
