package hicp_client.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.CommandInfo;
import hicp.message.command.ItemInfo;
import hicp.message.command.GUIInfo;
import hicp_client.gui.*;
import hicp_client.gui.selection.SelectionSource;
import hicp_client.text.TextItemAdapter;
import hicp_client.text.TextItemAdapterListener;
import hicp_client.text.TextLibrary;

public class ItemSource {
    private static final Logger LOGGER =
        Logger.getLogger( ItemSource.class.getName() );

    public static Item newItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final CommandInfo commandInfo = addCmd.getCommandInfo();
        final ItemInfo itemInfo = commandInfo.getItemInfo();
        final GUIInfo guiInfo = itemInfo.getGUIInfo();

        // Make sure it's a real integer - not used.
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
            guiItem = new ButtonItem(addCmd, messageExchange);
            break;
          case LABEL:
            guiItem = new LabelItem(addCmd);
        break;
          case PANEL:
            guiItem = new PanelItem(addCmd);
            break;
          case SELECTION:
            guiItem =
                SelectionSource.newItem(addCmd, textLibrary, messageExchange);
            break;
          case TEXTFIELD:
            guiItem = new TextFieldItem(addCmd, messageExchange);
            break;
          case WINDOW:
            guiItem = new WindowItem(addCmd, messageExchange);
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
        guiItem.add(addCmd);

        return guiItem;
    }

    public static void disposeItem(final Item guiItem) {
        if (TextItemAdapterListener.class.isInstance(guiItem)) {
            ((TextItemAdapterListener)guiItem).removeAdapter();
        }
        guiItem.dispose();
    }
}
