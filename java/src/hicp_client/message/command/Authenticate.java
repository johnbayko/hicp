package hicp_client.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class Authenticate {
    public final String method;
    public final String password;

    private final Set<String> _allMethods;

    public Authenticate(final Message m) {
        method = m.getHeaderString(HeaderEnum.METHOD);
        _allMethods = m.getStringSet(method);

        password = m.getHeaderString(HeaderEnum.PASSWORD);
    }

    public boolean hasMethod(final String method) {
        return _allMethods.contains(method);
    }
}
