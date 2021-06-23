package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

public class Authenticate
    extends Command
{
    private String _method = null;
    private String _password = null;

    private Set<String> _allMethods = null;

    public Authenticate(final String name) {
        super(name);
    }

    public Authenticate(
        final String name,
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super(name);
        addHeaders(headerMap);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public Authenticate addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);

        _method = getHeaderString(HeaderEnum.METHOD);
        _allMethods = getStringSet(_method);

        _password = getHeaderString(HeaderEnum.PASSWORD);

        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        addHeaderString(headerMap, HeaderEnum.METHOD, _method);
        addHeaderString(headerMap, HeaderEnum.PASSWORD, _password);

        return headerMap;
    }

    public boolean hasMethod(final String method) {
        return _allMethods.contains(method);
    }
}
