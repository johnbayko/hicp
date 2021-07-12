package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class CommandInfo {
    public static enum Command
    {
        AUTHENTICATE("authenticate"),
        ADD("add"),
        MODIFY("modify"),
        REMOVE("remove"),
        DISCONNECT("disconnect");

        public final String name;

        private static final Map<String, Command> enumMap =
            Arrays.stream(Command.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        Command(final String forName) {
            name = forName;
        }

        public static Command getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public Command command;

    public static final AuthenticateInfo DEFAULT_AUTHENTICATE_INFO =
        new AuthenticateInfo();
    public static final ItemInfo DEFAULT_ITEM_INFO =
        new ItemInfo();

    private AuthenticateInfo _authenticateInfo = DEFAULT_AUTHENTICATE_INFO;
    private ItemInfo _itemInfo = DEFAULT_ITEM_INFO;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;

    public CommandInfo() {
    }

    public CommandInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        command =
            Command.getEnum(
                headerMap.getString(HeaderEnum.COMMAND)
            );
    }

    public CommandInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.COMMAND, command.name);

        if (DEFAULT_AUTHENTICATE_INFO != _authenticateInfo) {
            _authenticateInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_ITEM_INFO != _itemInfo) {
            _itemInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public AuthenticateInfo getAuthenticateInfo() {
        if (DEFAULT_AUTHENTICATE_INFO == _authenticateInfo) {
            _authenticateInfo = new AuthenticateInfo(_headerMap);
        }
        return _authenticateInfo;
    }

    public CommandInfo setAuthenticateInfo(final AuthenticateInfo i) {
        _authenticateInfo = i;
        return this;
    }

    public ItemInfo getItemInfo() {
        if (DEFAULT_ITEM_INFO == _itemInfo) {
            _itemInfo = new ItemInfo(_headerMap);
        }
        return _itemInfo;
    }

    public CommandInfo setItemInfo(final ItemInfo i) {
        _itemInfo = i;
        return this;
    }
}
