package hicp.message.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.HICPHeader;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class ItemInfo {
    public static enum CategoryEnum {
        GUI("gui"),
        TEXT("text");

        public final String name;

        private static final Map<String, CategoryEnum> enumMap =
            Arrays.stream(CategoryEnum.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        CategoryEnum(final String forName) {
            name = forName;
        }

        public static CategoryEnum getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public CategoryEnum category;
    public String id;

    private TextInfo _textInfo = null;
    private GUIInfo _guiInfo = null;

    private HeaderMap _headerMap = Message.DEFAULT_HEADER_MAP;


    public ItemInfo() {
    }

    public ItemInfo(final HeaderMap headerMap) {
        _headerMap = headerMap;

        category =
            CategoryEnum.getEnum(
                headerMap.getString(HeaderEnum.CATEGORY)
            );
        id = headerMap.getString(HeaderEnum.ID);
    }

    public ItemInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.CATEGORY, category.name);
        headerMap.putString(HeaderEnum.ID, id);

        if (null != _textInfo) {
            _textInfo.updateHeaderMap(headerMap);
        }
        if (null != _guiInfo) {
            _guiInfo.updateHeaderMap(headerMap);
        }

        return this;
    }

    public TextInfo getTextInfo() {
        if (null == _textInfo) {
            _textInfo = new TextInfo(_headerMap);
        }
        return _textInfo;
    }

    public GUIInfo getGUIInfo() {
        if (null == _guiInfo) {
            _guiInfo = new GUIInfo(_headerMap);
        }
        return _guiInfo;
    }
}
