package hicp.message.command;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class CommandInfo {
    // TODO Move command enum into here.

    public final CommandEnum command;

    private AuthenticateInfo _authenticateInfo;
    private ItemInfo _itemInfo;

    private final HeaderMap _headerMap;

    public CommandInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        command =
            CommandEnum.getEnum(
                headerMap.getString(HeaderEnum.COMMAND)
            );
    }

    public CommandInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.COMMAND, command.name);
        if (null != _authenticateInfo) {
            _authenticateInfo.updateHeaderMap(headerMap);
        }
        if (null != _itemInfo) {
            _itemInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public AuthenticateInfo getAuthenticateInfo() {
        if (null == _headerMap) {
            return null;
        }
        if (null == _authenticateInfo) {
            _authenticateInfo = new AuthenticateInfo(_headerMap);
        }
        return _authenticateInfo;
    }

    public CommandInfo setAuthenticateInfo(final AuthenticateInfo i) {
        _authenticateInfo = i;
        return this;
    }

    public ItemInfo getItemInfo() {
        if (null == _headerMap) {
            return null;
        }
        if (null == _itemInfo) {
            _itemInfo = new ItemInfo(_headerMap);
        }
        return _itemInfo;
    }

    public CommandInfo setItemInfo(final ItemInfo i) {
        _itemInfo = i;
        return this;
    }

}
