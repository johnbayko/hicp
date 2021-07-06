package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class GUIInfo {
    public static enum ComponentEnum {
         BUTTON("button"),
         LABEL("label"),
         PANEL("panel"),
         SELECTION("selection"),
         TEXTPANEL("textpanel"),
         TEXTFIELD("textfield"),
         WINDOW("window");

        public final String name;

        private static final Map<String, ComponentEnum> enumMap =
            Arrays.stream(ComponentEnum.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        ComponentEnum(final String forName) {
            name = forName;
        }

        public static ComponentEnum getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public ComponentEnum component = null;

    public static final ContainedGUIInfo DEFAULT_CONTAINED_GUI_INFO =
        new ContainedGUIInfo();
    private ContainedGUIInfo _containedGUIInfo = DEFAULT_CONTAINED_GUI_INFO;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;


    public GUIInfo() {
    }

    public GUIInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        component =
            ComponentEnum.getEnum(headerMap.getString(HeaderEnum.COMPONENT));
    }

    public GUIInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.COMPONENT, component.name);

        if (DEFAULT_CONTAINED_GUI_INFO != _containedGUIInfo) {
            _containedGUIInfo.updateHeaderMap(headerMap);
        }

        return this;
    }

    public ContainedGUIInfo getContainedGUIInfo() {
        if (DEFAULT_CONTAINED_GUI_INFO == _containedGUIInfo) {
            _containedGUIInfo = new ContainedGUIInfo(_headerMap);
        }
        return _containedGUIInfo;
    }

    public GUIInfo setContainedGUIInfo(final ContainedGUIInfo i) {
        _containedGUIInfo = i;
        return this;
    }
}
