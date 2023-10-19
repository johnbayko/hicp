package hicp_client.gui.selection;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.CommandInfo;
import hicp_client.gui.Item;
import hicp_client.text.TextLibrary;

public class SelectionSource {
    private static final Logger LOGGER =
        Logger.getLogger( SelectionSource.class.getName() );

    public static Item newItem(
        final CommandInfo commandInfo,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var guiSelectionInfo = guiInfo.getGUISelectionInfo();

        switch (guiSelectionInfo.presentation) {
          case SCROLL:
            return new ScrollItem(commandInfo, textLibrary, messageExchange);
          case TOGGLE:
            return new ToggleItem(commandInfo, textLibrary, messageExchange);
          case DROPDOWN:
            return new DropdownItem(commandInfo, textLibrary, messageExchange);
        }
        return null;  // Should never get here, but you know...
    }
}

