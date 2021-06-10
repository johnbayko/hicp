package hicp_client;

import java.awt.Component;
//import java.text.ParseException;
import java.util.Arrays;
//import java.util.ArrayList;
//import java.util.EventListener;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.regex.Pattern;
import java.util.stream.Collectors;
//import javax.swing.AbstractListModel;
//import javax.swing.DefaultListSelectionModel;
import javax.swing.JLabel;
//import javax.swing.JList;
//import javax.swing.JScrollPane;
//import javax.swing.ListCellRenderer;
//import javax.swing.ListSelectionModel;
//import javax.swing.SwingUtilities;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
//import hicp.message.event.Changed;
//import hicp.message.event.EventEnum;

public class GUISelectionItem
    extends GUIItem  // TODO becoming a factory, no longer needed?
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIItem.class.getName() );

    protected Component _component;

    // Do these need to be enums? Or is that overkill?
    public static enum Mode {
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

    public static enum Presentation {
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

    public static enum Events {
        ENABLED("enabled"),
        DISABLED("disabled"),
        UNSELECT("unselect");

        public final String name;

        private static final Map<String, Events> eventsMap =
            Arrays.stream(Events.values())
                .collect(
                    Collectors.toMap(
                        events -> events.name,
                        events -> events
                    )
                );

        Events(final String forEvents) {
            name = forEvents;
        }

        public static Events getEnum(final String forEvents) {
            return eventsMap.getOrDefault(forEvents, ENABLED);
        }
    }

    public GUISelectionItem(Add addCmd) {
        super(addCmd);
    }

    public static GUIItem newGUIItem(
        final Add addCmd,
        final TextLibrary textLibrary,
        final MessageExchange messageExchange
    ) {
        final Presentation presentation =
            Presentation.getEnum(addCmd.presentation);

        final Mode mode = Mode.getEnum(addCmd.mode);

        switch (presentation) {
          case SCROLL:
            return new GUIScrollItem(addCmd, textLibrary, messageExchange);
          case TOGGLE:
            switch (mode) {
              case SINGLE:
                return new GUISelectionItem(addCmd);
              case MULTIPLE:
                return new GUISelectionItem(addCmd);
            }
            break;
          case DROPDOWN:
            return new GUISelectionItem(addCmd);
        }
        return null;  // Should never get here, but you know...
    }

    protected GUIItem addInvoked(final Add addCmd) {
        final Presentation presentation =
            Presentation.getEnum(addCmd.presentation);

        final Mode mode = Mode.getEnum(addCmd.mode);
        final Events events = Events.getEnum(addCmd.events);

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

    protected GUIItem modifyInvoked(final Modify modifyCmd) {
        // See what's changed.
        // Changed parent ID is handled by Controller.
        return this;
    }
}

