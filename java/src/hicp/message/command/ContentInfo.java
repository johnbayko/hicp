package hicp.message.command;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;
import hicp.message.Message;

public class ContentInfo {
    public static enum Action {
        SET("set"),
        ADD("add"),
        DELETE("delete");

        public final String name;

        private static final Map<String, Action> enumMap =
            Arrays.stream(Action.values())
                .collect(
                    Collectors.toMap(
                        e -> e.name,
                        e -> e
                    )
                );


        Action(final String forName) {
            name = forName;
        }

        public static Action getEnum(String name) {
            return enumMap.get(name);
        }
    }

    public static class SetInfo {
        public String text = "";

        public SetInfo() {
        }

        public SetInfo(final String contentInfoStr) {
            text = contentInfoStr;
        }

        public SetInfo updateHeaderMap(
            final HeaderMap headerMap
        ) {
            headerMap.putString(
                HeaderEnum.CONTENT,
                Action.SET.name + "set:" + text
            );
            return this;
        }
    }

    public static class AddInfo {
        public String text = "";
        public int position = 0;

        public AddInfo() {
        }

        public AddInfo(final String contentInfoStr)
            throws ParseException
        {
            final String[] contentSplit =
                Message.splitWith(Message.COLON_SPLITTER, contentInfoStr, 2);

            // Needs at least 2 results.
            if (contentSplit.length < 2) {
                throw new ParseException(
                        "Expected add:<position>:<text>, missing <position> or <text>", 0
                    );
            }
            try {
                position = Integer.parseInt(contentSplit[0]);
            } catch (NumberFormatException nfe) {
                throw new ParseException(
                        "Expected add:<position>:<text>, invalid <position>", 0
                    );
            }
            text = contentSplit[1];
        }

        public AddInfo updateHeaderMap(
            final HeaderMap headerMap
        ) {
            headerMap.putString(
                HeaderEnum.CONTENT,
                Action.ADD.name + ":"+ position + ":" + text
            );
            return this;
        }
    }

    public static class DeleteInfo {
        public int position = 0;
        public int length = 0;

        public DeleteInfo() {
        }

        public DeleteInfo(final String contentInfoStr)
            throws ParseException
        {
            final String[] contentSplit =
                Message.splitWith(Message.COLON_SPLITTER, contentInfoStr);

            // Needs at least 2 results.
            if (contentSplit.length < 2) {
                throw new ParseException(
                        "Expected delete:<position>:<length>, missing <position> or <length>", 0
                    );
            }
            try {
                position = Integer.parseInt(contentSplit[0]);
            } catch (NumberFormatException nfe) {
                throw new ParseException(
                        "Expected delete:<position>:<text>, invalid <position>", 0
                    );
            }
            try {
                length = Integer.parseInt(contentSplit[1]);
            } catch (NumberFormatException nfe) {
                throw new ParseException(
                        "Expected delete:<position>:<length>, invalid <length>", 0
                    );
            }
        }

        public DeleteInfo updateHeaderMap(
            final HeaderMap headerMap
        ) {
            headerMap.putString(
                HeaderEnum.CONTENT,
                Action.DELETE.name + ":"+ position + ":" + length
            );
            return this;
        }
    }


    public Action action = null;

//    private String[] contentSplit = null;
    private String contentInfoStr = "";

    private SetInfo setInfo = null;
    private AddInfo addInfo = null;
    private DeleteInfo deleteInfo = null;

    public ContentInfo() {
    }

    public ContentInfo(final HeaderMap headerMap)
        throws ParseException
    {
        final String contentStr = headerMap.getString(HeaderEnum.CONTENT);

        final String[] contentSplit =
            Message.splitWith(Message.COLON_SPLITTER, contentStr, 2);

        // Needs at least 2 results.
        if (contentSplit.length < 2) {
            throw new ParseException(
                    "Expected <content action>:<content info>, missing separator ':' or <content info>", 0
                );
        }
        final String actionName = contentSplit[0];
        action = Action.getEnum(actionName);

        contentInfoStr = contentSplit[1];
    }

    public ContentInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        if (null != setInfo) {
            setInfo.updateHeaderMap(headerMap);
        }
        if (null != addInfo) {
            addInfo.updateHeaderMap(headerMap);
        }
        if (null != deleteInfo) {
            deleteInfo.updateHeaderMap(headerMap);
        }
        return this;
    }

    public SetInfo getSetInfo() {
        if (null == setInfo) {
            setInfo = new SetInfo(contentInfoStr);
        }
        return setInfo;
    }

    public AddInfo getAddInfo()
        throws ParseException
    {
        if (null == addInfo) {
            addInfo = new AddInfo(contentInfoStr);
        }
        return addInfo;
    }

    public DeleteInfo getDeleteInfo()
        throws ParseException
    {
        if (null == deleteInfo) {
            deleteInfo = new DeleteInfo(contentInfoStr);
        }
        return deleteInfo;
    }
}
