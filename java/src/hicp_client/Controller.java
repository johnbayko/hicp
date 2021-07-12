package hicp_client;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.GUIInfo;
import hicp.message.event.Connect;
import hicp_client.gui.ContainerItem;
import hicp_client.gui.Item;
import hicp_client.gui.ItemSource;
import hicp_client.gui.RootItem;
import hicp_client.text.TextLibrary;

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

    protected TextLibrary _textLibrary = new TextLibrary();
    protected Map<String, Item> _guiMap = new HashMap<>();

    protected RootItem _root = null;

    public Controller(Session session, Monitor monitor)
        throws UnsupportedEncodingException
    {
        session.getClass();
        monitor.getClass();

        _session = session;
        _monitor = monitor;

        _messageExchange =
            new MessageExchange(_session.in, _session.out, this);

        _root = new RootItem();
    }

// Called by owner.
    public Controller connect() {
        if (null == _messageExchange) {
            return this;
        }
        // Send connect message.
        {
            final Connect connectEvent = new Connect();
            {
                final String application = _session.params.application;
                if ( (null != application) && (0 != application.length()) )
                {
                    connectEvent.application = application;
                }
            }
            _messageExchange.send(connectEvent);
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
        _root.dispose();
        _guiMap.clear();

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
                final var authenticateInfo = commandInfo.getAuthenticateInfo();

                // Empty event to fill and send back.
                final hicp.message.event.Authenticate authenticateEvent =
                    new hicp.message.event.Authenticate();
                {
                    final String username = _session.params.username;
                    if ((null != username) && (0 != username.length())) {
                        authenticateEvent.user = username;
                    }
                }
                {
                    final String password = _session.params.password;

                    if ((null != password) && (0 != password.length())) {
                        // There is a password. Make sure that the other
                        // side supports this authentication method.
                        final String method = "plain";

                        if (authenticateInfo.hasMethod(method)) {
                            // Method is supported.
                            authenticateEvent.password = password;
                            authenticateEvent.method = method;
                        }
                    } // if (password...)
                }
                _messageExchange.send(authenticateEvent);
            }
            break;
          case ADD:
            {
                final var itemInfo = commandInfo.getItemInfo();
                if (null == itemInfo.category) {
                    // No category, ignore incomplete message.
                    LOGGER.log(Level.FINE, "Add without category");
                    break;
                }
                switch (itemInfo.category) {
                  case TEXT:
                    {
                        // Must have id field.
                        if (null == itemInfo.id) {
                            log("Add text missing id");
                            break;
                        }
                        _textLibrary.update(m);
                    }
                    break;
                  case GUI:
                    {
                        final var guiInfo = itemInfo.getGUIInfo();
                        // Must have id and component fields.
                        final String id = itemInfo.id;
                        if ((null == id) || (null == guiInfo.component)) {
                            LOGGER.log(Level.FINE, "Add gui missing id or component");
                            break;
                        }
                        {
                            final Item oldItem = _guiMap.get(id);

                            if (null != oldItem) {
                                // Remove the old one.
                                ItemSource.disposeItem(oldItem);
                                _guiMap.remove(id);
                            }
                        }
                        {
                            final Item guiItem =
                                ItemSource.newItem(
                                    m, _textLibrary, _messageExchange
                                );

                            if (null != guiItem) {
                                _guiMap.put(id, guiItem);

                                if ( GUIInfo.ComponentEnum.WINDOW.name.equals(
                                        guiItem.component
                                    )
                                ) {
                                    // Windows all get added to the root.
                                    guiItem.setParent(_root);
                                } else {
                                    // If this should be added to a parent,
                                    // determine the parent item and add to it.
                                    final var containedGUIInfo =
                                        guiInfo.getContainedGUIInfo();
                                    if (null != containedGUIInfo.parent) {
                                        final ContainerItem parentItem =
                                            (ContainerItem)
                                                _guiMap.get(
                                                    containedGUIInfo.parent
                                                );
                                        guiItem.setParent(parentItem);
                                    }
                                }
                            }
                        }
                    }
                    break;
                  default:
                    // Unrecognized category.
                    LOGGER.log(Level.FINE,
                        "Add to unrecognized category: " + itemInfo.category.name
                    );
                    break;
                }
            }
            break;
          case MODIFY:
            {
                final var itemInfo = commandInfo.getItemInfo();
                if (null == itemInfo.category) {
                    // No category, ignore incomplete message.
                    log("Modify without category");
                    break;
                }

                switch (itemInfo.category) {
                  case TEXT:
                    {
                        // Must have id field.
                        if (null == itemInfo.id) {
                            log("Modify text missing id");
                            break;
                        }
                        _textLibrary.update(m);
                    }
                  case GUI:
                    {
                        final String id = itemInfo.id;
                        final Item guiItem;
                        if (null != id) {
                            // Get GUI item based on id field.
                            guiItem = _guiMap.get(id);
                        } else {
                            // No id, modify _root.
                            guiItem = _root;
                        }
                        if (null == guiItem) {
                            // No item to modify.
                            break;
                        }
                        guiItem.modify(m);
                    }
                  default:
                    // Unrecognized category.
                    log("Add to unrecognized category: " + itemInfo.category.name);
                }
            }
            break;
          case REMOVE:
            {
                final var itemInfo = commandInfo.getItemInfo();
                if (null == itemInfo.category) {
                    // No category, ignore incomplete message.
                    log("Remove without category");
                    break;
                }

                switch (itemInfo.category) {
                  case TEXT:
                    {
                        // Must have id field.
                        if (null == itemInfo.id) {
                            log("Remove text missing id");
                            break;
                        }
                        _textLibrary.remove(itemInfo.id);
                    }
                    break;
                  case GUI:
                    {
                        log("Remove GUI");
                        // Must have id field.
                        if (null == itemInfo.id) {
                            log("Remove GUI missing id");
                            break;
                        }

                        // find the GUI item to remove.
                        final Item guiItem = _guiMap.get(itemInfo.id);
                        if (null == guiItem) {
                            // No item to remove.
                            break;
                        }

                        ItemSource.disposeItem(guiItem);

                        // Remove it from the GUI item list.
                        _guiMap.remove(itemInfo.id);
                    }
                    break;
                  default:
                    // Unrecognized category.
                    log("Remove from unrecognized category: " + itemInfo.category.name);
                    break;
                }
            }
            break;
          case DISCONNECT:
            {
                if (_messageExchange.getLastMessage()
                    instanceof hicp.message.event.Authenticate
                ) {
                    // User authentication failure.
                    log("User authentication failure.");

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

    // Utility
    private void log(String msg, Level level) {
        LOGGER.log(level, msg);
    }

    private void log(String msg) {
        log(msg, Level.FINE);
    }
}
