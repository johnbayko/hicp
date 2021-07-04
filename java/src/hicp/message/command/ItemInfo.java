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

    public static final TextInfo DEFAULT_TEXT_INFO = new TextInfo();
    public static final GUIInfo DEFAULT_GUI_INFO = new GUIInfo();

    private TextInfo _textInfo = DEFAULT_TEXT_INFO;
    private GUIInfo _guiInfo = DEFAULT_GUI_INFO;

    private static final HeaderMap DEFAULT_HEADER_MAP = new HeaderMap();
    private HeaderMap _headerMap = DEFAULT_HEADER_MAP;


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

        if (DEFAULT_TEXT_INFO != _textInfo) {
            _textInfo.updateHeaderMap(headerMap);
        }
        if (DEFAULT_GUI_INFO != _guiInfo) {
            _guiInfo.updateHeaderMap(headerMap);
        }

        return this;
    }

    public TextInfo getTextInfo() {
        if (DEFAULT_TEXT_INFO == _textInfo) {
            _textInfo = new TextInfo(_headerMap);
        }
        return _textInfo;
    }

    public ItemInfo setTextInfo(final TextInfo i) {
        _textInfo = i;
        return this;
    }

    public GUIInfo getGUIInfo() {
        if (DEFAULT_GUI_INFO == _guiInfo) {
            _guiInfo = new GUIInfo(_headerMap);
        }
        return _guiInfo;
    }

    public ItemInfo setGUIInfo(final GUIInfo i) {
        _guiInfo = i;
        return this;
    }
}
