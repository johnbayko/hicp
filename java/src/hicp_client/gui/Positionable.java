package hicp_client.gui;

import java.awt.Component;

public interface Positionable {
    PositionInfo getPositionInfo();

    int getGridBagAnchor();

    int getGridBagFill();

    Component getComponent();
}
