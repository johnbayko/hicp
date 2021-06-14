package hicp_client.gui.selection;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JLabel;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp_client.gui.Item;
import hicp_client.text.TextLibrary;

public class SelectionItem
    extends Item  // TODO becoming a factory, no longer needed?
{
    private static final Logger LOGGER =
        Logger.getLogger( SelectionItem.class.getName() );

    protected Component _component;

    public SelectionItem(Add addCmd) {
        super(addCmd);
    }

    public static Item newItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final PresentationEnum presentation =
            PresentationEnum.getEnum(addCmd.presentation);

        final ModeEnum mode = ModeEnum.getEnum(addCmd.mode);

        switch (presentation) {
          case SCROLL:
            return new ScrollItem(addCmd, textLibrary, messageExchange);
          case TOGGLE:
            switch (mode) {
              case SINGLE:
                return new SelectionItem(addCmd);
              case MULTIPLE:
                return new SelectionItem(addCmd);
            }
            break;
          case DROPDOWN:
            return new SelectionItem(addCmd);
        }
        return null;  // Should never get here, but you know...
    }

    protected Item addInvoked(final Add addCmd) {
        final PresentationEnum presentation =
            PresentationEnum.getEnum(addCmd.presentation);

        final ModeEnum mode = ModeEnum.getEnum(addCmd.mode);
        final EventsEnum events = EventsEnum.getEnum(addCmd.events);

        switch (presentation) {
//          case SCROLL:
//            _component = new JLabel("scroll selection list");  // debug
//            break;
          case TOGGLE:
            switch (mode) {
              case SINGLE:
                _component = new JLabel("radio selection list");  // debug
                break;
              case MULTIPLE:
                _component = new JLabel("checkbox selection list");  // debug
                break;
            }
            break;
          case DROPDOWN:
            _component = new JLabel("dropdoen selection list");  // debug
            break;
        }

        return this;
    }

    protected Component getComponent() {
        return _component;
    }

    protected int getGridBagAnchor() {
        if (hicp.TextDirection.RIGHT == _parent.getHorizontalTextDirection()) {
            return java.awt.GridBagConstraints.WEST;
        } else {
            return java.awt.GridBagConstraints.EAST;
        }
    }

    protected int getGridBagFill() {
        return java.awt.GridBagConstraints.NONE;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected Item modifyInvoked(final Modify modifyCmd) {
        // See what's changed.
        // Changed parent ID is handled by Controller.
        return this;
    }
}

