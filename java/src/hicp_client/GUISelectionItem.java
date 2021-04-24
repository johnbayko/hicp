package hicp_client;

import java.awt.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;

public class GUISelectionItem
    extends GUIItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIItem.class.getName() );

    protected final MessageExchange _messageExchange;

    protected Component _component;

    // Do these need to be enums? Or is that overkill?
    static enum Mode {
        SINGLE("single"),
        MULTIPLE("multiple");

        public final String name;

        private static final Map<String, Mode> modeMap =
            Arrays.stream(Mode.values())
                .collect(
                    Collectors.toMap(
                        mode -> mode.name,
                        mode -> mode
                    )
                );

        Mode(final String forMode) {
            name = forMode;
        }

        public static Mode getEnum(final String forMode) {
            return modeMap.getOrDefault(forMode, MULTIPLE);
        }
    }

    static enum Presentation {
        SCROLL("scroll"),
        TOGGLE("toggle"),
        DROPDOWN("dropdown");

        public final String name;

        private static final Map<String, Presentation> presentationMap =
            Arrays.stream(Presentation.values())
                .collect(
                    Collectors.toMap(
                        presentation -> presentation.name,
                        presentation -> presentation
                    )
                );

        Presentation(final String forPresentation) {
            name = forPresentation;
        }

        public static Presentation getEnum(final String forPresentation) {
            return presentationMap.getOrDefault(forPresentation, SCROLL);
        }
    }

    public GUISelectionItem(
        Add addCmd,
        MessageExchange messageExchange
    ) {
        super(addCmd);

        _messageExchange = messageExchange;
    }

    protected GUIItem addInvoked(final Add addCmd) {
        final Presentation presentation =
            Presentation.getEnum(addCmd.presentation);

        final Mode mode =
            Mode.getEnum(addCmd.mode);

        switch (presentation) {
          case SCROLL:
            String[] testList = {"scroll", "selection", "list"};  // debug
            JList<String> newList = new JList<>(testList);  // debug
            _component = new JScrollPane(newList);
            break;
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

        // Label string.
// Each item will have to listen to its text, have to figure out how to
// generalise that.
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

    /**
        Called in GUI thread.
     */
    protected GUIItem setTextInvoked(String text) {
        LOGGER.log(Level.FINE, "setTextInvoked(\"" + text + "\")");  // debug

        return this;
    }

    public void dispose() {
        super.dispose();
        _component = null;
    }

    protected GUIItem setEventsInvoked(final String eventsValue) {
        // TODO all of this.

        return this;
    }

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        // See what's changed.
        if (null != modifyCmd.events) {
            setEventsInvoked(modifyCmd.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

