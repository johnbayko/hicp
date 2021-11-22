package hicp_client.gui;

import hicp.message.Message;

public class PositionInfo {
    public final int horizontalPosition;
    public final int verticalPosition;
    public final int horizontalSize;
    public final int verticalSize;

    public PositionInfo(Message m) {
        final var commandInfo = m.getCommandInfo();
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var containedGUIInfo = guiInfo.getContainedGUIInfo();

        horizontalPosition = containedGUIInfo.position.horizontal;
        verticalPosition = containedGUIInfo.position.vertical;
        horizontalSize = containedGUIInfo.size.horizontal;
        verticalSize = containedGUIInfo.size.vertical;
    }

    public PositionInfo() {
        horizontalPosition = 0;
        verticalPosition = 0;
        horizontalSize = 0;
        verticalSize = 0;
    }
}
