package hicp.message.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;
import hicp.message.TextAttributes;

public class SelectionInfo {
    private static final Logger LOGGER =
        Logger.getLogger( EventInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    // Move these to some common class
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

    public List<String> selected = null;

    public SelectionInfo() {
    } 

    public SelectionInfo(final HeaderMap headerMap) {
        selected = selectedFromString(
                headerMap.getString(HeaderEnum.SELECTED)
            );
    }

    public SelectionInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.SELECTED, selectedToString(selected));
        return this;
    }
}


