package hicp_client.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.TextDirection;
import hicp.message.command.CommandInfo;

public abstract class LayoutItem
    extends ContainerItem
{
    private static final Logger LOGGER =
        Logger.getLogger( LayoutItem.class.getName() );

    public final static int POSITION_LIMIT = 255;

    protected final SizeInfo[][] _positionGrid =
        new SizeInfo
            [POSITION_LIMIT]
            [POSITION_LIMIT];

    // This has no parameters, so can keep single instance and use that
    // as many times as needed.
    protected final Runnable _runApplyTextDirection =
        new Runnable() {
            public void run()
            {
                applyTextDirectionInvoked();
            }
        };

    protected List<SizeInfo> _itemSizeList = new LinkedList<>();

    protected LayoutItem(final CommandInfo commandInfo) {
        super(commandInfo);
    }

    /**
        GUI thread.
     */
    protected Item addInvoked(final CommandInfo commandInfo) {
        final var itemInfo = commandInfo.getItemInfo();
        final var guiInfo = itemInfo.getGUIInfo();
        final var layoutGUIInfo = guiInfo.getLayoutGUIInfo();

        // Text direction.
        if ( (null != layoutGUIInfo.textDirection.first)
          || (null != layoutGUIInfo.textDirection.second)
        ) {
            setTextDirectionInvoked(
                layoutGUIInfo.textDirection.first,
                layoutGUIInfo.textDirection.second
            );
        } else {
            // Default text direction.
            applyTextDirectionInvoked();
        }
        return this;
    }

    class RunAdd
        implements Runnable
    {
        protected final Positionable _guiItem;
        protected boolean _addSuccessful = false;

        public RunAdd(Positionable guiItem)
        {
            _guiItem = guiItem;
        }

        public void run()
        {
            final PositionInfo positionInfo = _guiItem.getPositionInfo();

            if ( (POSITION_LIMIT <= positionInfo.horizontalPosition)
              || (POSITION_LIMIT <= positionInfo.verticalPosition)
              || (0 > positionInfo.horizontalPosition)
              || (0 > positionInfo.verticalPosition) )
            {
                // Exceeds max horizontal or vertical.
                return;
            }

            // Add to item size list.
            final SizeInfo sizeInfo = new SizeInfo(_guiItem);

            // Add to position grid.
            _positionGrid
                [positionInfo.horizontalPosition]
                [positionInfo.verticalPosition] = sizeInfo;

            _itemSizeList.add(sizeInfo);

            // Adjust all sizes. This will perform actual component add.
            adjustSizesInvoked();
        }
    }

    class RunRemove
        implements Runnable
    {
        protected final Positionable _guiItem;

        public RunRemove(Positionable guiItem)
        {
            _guiItem = guiItem;
        }

        public void run()
        {
            // Remove guiItem's component from this item's component.
            removeComponentInvoked(_guiItem.getComponent());

            final var positionInfo = _guiItem.getPositionInfo();

            final int horizontalPosition = positionInfo.horizontalPosition;
            final int verticalPosition = positionInfo.verticalPosition;

            final SizeInfo sizeInfo =
                _positionGrid[horizontalPosition][verticalPosition];

            _positionGrid[horizontalPosition][verticalPosition] = null;

            _itemSizeList.remove(sizeInfo);

            adjustSizesInvoked();
        }
    }

    protected abstract void removeComponentInvoked(Component component);

    protected abstract void addComponentInvoked(
            Component component, GridBagConstraints gridBagConstraints
        );

    protected Item adjustSizesInvoked() {
        final SizeInfo[][] sizeGrid =
            new SizeInfo[POSITION_LIMIT][POSITION_LIMIT];

        int maxHorizontal = 0;
        int maxVertical = 0;

        for (final var sizeInfo : _itemSizeList) {
            sizeInfo.oldHorizontalSize = sizeInfo.horizontalSize;
            sizeInfo.oldVerticalSize = sizeInfo.verticalSize;

            final Positionable guiItem = sizeInfo.guiItem;
            final PositionInfo positionInfo = guiItem.getPositionInfo();

            final int horizontalPosition = positionInfo.horizontalPosition;
            final int verticalPosition = positionInfo.verticalPosition;

            if (horizontalPosition > maxHorizontal) {
                maxHorizontal = horizontalPosition;
            }
            if (verticalPosition > maxVertical) {
                maxVertical = verticalPosition;
            }
        }

        int maxHorizontalLim = maxHorizontal + 1;
        int maxVerticalLim = maxVertical + 1;

        // Expand each item to its full defined size, in backwards
        // order, to see which items truncate which.
        for (int verticalPosition = maxVertical;
            verticalPosition >= 0;
            verticalPosition--)
        {
            for (int horizontalPosition = maxHorizontal;
                horizontalPosition >= 0;
                horizontalPosition--)
            {
                final SizeInfo sizeInfo =
                    _positionGrid[horizontalPosition][verticalPosition];

                if (null != sizeInfo) {
                    final Positionable guiItem = sizeInfo.guiItem;
                    final PositionInfo positionInfo = guiItem.getPositionInfo();

                    if ( (1 <= positionInfo.horizontalSize)
                      && (1 <= positionInfo.verticalSize) )
                    {
                        // Calculate initial limits.
                        int horizontalLim =
                            horizontalPosition + positionInfo.horizontalSize;
                        if (horizontalLim == horizontalPosition) {
                            // Size of 0, treat it as 1.
                            horizontalLim++;
                        }
                        if (horizontalLim > POSITION_LIMIT) {
                            horizontalLim = POSITION_LIMIT;
                        }

                        int verticalLim =
                            verticalPosition + positionInfo.verticalSize;
                        if (verticalLim == verticalPosition) {
                            // Size of 0, treat it as 1.
                            verticalLim++;
                        }
                        if (verticalLim > POSITION_LIMIT) {
                            verticalLim = POSITION_LIMIT;
                        }

                        // Find out if anything is already in that area,
                        // truncate limits if so.
adjustSizeLoop:
                        for (int checkVertical = verticalPosition;
                            checkVertical < verticalLim;
                            checkVertical++)
                        {
                            for (int checkHorizontal = horizontalPosition;
                                checkHorizontal < horizontalLim;
                                checkHorizontal++)
                            {
                                if (null !=
                                    sizeGrid[checkHorizontal][checkVertical])
                                {
                                    if (checkVertical == verticalPosition) {
                                        // First row - truncate horizontally
                                        // and continue.
                                        horizontalLim = checkHorizontal;
                                    } else {
                                        // Not first row, truncate
                                        // vertically and stop.
                                        verticalLim = checkVertical;
                                        break adjustSizeLoop;
                                    }
                                }
                            }
                        } // adjustSizeLoop:

                        sizeInfo.horizontalSize = 
                                horizontalLim - horizontalPosition;

                        sizeInfo.verticalSize = 
                                verticalLim - verticalPosition;

                        // Now fill in the area.
                        fillPositionGridArea(
                            sizeGrid,
                            sizeInfo,
                            horizontalPosition,
                            horizontalLim,
                            verticalPosition,
                            verticalLim
                        );

                        if (maxHorizontalLim < horizontalLim) {
                            maxHorizontalLim = horizontalLim;
                        }
                        if (maxVerticalLim < verticalLim) {
                            maxVerticalLim = verticalLim;
                        }
                    } else {
                        sizeInfo.horizontalSize =  1;
                        sizeInfo.verticalSize =  1;
                    }
                } // if (null != sizeInfo)

            } // for (int horizontalPosition = maxHorizontal; ...)

        } // for (int verticalPosition = maxVertical; ...)

        // Do the same (more or less) for size 0.
        for (int verticalPosition = maxVertical;
            verticalPosition >= 0;
            verticalPosition--)
        {
            for (int horizontalPosition = maxHorizontal;
                horizontalPosition >= 0;
                horizontalPosition--)
            {
                final SizeInfo sizeInfo =
                    _positionGrid[horizontalPosition][verticalPosition];

                if (null != sizeInfo) {
                    final Positionable guiItem = sizeInfo.guiItem;
                    final PositionInfo positionInfo = guiItem.getPositionInfo();

                    if ( (0 == positionInfo.horizontalSize)
                      && (0 != positionInfo.verticalSize) )
                    {
                        // Find new horizontal size.

                        // Initial horizontal limit is window limit.
                        int horizontalLim = maxHorizontalLim;

                        // Vertical limit is from vertical size, not
                        // changed.
                        final int verticalLim = sizeInfo.verticalSize + 1;

                        // Find out if anything is already in that area,
                        // truncate limits if so.
                        final int horizontalStart = horizontalPosition + 1;
                        final int verticalStart = verticalPosition;
adjustZeroHorizSizeLoop:
                        for (int checkHorizontal = horizontalStart;
                            checkHorizontal < horizontalLim;
                            checkHorizontal++)
                        {
                            for (int checkVertical = verticalStart;
                                checkVertical < verticalLim;
                                checkVertical++)
                            {
                                if (null !=
                                    sizeGrid[checkHorizontal][checkVertical])
                                {
                                    horizontalLim = checkHorizontal;
                                    break adjustZeroHorizSizeLoop;
                                }
                            }
                        } // adjustZeroHorizSizeLoop:

                        sizeInfo.horizontalSize = 
                                horizontalLim - horizontalPosition;

                        // Now fill in the area.
                        fillPositionGridArea(
                            sizeGrid,
                            sizeInfo,
                            horizontalStart,
                            horizontalLim,
                            verticalStart,
                            verticalLim
                        );
                    } else if ( (0 != positionInfo.horizontalSize)
                        && (0 == positionInfo.verticalSize) )
                    {
                        // Find new vertical size.

                        // Horizontal limit is from horizontal size, not
                        // changed.
                        final int horizontalLim = sizeInfo.horizontalSize + 1;

                        // Initial vertical limit is window limit.
                        int verticalLim = maxVerticalLim;

                        // Find out if anything is already in that area,
                        // truncate limits if so.
                        final int horizontalStart = horizontalPosition;
                        final int verticalStart = verticalPosition + 1;
adjustZeroVertSizeLoop:
                        for (int checkVertical = verticalStart;
                            checkVertical < verticalLim;
                            checkVertical++)
                        {
                            for (int checkHorizontal = horizontalStart;
                                checkHorizontal < horizontalLim;
                                checkHorizontal++)
                            {
                                if (null !=
                                    sizeGrid[checkHorizontal][checkVertical])
                                {
                                    verticalLim = checkVertical;
                                    break adjustZeroVertSizeLoop;
                                }
                            }
                        } // adjustZeroVertSizeLoop:

                        sizeInfo.verticalSize = verticalLim - verticalPosition;

                        // Now fill in the area.
                        fillPositionGridArea(
                            sizeGrid,
                            sizeInfo,
                            horizontalStart,
                            horizontalLim,
                            verticalStart,
                            verticalLim
                        );
                    } else if ( (0 == positionInfo.horizontalSize)
                        && (0 == positionInfo.verticalSize) )
                    {
                        // Adjust both sizes.

                        // Initial limits are window limits.
                        int horizontalLim = maxHorizontalLim;
                        int verticalLim = maxVerticalLim;

                        // Find out if anything is already in that area,
                        // truncate limits if so.
adjustZeroSizeLoop:
                        for (int checkVertical = verticalPosition;
                            checkVertical < verticalLim;
                            checkVertical++)
                        {
                            for (int checkHorizontal = horizontalPosition;
                                checkHorizontal < horizontalLim;
                                checkHorizontal++)
                            {
                                if (null !=
                                    sizeGrid[checkHorizontal][checkVertical])
                                {
                                    if (checkVertical == verticalPosition) {
                                        // First row - truncate horizontally
                                        // and continue.
                                        horizontalLim = checkHorizontal;
                                    } else {
                                        // Not first row, truncate
                                        // vertically and stop.
                                        verticalLim = checkVertical;

                                        // Don't check rest of this row.
                                        break adjustZeroSizeLoop;
                                    }
                                }
                            }
                        } // adjustZeroSizeLoop:

                        sizeInfo.horizontalSize = 
                                horizontalLim - horizontalPosition;

                        sizeInfo.verticalSize = 
                                verticalLim - verticalPosition;

                        // Now fill in the area.
                        fillPositionGridArea(
                            sizeGrid,
                            sizeInfo,
                            horizontalPosition,
                            horizontalLim,
                            verticalPosition,
                            verticalLim
                        );
                    }

                } // if (null != sizeInfo)

            } // for (int horizontalPosition = maxHorizontal; ...)

        } // for (int verticalPosition = maxVertical; ...)

        // Iterate through size list, any item that has changed size
        // must be removed and re-added with the new size.
        for (final var sizeInfo : _itemSizeList) {
            if ( (sizeInfo.oldHorizontalSize != sizeInfo.horizontalSize)
              || (sizeInfo.oldVerticalSize != sizeInfo.verticalSize) )
            {
                final var guiItem = sizeInfo.guiItem;
                final var positionInfo = guiItem.getPositionInfo();

                removeComponentInvoked(guiItem.getComponent());

                // Set layout parameters.
                final GridBagConstraints gridBagConstraints =
                    new GridBagConstraints(
                        positionInfo.horizontalPosition,
                        positionInfo.verticalPosition,
                        sizeInfo.horizontalSize,
                        sizeInfo.verticalSize,
                        1.0, 1.0,
                        guiItem.getGridBagAnchor(),
                        guiItem.getGridBagFill(),
                        new Insets(1, 1, 1, 1),
                        0, 0
                    );

                addComponentInvoked(
                    guiItem.getComponent(), gridBagConstraints
                );
            }
        }
        return this;
    }

    protected Item fillPositionGridArea(
        final SizeInfo[][] sizeGrid,
        final SizeInfo sizeInfo,
        final int horizontalStart,
        final int horizontalLim,
        final int verticalStart,
        final int verticalLim
    ) {
        for (int fillVertical = verticalStart;
            fillVertical < verticalLim;
            fillVertical++)
        {
            for (int fillHorizontal = horizontalStart;
                fillHorizontal < horizontalLim;
                fillHorizontal++)
            {
                sizeGrid[fillHorizontal][fillVertical] = sizeInfo;
            }
        }
        return this;
    }

    public Item setParent(ContainerItem parent) {
        super.setParent(parent);

        SwingUtilities.invokeLater(_runApplyTextDirection);

        return this;
    }

    public Item setTextDirectionInvoked(
        TextDirection firstTextDirection,
        TextDirection secondTextDirection
    ) {
        super.setTextDirectionInvoked(firstTextDirection, secondTextDirection);

        applyTextDirectionInvoked();

        return this;
    }

    protected abstract Item applyTextDirectionInvoked();
}

class SizeInfo {
    public final Positionable guiItem;

    public int horizontalSize = 0;
    public int verticalSize = 0;

    public int oldHorizontalSize = 0;
    public int oldVerticalSize = 0;

    public SizeInfo(Positionable newItem) {
        guiItem = newItem;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SizeInfo)) {
            return false;
        }
        SizeInfo sizeInfo = (SizeInfo)o;
        if (null == sizeInfo.guiItem) {
            return (null == guiItem);
        }
        final Positionable otherItem = sizeInfo.guiItem;
        final PositionInfo positionInfo = otherItem.getPositionInfo();

        return ( (positionInfo.horizontalSize == horizontalSize)
            && (positionInfo.verticalSize == verticalSize) );
    }

    public int hashCode() {
        return guiItem.hashCode();
    }
}

