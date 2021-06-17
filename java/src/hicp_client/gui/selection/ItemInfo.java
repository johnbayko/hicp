package hicp_client.gui.selection;

import java.text.ParseException;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import hicp.message.Message;

public class ItemInfo {
    public final String id;
    public final String textId;
    public final EventsEnum events;

    public ItemInfo(final String itemStr)
        throws ParseException
    {
        // <id>:<type-value list>
        final String[] idInfoSplit =
            Message.COLON_SPLITTER.split(itemStr);

        // Needs at least 2 results.
        if (idInfoSplit.length < 2) {
            throw new ParseException(
                    "Expected <id>:<item info>, missing separator ':'", 0
                );
        }
        id = idInfoSplit[Message.ID_IDX];

        final String[] infoList =
            Message.COMMA_SPLITTER.split(
                idInfoSplit[Message.INFO_IDX]
            );

        // Needs at least 1 result.
        if (infoList.length < 1) {
            throw new ParseException("No selection item info found.", 0);
        }

        String textIdStr = null;
        String eventsStr = null;
        for (final String typeValueStr : infoList) {
            final String[] typeValueSplit =
                Message.KEY_VALUE_SPLITTER.split(typeValueStr);

            if (typeValueSplit.length < 2) {
                // Just skip this one.
                continue;
            }
            final String type = typeValueSplit[Message.KEY_IDX];
            final String value = typeValueSplit[Message.VALUE_IDX];

            if ("text".equals(type) && (null == textIdStr)) {
                textIdStr = value;
            } else if ("events".equals(type) && (null == eventsStr)) {
                eventsStr = value;
            }
        }

        if (null != textIdStr) {
            textId = textIdStr;
        } else {
            throw new ParseException(
                "Expected at text ID in type info, found none.", 0
                );
        }
        events = EventsEnum.getEnum(eventsStr);  // Handles null.
    }
}

