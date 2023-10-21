package hicp_client;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.event.EventInfo;
import hicp_client.gui.GUIController;

// Main controller for handling HICP communication.
public class Controller
    implements hicp.Controller
{
    private static final Logger LOGGER =
        Logger.getLogger( Controller.class.getName() );

    protected final Session _session;
    protected final Monitor _monitor;

    protected MessageExchange _messageExchange;
    protected boolean _isConnected = false;

    protected GUIController guiController;

    public Controller(Session session, Monitor monitor)
        throws UnsupportedEncodingException
    {
        session.getClass();
        monitor.getClass();

        _session = session;
        _monitor = monitor;

        _messageExchange =
            new MessageExchange(_session.in, _session.out, this);

        guiController = new GUIController(_messageExchange);
    }

// Called by owner.
    public Controller connect() {
        if (null == _messageExchange) {
            return this;
        }
        // Send connect message.
        {
            // New event to fill and send back.
            final var event = new Message(EventInfo.Event.CONNECT);
            final var eventInfo = event.getEventInfo();
            final var connectInfo = eventInfo.getConnectInfo();
            {
                final String application = _session.params.application;
                if ( (null != application) && (0 != application.length()) )
                {
                    connectInfo.application = application;
                }
            }
            _messageExchange.send(event);
        }
        setConnected(true);

        return this;
    }

    public Controller disconnect() {
        if (null == _messageExchange) {
            return this;
        }

        setConnected(false);
        _messageExchange = null;

        return this;
    }

    protected Controller setConnected(boolean b) {
        if (b == _isConnected) {
            return this;
        }
        _isConnected = b;
        if (_isConnected) {
            _monitor.connected();
        } else {
            _monitor.disconnected();
        }

        return this;
    }

    public boolean isConnected() {
        return _isConnected;
    }

    public Controller dispose() {
        if (null == _messageExchange) {
            return this;
        }

        // Dispose of any opened GUI objects.
        guiController.dispose();
        guiController = null;

        // Dispose of message exchange.
        _messageExchange.dispose();
        _messageExchange = null;

        return this;
    }

// Called by message exchange (input thread).
    public void receivedMessage(Message m) {
        if (!m.isCommand()) {
            // Client is not interested in events.
            return;
        }
        final var commandInfo = m.getCommandInfo();

        // Action based on message command.
        if (null == commandInfo.command) {
            return;
        }
        switch (commandInfo.command) {
          case AUTHENTICATE:
            {
                final var acInfo = commandInfo.getAuthenticateInfo();

                // New event to fill and send back.
                final var event = new Message(EventInfo.Event.AUTHENTICATE);
                final var eventInfo = event.getEventInfo();
                final var authenticateInfo = eventInfo.getAuthenticateInfo();
                {
                    final String username = _session.params.username;
                    if ((null != username) && (0 != username.length())) {
                        authenticateInfo.user = username;
                    }
                }
                {
                    final String password = _session.params.password;

                    if ((null != password) && (0 != password.length())) {
                        // There is a password. Make sure that the other
                        // side supports this authentication method.
                        final String method = "plain";

                        if (acInfo.hasMethod(method)) {
                            // Method is supported.
                            authenticateInfo.password = password;
                            authenticateInfo.method = method;
                        }
                    } // if (password...)
                }
                _messageExchange.send(event);
            }
            break;
          case ADD:
          case MODIFY:
          case REMOVE:
            guiController.receivedCommand(commandInfo);
            break;
          case DISCONNECT:
            {
                if (EventInfo.Event.AUTHENTICATE
                    == _messageExchange.getLastMessage().getEventInfo().event
                ) {
                    // User authentication failure.
                    LOGGER.log(Level.FINE, "User authentication failure.");

                    // TODO: Find a better way to notify the user.
                }
                disconnect();
            }
            break;
        }
    }

    public void closed() {
        setConnected(false);
    }
}
