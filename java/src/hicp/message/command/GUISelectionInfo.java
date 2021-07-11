package hicp.message.command;

import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class GUISelectionInfo {
    public static enum EventsEnum {
        ENABLED("enabled"),
        DISABLED("disabled"),
        UNSELECT("unselect");

        public final String name;

        private static final Map<String, EventsEnum> enumMap =
            Arrays.stream(EventsEnum.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        EventsEnum(final String forName) {
            name = forName;
        }

        public static EventsEnum getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public static class Item {
        public final String id;
        public final String textId;
        public final EventsEnum events;

        public Item(final String itemStr)
            throws ParseException
        {
            // <id>:<type-value list>
            final String[] idInfoSplit =
                Message.splitWith(Message.COLON_SPLITTER, itemStr);

            // Needs at least 2 results.
            if (idInfoSplit.length < 2) {
                throw new ParseException(
                        "Expected <id>:<item info>, missing separator ':'", 0
                    );
            }
            id = idInfoSplit[Message.ID_IDX];

            final String[] infoList =
                Message.splitWith(
                    Message.COMMA_SPLITTER, idInfoSplit[Message.INFO_IDX]
                );

            // Needs at least 1 result.
            if (infoList.length < 1) {
                throw new ParseException("No selection item info found.", 0);
            }

            String textIdStr = null;
            String eventsStr = null;
            for (final String typeValueStr : infoList) {
                final String[] typeValueSplit =
                    Message.splitWith(Message.KEY_VALUE_SPLITTER, typeValueStr);

                if (typeValueSplit.length < 2) {
                    // Just skip this one.
                    continue;
                }
                final String type = typeValueSplit[Message.KEY_IDX];
                final String value = typeValueSplit[Message.VALUE_IDX];

                if ("text".equals(type) && (null == textIdStr)) {
                    textIdStr = value;
                } else if ("events".equals(type) && (null == eventsStr)) {
                    eventsStr = value;
                }
            }

            if (null != textIdStr) {
                textId = textIdStr;
            } else {
                throw new ParseException(
                    "Expected at text ID in type info, found none.", 0
                    );
            }
            events = EventsEnum.getEnum(eventsStr);
        }

        @Override
        public String toString() {
            // Required values
            if ((null == id) || (null == textId)) {
                return "";
            }
            return id + ": " + textId
                + ((null != events) ? (", " + events) : "");
        }
    }

    public static enum Mode {
        SINGLE("single"),
        MULTIPLE("multiple");

        public final String name;

        private static final Map<String, Mode> enumMap =
            Arrays.stream(Mode.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );

        Mode(final String newName) {
            name = newName;
        }

        public static Mode getEnum(final String name) {
            return enumMap.getOrDefault(name, MULTIPLE);
        }
    }

    public static enum Presentation {
        SCROLL("scroll"),
        TOGGLE("toggle"),
        DROPDOWN("dropdown");

        public final String name;

        private static final Map<String, Presentation> enumMap =
            Arrays.stream(Presentation.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );

        Presentation(final String newName) {
            name = newName;
        }

        public static Presentation getEnum(final String name) {
            return enumMap.getOrDefault(name, SCROLL);
        }
    }

    public static List<Item> itemsFromString(final String itemsListStr) {
        if (null == itemsListStr) {
            return null;
        }
        final String[] itemsArray =
            Message.splitWith(Message.LINE_SPLITTER, itemsListStr);
        if (0 == itemsArray.length) {
            return null;
        }

        final List<Item> itemsList = new ArrayList<>(itemsArray.length);
        for (final String itemStr : itemsArray) {
            try {
                itemsList.add(new Item(itemStr));
            } catch (ParseException ex) {
                // Just skip.
            }
        }
        return itemsList;
    }

    public static String itemsToString(final List<Item> items) {
        if (null == items) {
            return null;
        }
        final StringBuilder s = new StringBuilder();
        String sep= "";
        for (final var item : items) {
            s.append(sep).append(item.toString());
            sep = Message.EOL;
        }
        return s.toString();
    }

    public static List<String> selectedFromString(
        final String selectedListStr
    ) {
        if (null == selectedListStr) {
            return null;
        }
        final String[] selectedArray =
            Message.splitWith(Message.COMMA_SPLITTER, selectedListStr);
        if (0 == selectedArray.length) {
            return null;
        }

        final List<String> selectedList =
            new ArrayList<>(selectedArray.length);
        for (final String selectedStr : selectedArray) {
            selectedList.add(selectedStr);
        }
        return selectedList;
    }

    public static String selectedToString(final List<String> selected) {
        if (null == selected) {
            return null;
        }
        final StringBuilder s = new StringBuilder();
        String sep= "";
        for (final var selectedStr : selected) {
            s.append(sep).append(selectedStr);
            sep = Message.EOL;
        }
        return s.toString();
    }

    private GUISelectionInfo setHeightFromString(final String heightStr) {
        if (null == heightStr) {
            _hasHeight = false;
            return this;
        }
        try {
            _height = Integer.parseInt(heightStr);
            _hasHeight = true;
        } catch (NumberFormatException ex) {
            _hasHeight = false;
        }
        return this;
    }

    private GUISelectionInfo setWidthFromString(final String widthStr) {
        if (null == widthStr) {
            _hasWidth = false;
            return this;
        }
        try {
            _width = Integer.parseInt(widthStr);
            _hasWidth = true;
        } catch (NumberFormatException ex) {
            _hasWidth = false;
        }
        return this;
    }

    public EventsEnum events = null;
    public List<Item> items = null;
    public List<String> selected = null;

    private boolean _hasHeight = false;
    private int _height = 0;

    public Mode mode = null;
    public Presentation presentation = null;

    private boolean _hasWidth = false;
    private int _width = 0;

    public GUISelectionInfo() {
    }

    public GUISelectionInfo(final HeaderMap headerMap) {
        events =
            EventsEnum.getEnum(
                headerMap.getString(HeaderEnum.EVENTS)
            );
        items = itemsFromString(
                headerMap.getString(HeaderEnum.ITEMS)
            );
        selected = selectedFromString(
                headerMap.getString(HeaderEnum.SELECTED)
            );
        setHeightFromString(headerMap.getString(HeaderEnum.HEIGHT));
        mode =
            Mode.getEnum(
                headerMap.getString(HeaderEnum.MODE)
            );
        presentation =
            Presentation.getEnum(
                headerMap.getString(HeaderEnum.PRESENTATION)
            );
        setWidthFromString(headerMap.getString(HeaderEnum.WIDTH));
    }

    public GUISelectionInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.EVENTS, events.name);
        headerMap.putString(HeaderEnum.ITEMS, itemsToString(items));
        headerMap.putString(HeaderEnum.SELECTED, selectedToString(selected));
        if (_hasHeight) {
            headerMap.putString(HeaderEnum.HEIGHT, Integer.toString(_height));
        }
        headerMap.putString(HeaderEnum.MODE, mode.name);
        headerMap.putString(HeaderEnum.PRESENTATION, presentation.name);
        if (_hasWidth) {
            headerMap.putString(HeaderEnum.WIDTH, Integer.toString(_width));
        }

        return this;
    }

    public boolean hasHeight() {
        return _hasHeight;
    }
    public int getHeight() {
        return _height;
    }
    public GUISelectionInfo setHeight(final int height) {
        _height = height;
        _hasHeight = true;
        return this;
    }

    public boolean hasWidth() {
        return _hasWidth;
    }
    public int getWidth() {
        return _width;
    }
    public GUISelectionInfo setWidth(final int width) {
        _width = width;
        _hasWidth = true;
        return this;
    }
}

