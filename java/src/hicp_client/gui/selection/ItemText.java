package hicp_client.gui.selection;

import hicp.message.command.GUISelectionInfo;
import hicp_client.text.TextEvent;
import hicp_client.text.TextItem;
import hicp_client.text.TextLibrary;
import hicp_client.text.TextListener;
import hicp_client.text.TextListenerInvoker;

public class ItemText
    implements TextListener
{
    static interface ChangeListener {
        // GUI thread.
        public void itemChangedInvoked(ItemText itemText);
    }

    // Model and index in model are needed for fireContentsChanged().
    public final int idx;
    public final String id;

    private final TextLibrary textLibrary;
    private String text = "";

    private final ChangeListener changeListener;
    private final boolean enabled;

    public ItemText(
        final ChangeListener newChangeListener,
        final TextLibrary newTextLibrary,
        final int newIdx,
        final GUISelectionInfo.Item itemInfo
    ) {
        changeListener = newChangeListener;
        textLibrary = newTextLibrary;
        idx = newIdx;

        id = itemInfo.id;
        {
            final TextItem textItem = textLibrary.get(itemInfo.textId);
            text = textItem.getText();

            // Adds this as a listener to the text item, but through
            // SwingUtilities.invokeLater().
            textItem.addTextListener(new TextListenerInvoker(this));
        }
        enabled = (itemInfo.events != GUISelectionInfo.EventsEnum.DISABLED);
    }

    public String getText() {
        return text;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // GUI thread.
    public void textChanged(TextEvent e) {
        TextItem ti = (TextItem)e.getSource();
        text = ti.getText();

        if (null != changeListener) {
            changeListener.itemChangedInvoked(this);
        }
    }
}

