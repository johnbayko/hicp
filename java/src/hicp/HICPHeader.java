package hicp;

import hicp.message.HeaderEnum;

public class HICPHeader
{
    public final String name;
    public final HICPHeaderValue value;

    public HICPHeader(
        final HeaderEnum headerEnum,
        final HICPHeaderValue newValue
    ) {
        this(headerEnum.name, newValue);
    }

    public HICPHeader(final String newName, final HICPHeaderValue newValue) {
        name = newName;
        value = newValue;
    }
}
