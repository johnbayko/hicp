package hicp_client.gui.selection;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp_client.gui.Item;
import hicp_client.text.TextLibrary;

public class SelectionSource {
    private static final Logger LOGGER =
        Logger.getLogger( SelectionSource.class.getName() );

    public static final Pattern LINE_SPLITTER =
        Pattern.compile("\r\n", Pattern.LITERAL);

    public static List<ItemInfo> itemList(final String itemsStr) {
        final String[] itemsList =
            SelectionSource.LINE_SPLITTER.split(itemsStr);

        final List<ItemInfo> itemList = new ArrayList<>(itemsList.length);
        for (final String itemStr : itemsList) {
            try {
                itemList.add(new ItemInfo(itemStr));
            } catch (ParseException ex) {
                // Just skip.
            }
        }
        return itemList;
    }

    public static Item newItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final PresentationEnum presentation =
            PresentationEnum.getEnum(addCmd.presentation);

        final ModeEnum mode = ModeEnum.getEnum(addCmd.mode);

        switch (presentation) {
          case SCROLL:
            return new ScrollItem(addCmd, textLibrary, messageExchange);
          case TOGGLE:
            switch (mode) {
              case SINGLE:
                // TODO Think these will be the same. If so, remove mode check.
                return new ToggleItem(addCmd, textLibrary, messageExchange);
              case MULTIPLE:
                return new ToggleItem(addCmd, textLibrary, messageExchange);
            }
            break;
          case DROPDOWN:
            return new DropdownItem(addCmd, textLibrary, messageExchange);
        }
        return null;  // Should never get here, but you know...
    }
}

