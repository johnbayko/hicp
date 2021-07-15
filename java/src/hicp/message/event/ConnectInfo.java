package hicp.message.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.HeaderMap;
import hicp.message.HeaderEnum;

public class ConnectInfo {
    private static final Logger LOGGER =
        Logger.getLogger( ConnectInfo.class.getName() );
//LOGGER.log(Level.FINE, " " + );  // debug

    public String application = null;

    public ConnectInfo() {
    } 

    public ConnectInfo(final HeaderMap headerMap) {
        application = headerMap.getString(HeaderEnum.APPLICATION);
    }

    public ConnectInfo updateHeaderMap(
        final HeaderMap headerMap
    ) {
        headerMap.putString(HeaderEnum.APPLICATION, application);

        return this;
    }
}

