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

    public static final LayoutGUIInfo DEFAULT_LAYOUT_GUI_INFO =
        new LayoutGUIInfo();
    private LayoutGUIInfo _layoutGUIInfo = DEFAULT_LAYOUT_GUI_INFO;


    public static final GUIButtonInfo DEFAULT_GUI_BUTTON_INFO =
        new GUIButtonInfo();
    private GUIButtonInfo _guiButtonInfo = DEFAULT_GUI_BUTTON_INFO;

    public static final GUILabelInfo DEFAULT_GUI_LABEL_INFO =
        new GUILabelInfo();
    private GUILabelInfo _guiLabelInfo = DEFAULT_GUI_LABEL_INFO;

    public static final GUISelectionInfo DEFAULT_GUI_SELECTION_INFO =
        new GUISelectionInfo();
    private GUISelectionInfo _guiSelectionInfo = DEFAULT_GUI_SELECTION_INFO;

    public static final GUITextFieldInfo DEFAULT_GUI_TEXT_FIELD_INFO =
        new GUITextFieldInfo();
    private GUITextFieldInfo _guiTextFieldInfo = DEFAULT_GUI_TEXT_FIELD_INFO;

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
        if (DEFAULT_LAYOUT_GUI_INFO != _layoutGUIInfo) {
            _layoutGUIInfo.updateHeaderMap(headerMap);
        }

        if (DEFAULT_GUI_BUTTON_INFO != _guiButtonInfo) {
            _guiButtonInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_GUI_LABEL_INFO != _guiLabelInfo) {
            _guiLabelInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_GUI_SELECTION_INFO != _guiSelectionInfo) {
            _guiSelectionInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_GUI_TEXT_FIELD_INFO != _guiTextFieldInfo) {
            _guiTextFieldInfo.updateHeaderMap(headerMap);
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

    public LayoutGUIInfo getLayoutGUIInfo() {
        if (DEFAULT_LAYOUT_GUI_INFO == _layoutGUIInfo) {
            _layoutGUIInfo = new LayoutGUIInfo(_headerMap);
        }
        return _layoutGUIInfo;
    }

    public GUIInfo setLayoutGUIInfo(final LayoutGUIInfo i) {
        _layoutGUIInfo = i;
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

    public GUISelectionInfo getGUISelectionInfo() {
        if (DEFAULT_GUI_SELECTION_INFO == _guiSelectionInfo) {
            _guiSelectionInfo = new GUISelectionInfo(_headerMap);
        }
        return _guiSelectionInfo;
    }

    public GUIInfo setGUISelectionInfo(final GUISelectionInfo i) {
        _guiSelectionInfo = i;
        return this;
    }

    public GUITextFieldInfo getGUITextFieldInfo() {
        if (DEFAULT_GUI_TEXT_FIELD_INFO == _guiTextFieldInfo) {
            _guiTextFieldInfo = new GUITextFieldInfo(_headerMap);
        }
        return _guiTextFieldInfo;
    }

    public GUIInfo setGUITextFieldInfo(final GUITextFieldInfo i) {
        _guiTextFieldInfo = i;
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
