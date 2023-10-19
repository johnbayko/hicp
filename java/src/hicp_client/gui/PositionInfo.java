package hicp_client.gui;

import hicp.message.command.CommandInfo;

public class PositionInfo {
    public final int horizontalPosition;
    public final int verticalPosition;
    public final int horizontalSize;
    public final int verticalSize;

    public PositionInfo(final CommandInfo commandInfo) {
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
