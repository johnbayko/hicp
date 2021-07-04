package hicp.message.command;

import java.util.Map;
import java.util.Set;

import hicp.HeaderMap;
import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class AuthenticateInfo {
    private String _method;
    private Set<String> _allMethods;

    public String password;

    public AuthenticateInfo(final HeaderMap headerMap) {
        _method = headerMap.getString(HeaderEnum.METHOD);
        _allMethods = HeaderMap.makeStringSet(_method);

        password = headerMap.getString(HeaderEnum.PASSWORD);
    }

    public AuthenticateInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.METHOD, _method);
        headerMap.putString(HeaderEnum.PASSWORD, password);

        return this;
    }

    public boolean hasMethod(final String method) {
        return _allMethods.contains(method);
    }
}
