package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class GUIWindowInfo {
    private static final Logger LOGGER =
        Logger.getLogger( GUIWindowInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public static enum Visible {
         TRUE("true"),
         FALSE("false");

        public final String name;

        private static final Map<String, Visible> enumMap =
            Arrays.stream(Visible.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        Visible(final String forName) {
            name = forName;
        }

        public static Visible getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public String text = null;
    public boolean visible = false;

    public GUIWindowInfo() {
    }

    public GUIWindowInfo(final HeaderMap headerMap) {
        text = headerMap.getString(HeaderEnum.TEXT);
        {
            final Visible visibleEnum = 
                Visible.getEnum(
                    headerMap.getString(HeaderEnum.VISIBLE)
                );
            // Default to false for null.
            visible = (Visible.TRUE == visibleEnum);
        }
    }

    public GUIWindowInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.TEXT, text);
        headerMap.putString(
            HeaderEnum.VISIBLE,
            visible ? Visible.TRUE.name : Visible.FALSE.name
        );

        return this;
    }
}
