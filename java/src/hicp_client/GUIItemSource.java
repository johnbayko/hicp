package hicp_client;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.Add;

public class GUIItemSource {
    private static final Logger LOGGER =
        Logger.getLogger( GUIItemSource.class.getName() );

    public static GUIItem newGUIItem(
        Add addCmd,
        TextItem textItem,
        MessageExchange messageExchange
    ) {
        try {
            // Make sure it's a real integer - not used.
            final int id = Integer.parseInt(addCmd.id);

            if (Add.BUTTON.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUIButtonItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.LABEL.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUILabelItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.PANEL.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUIPanelItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.TEXTFIELD.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUITextFieldItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.WINDOW.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUIWindowItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else if (Add.SELECTION.equals(addCmd.component)) {
                GUIItem guiItem =
                    new GUISelectionItem(addCmd, textItem, messageExchange);

                return guiItem;
            } else {
                // Unrecognized category.
                LOGGER.log(Level.FINE, "Add to unrecognized category: " + addCmd.category);
                return null;
            }
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINE, "ID field not an integer.");

            // Not an integer ID, ignore message.
            return null;
        }
    }
}
