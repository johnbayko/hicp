package hicp_client.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.Add;
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
        try {
            // Make sure it's a real integer - not used.
            final int id = Integer.parseInt(addCmd.getId());
            final Item guiItem;

            if (Add.BUTTON.equals(addCmd.component)) {
                guiItem =
                    new ButtonItem(addCmd, messageExchange);
            } else if (Add.LABEL.equals(addCmd.component)) {
                guiItem =
                    new LabelItem(addCmd);
            } else if (Add.PANEL.equals(addCmd.component)) {
                guiItem =
                    new PanelItem(addCmd);
            } else if (Add.TEXTFIELD.equals(addCmd.component)) {
                guiItem =
                    new TextFieldItem(addCmd, messageExchange);
            } else if (Add.WINDOW.equals(addCmd.component)) {
                guiItem =
                    new WindowItem(addCmd, messageExchange);
            } else if (Add.SELECTION.equals(addCmd.component)) {
                guiItem =
                    SelectionSource
                        .newItem(addCmd, textLibrary, messageExchange);
            } else {
                // Unrecognized category.
                LOGGER.log(Level.FINE, "Add unrecognized component: " + addCmd.component);
                guiItem = null;
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
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINE, "ID field not an integer.");

            // Not an integer ID, ignore message.
            return null;
        }
    }

    public static void disposeItem(final Item guiItem) {
        if (TextItemAdapterListener.class.isInstance(guiItem)) {
            ((TextItemAdapterListener)guiItem).removeAdapter();
        }
        guiItem.dispose();
    }
}
