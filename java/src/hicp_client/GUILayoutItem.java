package hicp_client;

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
import hicp.message.command.Add;

public abstract class GUILayoutItem
    extends GUIContainerItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUILayoutItem.class.getName() );

    public final static int POSITION_LIMIT = 255;

    protected final MessageExchange _messageExchange;

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

    protected GUILayoutItem(
        Add addCmd,
        TextItem textItem,
        MessageExchange messageExchange
    ) {
        super(addCmd);

        _messageExchange = messageExchange;
    }

    class RunNew
        implements Runnable
    {
        protected final Add _addCmd;

        public RunNew(Add addCmd)
        {
            _addCmd = addCmd;
        }

        public void run()
        {
            // Text direction.
            if ( (null != _addCmd.firstTextDirection)
              || (null != _addCmd.secondTextDirection)
            ) {
                setTextDirectionInvoked(
                    _addCmd.firstTextDirection,
                    _addCmd.secondTextDirection
                );
            } else {
                // Default text direction.
                applyTextDirectionInvoked();
            }
        }
    }

    class RunAdd
        implements Runnable
    {
        protected final GUIItem _guiItem;
        protected boolean _addSuccessful = false;

        public RunAdd(GUIItem guiItem)
        {
            _guiItem = guiItem;
        }

        public void run()
        {
            // Add to item size list.
            final SizeInfo sizeInfo = new SizeInfo(_guiItem);

            // Add to position grid.
            _positionGrid
                [_guiItem.horizontalPosition]
                [_guiItem.verticalPosition] = sizeInfo;

            _itemSizeList.add(sizeInfo);

            // Adjust all sizes. This will perform actual component add.
            adjustSizesInvoked();
        }
    }

    class RunRemove
        implements Runnable
    {
        protected final GUIItem _guiItem;

        public RunRemove(GUIItem guiItem)
        {
            _guiItem = guiItem;
        }

        public void run()
        {
            // Remove guiItem's component from this item's component.
            removeComponentInvoked(_guiItem.getComponent());

            final int horizontalPosition = _guiItem.horizontalPosition;
            final int verticalPosition = _guiItem.verticalPosition;

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

    protected GUIItem adjustSizesInvoked() {
        final SizeInfo[][] sizeGrid =
            new SizeInfo[POSITION_LIMIT][POSITION_LIMIT];

        int maxHorizontal = 0;
        int maxVertical = 0;

        for (final var sizeInfo : _itemSizeList) {
            sizeInfo.oldHorizontalSize = sizeInfo.horizontalSize;
            sizeInfo.oldVerticalSize = sizeInfo.verticalSize;

            final GUIItem guiItem = sizeInfo.guiItem;

            final int horizontalPosition = guiItem.horizontalPosition;
            final int verticalPosition = guiItem.verticalPosition;

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
                    final GUIItem guiItem = sizeInfo.guiItem;

                    if ( (1 <= guiItem.horizontalSize)
                      && (1 <= guiItem.verticalSize) )
                    {
                        // Calculate initial limits.
                        int horizontalLim =
                            horizontalPosition + guiItem.horizontalSize;
                        if (horizontalLim == horizontalPosition) {
                            // Size of 0, treat it as 1.
                            horizontalLim++;
                        }
                        if (horizontalLim > POSITION_LIMIT) {
                            horizontalLim = POSITION_LIMIT;
                        }

                        int verticalLim =
                            verticalPosition + guiItem.verticalSize;
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
                    final GUIItem guiItem = sizeInfo.guiItem;

                    if ( (0 == guiItem.horizontalSize)
                      && (0 != guiItem.verticalSize) )
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
                    } else if ( (0 != guiItem.horizontalSize)
                        && (0 == guiItem.verticalSize) )
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
                    } else if ( (0 == guiItem.horizontalSize)
                        && (0 == guiItem.verticalSize) )
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
                removeComponentInvoked(guiItem.getComponent());

                // Set layout parameters.
                final GridBagConstraints gridBagConstraints =
                    new GridBagConstraints(
                        guiItem.horizontalPosition,
                        guiItem.verticalPosition,
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

    protected GUIItem fillPositionGridArea(
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

    public GUIItem setParent(GUIContainerItem parent) {
        super.setParent(parent);

        SwingUtilities.invokeLater(_runApplyTextDirection);

        return this;
    }

    /**
        Called in non-GUI thread.
     */
    protected GUIItem setText(String text) {
        SwingUtilities.invokeLater(
            new RunSetText(text)
        );

        return this;
    }

    class RunSetText
        implements Runnable
    {
        protected final String _text;

        RunSetText(String text) {
            _text = text;
        }

        public void run() {
            setTextInvoked(_text);
        }
    }

    public GUIItem setTextDirectionInvoked(
        TextDirection firstTextDirection,
        TextDirection secondTextDirection
    ) {
        super.setTextDirectionInvoked(firstTextDirection, secondTextDirection);

        applyTextDirectionInvoked();

        return this;
    }

    protected abstract GUIItem applyTextDirectionInvoked();
}

class SizeInfo {
    public final GUIItem guiItem;

    public int horizontalSize = 0;
    public int verticalSize = 0;

    public int oldHorizontalSize = 0;
    public int oldVerticalSize = 0;

    public SizeInfo(GUIItem newGUIItem) {
        guiItem = newGUIItem;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SizeInfo)) {
            return false;
        }
        SizeInfo sizeInfo = (SizeInfo)o;
        if (null == sizeInfo.guiItem) {
            return (null == guiItem);
        }
        final GUIItem otherGUIItem = sizeInfo.guiItem;
        return ( (otherGUIItem.horizontalSize == horizontalSize)
            && (otherGUIItem.verticalSize == verticalSize) );
    }

    public int hashCode() {
        return guiItem.hashCode();
    }
}

