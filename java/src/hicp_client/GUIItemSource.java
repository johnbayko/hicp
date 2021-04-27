package hicp_client;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.Add;

public class GUIItemSource {
    private static final Logger LOGGER =
        Logger.getLogger( GUIItemSource.class.getName() );

    public static GUIItem newGUIItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        try {
            // Make sure it's a real integer - not used.
            final int id = Integer.parseInt(addCmd.id);
            final GUIItem guiItem;

            if (Add.BUTTON.equals(addCmd.component)) {
                guiItem =
                    new GUIButtonItem(addCmd, messageExchange);
            } else if (Add.LABEL.equals(addCmd.component)) {
                guiItem =
                    new GUILabelItem(addCmd);
            } else if (Add.PANEL.equals(addCmd.component)) {
                guiItem =
                    new GUIPanelItem(addCmd);
            } else if (Add.TEXTFIELD.equals(addCmd.component)) {
                guiItem =
                    new GUITextFieldItem(addCmd, messageExchange);
            } else if (Add.WINDOW.equals(addCmd.component)) {
                guiItem =
                    new GUIWindowItem(addCmd, messageExchange);
            } else if (Add.SELECTION.equals(addCmd.component)) {
                guiItem =
                    new GUISelectionItem(addCmd, messageExchange);
            } else {
                // Unrecognized category.
                LOGGER.log(Level.FINE, "Add to unrecognized category: " + addCmd.category);
                return null;
            }
            if (TextItemAdapterListener.class.isInstance(guiItem)) {
                ((TextItemAdapterListener)guiItem).setAdapter(new TextItemAdapter(textLibrary));
            }
            guiItem.add(addCmd);

            return guiItem;
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINE, "ID field not an integer.");

            // Not an integer ID, ignore message.
            return null;
        }
    }

    public static void disposeGUIItem(final GUIItem guiItem) {
        if (TextItemAdapterListener.class.isInstance(guiItem)) {
            ((TextItemAdapterListener)guiItem).removeAdapter();
        }
        guiItem.dispose();
    }
}
