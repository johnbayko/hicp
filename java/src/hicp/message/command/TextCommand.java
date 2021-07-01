package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class TextCommand
    extends ItemCommand
{
    private String _text = null;

    public TextCommand(final String name) {
        super(name);
    }

    public TextCommand(
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

    public TextCommand addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);

        for (final HeaderEnum h : headerMap.keySet()) {
            final HICPHeader v = headerMap.get(h);
            switch (h) {
              case TEXT:
                _text = v.value.getString();
                break;
            }
        }
        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        Message.addHeaderString(headerMap, HeaderEnum.TEXT, _text);

        return headerMap;
    }

    public String getText() {
        return _text;
    }
}
