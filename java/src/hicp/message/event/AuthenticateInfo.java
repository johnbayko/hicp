package hicp.message.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class AuthenticateInfo {
    private static final Logger LOGGER =
        Logger.getLogger( EventInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public String user = null;
    public String method = null;
    public String password = null;

    public AuthenticateInfo() {
    } 

    public AuthenticateInfo(final HeaderMap headerMap) {
        user = headerMap.getString(HeaderEnum.USER);
        method = headerMap.getString(HeaderEnum.METHOD);
        password = headerMap.getString(HeaderEnum.PASSWORD);
    }

    public AuthenticateInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.USER, user);
        headerMap.putString(HeaderEnum.METHOD, method);
        headerMap.putString(HeaderEnum.PASSWORD, password);

        return this;
    }
}
