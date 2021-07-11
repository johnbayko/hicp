package hicp.message.command;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class Add
    extends AddModify
{
    private static final Logger LOGGER =
        Logger.getLogger( Add.class.getName() );

    // TODO fill rest of these from modify - make them independent.

    public Add(final String name) {
        super(name);
    }

    public Add addHeaders(
        final HeaderMap headerMap
    ) {
        // TODO make independent from Modify.
        super.addHeaders(headerMap);

        return this;
    }
}
