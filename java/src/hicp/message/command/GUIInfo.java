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

    private ContainedGUIInfo _containedGUIInfo = null;
    private LayoutGUIInfo _layoutGUIInfo = null;

    private GUIButtonInfo _guiButtonInfo = null;
    private GUILabelInfo _guiLabelInfo = null;
    private GUIPanelInfo _guiPanelInfo = null;
    private GUISelectionInfo _guiSelectionInfo = null;
    private GUITextFieldInfo _guiTextFieldInfo = null;
    private GUIWindowInfo _guiWindowInfo = null;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;

    public GUIInfo() {
    }

    public GUIInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        final String componentName = headerMap.getString(HeaderEnum.COMPONENT);
        component = ComponentEnum.getEnum(componentName);
    }

    public GUIInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.COMPONENT, component.name);

        if (null != _containedGUIInfo) {
            _containedGUIInfo.updateHeaderMap(headerMap);
        }
        if (null != _layoutGUIInfo) {
            _layoutGUIInfo.updateHeaderMap(headerMap);
        }

        if (null != _guiButtonInfo) {
            _guiButtonInfo.updateHeaderMap(headerMap);
        }
        if (null != _guiLabelInfo) {
            _guiLabelInfo.updateHeaderMap(headerMap);
        }
        if (null != _guiPanelInfo) {
            _guiPanelInfo.updateHeaderMap(headerMap);
        }
        if (null != _guiSelectionInfo) {
            _guiSelectionInfo.updateHeaderMap(headerMap);
        }
        if (null != _guiTextFieldInfo) {
            _guiTextFieldInfo.updateHeaderMap(headerMap);
        }
        if (null != _guiWindowInfo) {
            _guiWindowInfo.updateHeaderMap(headerMap);
        }

        return this;
    }

    public ContainedGUIInfo getContainedGUIInfo() {
        if (null == _containedGUIInfo) {
            _containedGUIInfo = new ContainedGUIInfo(_headerMap);
        }
        return _containedGUIInfo;
    }

    public LayoutGUIInfo getLayoutGUIInfo() {
        if (null == _layoutGUIInfo) {
            _layoutGUIInfo = new LayoutGUIInfo(_headerMap);
        }
        return _layoutGUIInfo;
    }


    public GUIButtonInfo getGUIButtonInfo() {
        if (null == _guiButtonInfo) {
            _guiButtonInfo = new GUIButtonInfo(_headerMap);
        }
        return _guiButtonInfo;
    }

    public GUILabelInfo getGUILabelInfo() {
        if (null == _guiLabelInfo) {
            _guiLabelInfo = new GUILabelInfo(_headerMap);
        }
        return _guiLabelInfo;
    }

    public GUIPanelInfo getGUIPanelInfo() {
        if (null == _guiPanelInfo) {
            _guiPanelInfo = new GUIPanelInfo(_headerMap);
        }
        return _guiPanelInfo;
    }

    public GUISelectionInfo getGUISelectionInfo() {
        if (null == _guiSelectionInfo) {
            _guiSelectionInfo = new GUISelectionInfo(_headerMap);
        }
        return _guiSelectionInfo;
    }

    public GUITextFieldInfo getGUITextFieldInfo() {
        if (null == _guiTextFieldInfo) {
            _guiTextFieldInfo = new GUITextFieldInfo(_headerMap);
        }
        return _guiTextFieldInfo;
    }

    public GUIWindowInfo getGUIWindowInfo() {
        if (null == _guiWindowInfo) {
            _guiWindowInfo = new GUIWindowInfo(_headerMap);
        }
        return _guiWindowInfo;
    }
}
