package hicp_client.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.CommandInfo;
import hicp_client.gui.*;
import hicp_client.gui.selection.SelectionSource;
import hicp_client.text.TextItemAdapter;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextLibrary;

public class ItemSource {
    private static final Logger LOGGER =
        Logger.getLogger( ItemSource.class.getName() );

    public static Item newItem(
        final CommandInfo commandInfo,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();

        // Make sure it's a real integer - integer value not used.
        try {
            Integer.parseInt(itemInfo.id);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINE, "ID field not an integer.");

            // Not an integer ID, ignore message.
            return null;
        }
        final Item guiItem;

        if (null == guiInfo.component) {
            LOGGER.log(Level.FINE, "Add has no component");
            guiItem = null;
        } else switch (guiInfo.component) {
          case BUTTON:
            guiItem = new ButtonItem(commandInfo, messageExchange);
            break;
          case LABEL:
            guiItem = new LabelItem(commandInfo);
            break;
          case PANEL:
            guiItem = new PanelItem(commandInfo);
            break;
          case SELECTION:
            guiItem =
                SelectionSource.newItem(commandInfo, textLibrary, messageExchange);
            break;
          case TEXTFIELD:
            guiItem = new TextFieldItem(commandInfo, messageExchange);
            break;
          case WINDOW:
            guiItem = new WindowItem(commandInfo, messageExchange);
            break;
          default:
            // Unrecognized category.
            LOGGER.log(
                Level.FINE,
                "Add unrecognized component: " + guiInfo.component.name
            );
            guiItem = null;
            break;
        }
        if (null == guiItem) {
            return null;
        }
        if (TextItemAdapterListener.class.isInstance(guiItem)) {
            ((TextItemAdapterListener)guiItem)
                .setAdapter(new TextItemAdapter(textLibrary));
        }
        guiItem.add(commandInfo);

        return guiItem;
    }

    public static void disposeItem(final Item guiItem) {
        if (TextItemAdapterListener.class.isInstance(guiItem)) {
            ((TextItemAdapterListener)guiItem).removeAdapter();
        }
        guiItem.dispose();
    }
}
