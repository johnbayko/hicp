package hicp.message.command;

import java.io.IOException;
import java.io.Writer;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class TextCommand
    extends ItemCommand
{
    private String _text = null;

    public TextCommand(final String name) {
        super(name);
    }

    public TextCommand(
        final String name,
        final HeaderMap headerMap
    ) {
        super(name);

        addHeaders(headerMap);
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public TextCommand addHeaders(
        final HeaderMap headerMap
    ) {
        super.addHeaders(headerMap);

        _text = headerMap.getString(HeaderEnum.TEXT);

        return this;
    }

    public HeaderMap getHeaders() {
        final HeaderMap headerMap = super.getHeaders();

        headerMap.putString(HeaderEnum.TEXT, _text);

        return headerMap;
    }

    public String getText() {
        return _text;
    }
}
