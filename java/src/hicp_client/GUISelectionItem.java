package hicp_client;

import java.awt.Component;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Modify;
import hicp.message.event.Changed;
import hicp.message.event.EventEnum;

public class GUISelectionItem
    extends GUIItem
{
    private static final Logger LOGGER =
        Logger.getLogger( GUIItem.class.getName() );

    protected final TextLibrary _textLibrary;
    protected final MessageExchange _messageExchange;

    public static final Pattern lineSplitter =
        Pattern.compile("\r\n", Pattern.LITERAL);

    public static final Pattern colonSplitter =
        Pattern.compile("\\s*:\\s*");
    public final int ID_IDX = 0;
    public final int INFO_IDX = 1;

    public static final Pattern commaSplitter =
        Pattern.compile("\\s*,\\s*");

    public static final Pattern keyValueSplitter =
        Pattern.compile("\\s*=\\s*");
    public final int KEY_IDX = 0;
    public final int VALUE_IDX = 1;

    // Scroll list support
    SelectionListModel _selectionListModel = null;

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

    // TODO make separate classes for these.

    // Scroll component
    class SelectionListModel
        extends AbstractListModel<SelectionItem>
    {
        // Empty list by default.
        private List<SelectionItem> _selectionItemList = new ArrayList<>();

        // GUI thread (addInvoked()).
        public SelectionListModel(
            final String itemsStr
        ) {
            updateItems(itemsStr);
        }

        // GUI thread (modifyInvoked()).
        public void updateItems(final String itemsStr) {
            final int oldSize = _selectionItemList.size();

            final String[] itemsList = lineSplitter.split(itemsStr);

            _selectionItemList = new ArrayList<>(itemsList.length);

            for (final String itemStr : itemsList) {
                try {
                    // Index from 0 to size - 1, next index will be size.
                    final int newIdx = _selectionItemList.size();

                    final SelectionItem si =
                        new SelectionItem(this, newIdx, itemStr);

                    _selectionItemList.add(si);
                } catch (ParseException | NumberFormatException ex) {
                    // Just skip.
                }
            }

            final int newSize = _selectionItemList.size();
            final int size = Math.max(oldSize, newSize);

            fireContentsChanged(this, 0, size);
        }

        public SelectionItem getElementAt(final int index) {
            return _selectionItemList.get(index);
        }

        public int getSize() {
            return _selectionItemList.size();
        }

        // GUI thread.
        // Inform JList that this item changed.
        public void itemChangedInvoked(final SelectionItem si) {
            final int idx = si.idx;

            fireContentsChanged(this, idx, idx);
        }
    }

    class SelectionItem
        implements TextListener
    {
        // Model and index in model, needed for fireContentsChanged().
        public final SelectionListModel selectionListModel;
        public final int idx;

        public final String id;

        public final String textId;
        private String text = "";

        private boolean enabled = true;

        public SelectionItem(
            final SelectionListModel newSelectionListModel,
            final int newIdx,
            final String itemStr
        )
            throws ParseException, NumberFormatException
        {
            selectionListModel = newSelectionListModel;
            idx = newIdx;

            // <id>:<type-value list>
            final String[] idInfoSplit =
                colonSplitter.split(itemStr);

            // Needs at least 2 results.
            if (idInfoSplit.length < 2) {
                throw new ParseException(
                        "Expected <id>:<item info>, missing separator ':'", 0
                    );
            }
            id = idInfoSplit[ID_IDX];

            final String[] infoList =
                commaSplitter.split(idInfoSplit[INFO_IDX]);

            // Needs at least 1 result.
            if (infoList.length < 1) {
                throw new ParseException("No selection item info found.", 0);
            }

            String textIdStr = null;
            String eventsStr = null;
            for (final String typeValueStr : infoList) {
                final String[] typeValueSplit =
                    keyValueSplitter.split(typeValueStr);

                if (typeValueSplit.length < 2) {
                    // Just skip this one.
                    continue;
                }
                final String type = typeValueSplit[KEY_IDX];
                final String value = typeValueSplit[VALUE_IDX];

                if ("text".equals(type) && (null == textIdStr)) {
                    textIdStr = value;
                } else if ("events".equals(type) && (null == eventsStr)) {
                    eventsStr = value;
                }
            }
            if (null != textIdStr) {
                textId = textIdStr;

                final TextItem textItem = _textLibrary.get(textId);
                text = textItem.getText();

                // Adds this as a listener to the text item, but through
                // SwingUtilities.invokeLater().
                textItem.addTextListener(new TextListenerInvoker(this));
            } else {
                throw new ParseException(
                    "Expected at text ID in type info, found none.", 0
                    );
            }
            if (null != eventsStr) {
                // Default is enabled, explicitly disable.
                // If not "disabled", don't disable.
                enabled = !"disabled".equals(eventsStr);
            }
        }

        public String getText() {
            return text;
        }

        public boolean isEnabled() {
            return enabled;
        }

        /*
            GUI thread.
         */
        public void textChanged(TextEvent e) {
            TextItem ti = (TextItem)e.getSource();
            text = ti.getText();

            selectionListModel.itemChangedInvoked(this);
        }
    }

    static class SelectionItemRenderer
        extends JLabel
        implements ListCellRenderer<SelectionItem>
    {
        public Component getListCellRendererComponent(
            JList<? extends SelectionItem> list,
            SelectionItem value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            setText(value.getText());
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled() && value.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

    public GUISelectionItem(
        Add addCmd,
        TextLibrary textLibrary,
        MessageExchange messageExchange
    ) {
        super(addCmd);

        _textLibrary = textLibrary;
        _messageExchange = messageExchange;

    }

    protected GUIItem addInvoked(final Add addCmd) {
        final Presentation presentation =
            Presentation.getEnum(addCmd.presentation);

        final Mode mode =
            Mode.getEnum(addCmd.mode);

        switch (presentation) {
          case SCROLL:
            final JList<SelectionItem> newList = new JList<>();

            _selectionListModel = new SelectionListModel(addCmd.items);
            newList.setModel(_selectionListModel);

            newList.setCellRenderer(new SelectionItemRenderer());

            switch (mode) {
              case SINGLE:
                newList
                    .setSelectionMode(
                        ListSelectionModel.SINGLE_SELECTION
                    );
                break;
              case MULTIPLE:
                newList
                    .setSelectionMode(
                        ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                    );
                break;
            }

            newList
                .addListSelectionListener(
                    new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            if (e.getValueIsAdjusting()) {
                                // Wait until last event to make new event
                                // message
                                return;
                            }
                            final JList source = (JList)e.getSource();
                            final int[] selected = source.getSelectedIndices();

                            final Changed changedEvent =
                                (Changed)EventEnum.CHANGED.newEvent();

                            changedEvent.id = idString;
                            changedEvent.selected = selected;

                            _messageExchange.send(changedEvent);
                        }
                    }
                );

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
        if (null != modifyCmd.items) {
            _selectionListModel.updateItems(modifyCmd.items);
        }
        if (null != modifyCmd.events) {
            setEventsInvoked(modifyCmd.events);
        }
        // Changed parent ID is handled by Controller.
        return this;
    }
}

