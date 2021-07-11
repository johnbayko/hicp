package hicp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import hicp.message.HeaderEnum;
import hicp.message.Message;

public class HeaderMap {
    private final Map<HeaderEnum, HICPHeader> _headerMap = new HashMap<>();

    public HeaderMap put(
        final HeaderEnum e,
        final HICPHeader h
    ) {
        // Quietly don't add if not complete.
        if (null == e || null == h) {
            return this;
        }
        _headerMap.put(e, h);

        return this;
    }

    public HeaderMap putString(
        final HeaderEnum e,
        final String vs
    ) {
        if (null == vs) {
            return this;
        }
        return put(e, new HICPHeader(e, new HICPHeaderValue(vs) ));
    }

    public HICPHeader getHeader(final HeaderEnum e) {
        return _headerMap.get(e);
    }

    public String getString(final HeaderEnum e) {
        final HICPHeader h = getHeader(e);
        if (null == h) {
            return null;
        }
        return h.value.getString();
    }

    public boolean getIsMatch(final HeaderEnum e, final String compareStr) {
        if (null == compareStr) {
            return false;
        }
        return compareStr.equals(getString(e));
    }

    public HICPHeader remove(final HeaderEnum e) {
        if (null == e) {
            return null;
        }
        return _headerMap.remove(e);
    }

    public Collection<HICPHeader> values() {
        return _headerMap.values();
    }

    /**
        Convert comma separated string to Set.
     */
    public static Set<String> makeStringSet(final String s) {
        // Treat "" as // empty.
        if ((null == s) || "".equals(s)) {
            // Empty set.
            return Set.of();
        }
        final String[] sSplit = Message.splitWith(Message.COMMA_SPLITTER, s);
        return Set.of(sSplit);
    }
}
