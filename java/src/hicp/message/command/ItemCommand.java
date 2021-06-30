package hicp.message.command;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import hicp.HICPHeader;
import hicp.message.HeaderEnum;

public class ItemCommand
    extends Command
{
    private final ItemInfo _itemInfo;

    public ItemCommand(final String name) {
        super(name);
        _itemInfo = new ItemInfo();
    }

    public ItemCommand(
        final String name,
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super(name);

        _itemInfo = new ItemInfo();

        addHeaders(headerMap);
    }

    public ItemInfo getItemInfo() {
        return _itemInfo;
    }

    public void write(Writer out)
        throws IOException
    {
    }

    public ItemCommand addHeaders(
        final Map<HeaderEnum, HICPHeader> headerMap
    ) {
        super.addHeaders(headerMap);
        
        _itemInfo.addHeaders(headerMap);

        return this;
    }

    public Map<HeaderEnum, HICPHeader> getHeaders() {
        final Map<HeaderEnum, HICPHeader> headerMap = super.getHeaders();

        _itemInfo.getHeaders(headerMap);

        return headerMap;
    }
}
