package hicp_client.gui.selection;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp_client.gui.Item;
import hicp_client.text.TextLibrary;

public class SelectionSource {
    private static final Logger LOGGER =
        Logger.getLogger( SelectionSource.class.getName() );

    public static Item newItem(
        final Message m,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final var commandInfo = m.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        switch (guiSelectionInfo.presentation) {
          case SCROLL:
            return new ScrollItem(m, textLibrary, messageExchange);
          case TOGGLE:
            return new ToggleItem(m, textLibrary, messageExchange);
          case DROPDOWN:
            return new DropdownItem(m, textLibrary, messageExchange);
        }
        return null;  // Should never get here, but you know...
    }
}

