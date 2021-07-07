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

    public static final GUILabelInfo DEFAULT_GUI_LABEL_INFO =
        new GUILabelInfo();
    private GUILabelInfo _guiLabelInfo = DEFAULT_GUI_LABEL_INFO;

    public static final GUIButtonInfo DEFAULT_GUI_BUTTON_INFO =
        new GUIButtonInfo();
    private GUIButtonInfo _guiButtonInfo = DEFAULT_GUI_BUTTON_INFO;

    public static final GUIWindowInfo DEFAULT_GUI_WINDOW_INFO =
        new GUIWindowInfo();
    private GUIWindowInfo _guiWindowInfo = DEFAULT_GUI_WINDOW_INFO;

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
        if (DEFAULT_GUI_LABEL_INFO != _guiLabelInfo) {
            _guiLabelInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_GUI_BUTTON_INFO != _guiButtonInfo) {
            _guiButtonInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_GUI_WINDOW_INFO != _guiWindowInfo) {
            _guiWindowInfo.updateHeaderMap(headerMap);
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

    public GUILabelInfo getGUILabelInfo() {
        if (DEFAULT_GUI_LABEL_INFO == _guiLabelInfo) {
            _guiLabelInfo = new GUILabelInfo(_headerMap);
        }
        return _guiLabelInfo;
    }

    public GUIInfo setGUILabelInfo(final GUILabelInfo i) {
        _guiLabelInfo = i;
        return this;
    }

    public GUIButtonInfo getGUIButtonInfo() {
        if (DEFAULT_GUI_BUTTON_INFO == _guiButtonInfo) {
            _guiButtonInfo = new GUIButtonInfo(_headerMap);
        }
        return _guiButtonInfo;
    }

    public GUIInfo setGUIButtonInfo(final GUIButtonInfo i) {
        _guiButtonInfo = i;
        return this;
    }

    public GUIWindowInfo getGUIWindowInfo() {
        if (DEFAULT_GUI_WINDOW_INFO == _guiWindowInfo) {
            _guiWindowInfo = new GUIWindowInfo(_headerMap);
        }
        return _guiWindowInfo;
    }

    public GUIInfo setGUIWindowInfo(final GUIWindowInfo i) {
        _guiWindowInfo = i;
        return this;
    }
}
