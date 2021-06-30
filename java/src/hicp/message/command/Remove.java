package hicp.message.command;

import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

public class Remove
    extends ItemCommand
{
    public Remove(final String name) {
        super(name);
    }

    public Remove(
        final String name,
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super(name);
        addHeaders(headerMap);
    }

    public Remove addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);
        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        return headerMap;
    }
}
