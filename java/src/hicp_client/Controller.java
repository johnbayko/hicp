package hicp_client;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.Add;
import hicp.message.command.Command;
import hicp.message.command.CommandEnum;
import hicp.message.command.ItemCommand;
import hicp.message.command.TextCommand;
import hicp.message.event.Connect;
import hicp.message.event.EventEnum;
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

    protected TextLibrary _textLibrary = new TextLibrary();  // debug
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

        // TODO Is text library needed? Can constructor without parameters be
        // made?
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
        if (!(m instanceof Command)) {
            // Client is not interested in events.
            return;
        }
        final Command c = (Command)m;

        // Action based on message command.
        switch (c.getCommand()) {
          case AUTHENTICATE:
            {
                final hicp.message.command.Authenticate authenticateCmd =
                    (hicp.message.command.Authenticate)c;

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

                        if (authenticateCmd.hasMethod(method)) {
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
                final ItemCommand itemCommand =
                    (ItemCommand)c;
                final ItemCommand.CategoryEnum category =
                    itemCommand.getCategory();
                if (null == category) {
                    // No category, ignore incomplete message.
                    LOGGER.log(Level.FINE, "Add without category");
                    break;
                }
                switch (category) {
                  case TEXT:
                    {
                        final TextCommand textCommand =
                            (TextCommand)itemCommand;
                        final String id = textCommand.getId();
                        // Must have id field.
                        if (null == id) {
                            log("Add text missing id");
                            break;
                        }
                        _textLibrary.update(textCommand);
                    }
                    break;
                  case GUI:
                    {
                        final hicp.message.command.Add addCmd =
                            (hicp.message.command.Add)itemCommand;
                        // Must have id and component fields.
                        final String id = addCmd.getId();
                        if ((null == id) || (null == addCmd.component)) {
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
                                    addCmd, _textLibrary, _messageExchange
                                );

                            if (null != guiItem) {
                                _guiMap.put(id, guiItem);

                                // If this should be added to a parent,
                                // determine the parent item and add to it.
                                if (Add.WINDOW.equals(guiItem.component)) {
                                    guiItem.setParent(_root);
                                } else {
                                    if (null != addCmd.parent) {
                                        final ContainerItem parentItem =
                                            (ContainerItem)
                                                _guiMap.get(addCmd.parent);
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
                        "Add to unrecognized category: " + category.name
                    );
                    break;
                }
            }
            break;
          case MODIFY:
            {
                final ItemCommand itemCommand =
                    (ItemCommand)c;
                final ItemCommand.CategoryEnum category =
                    itemCommand.getCategory();
                if (null == category) {
                    // No category, ignore incomplete message.
                    log("Modify without category");
                    break;
                }

                switch (category) {
                  case TEXT:
                    {
                        final TextCommand textCommand =
                            (TextCommand)itemCommand;
                        final String id = textCommand.getId();
                        // Must have id field.
                        if (null == id) {
                            log("Modify text missing id");
                            break;
                        }
                        _textLibrary.update(textCommand);
                    }
                  case GUI:
                    {
                        final hicp.message.command.Modify modifyCmd =
                            (hicp.message.command.Modify)itemCommand;
                        final String id = modifyCmd.getId();
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
                        guiItem.modify(modifyCmd);
                    }
                  default:
                    // Unrecognized category.
                    log("Add to unrecognized category: " + category.name);
                }
            }
            break;
          case REMOVE:
            {
                final hicp.message.command.Remove cmd =
                    (hicp.message.command.Remove)c;
                final ItemCommand.CategoryEnum category =
                    cmd.getCategory();
                if (null == category) {
                    // No category, ignore incomplete message.
                    log("Remove without category");
                    break;
                }

                switch (category) {
                  case TEXT:
                    {
                        final String id = cmd.getId();
                        // Must have id field.
                        if (null == id) {
                            log("Remove text missing id");
                            break;
                        }
                        _textLibrary.remove(id);
                    }
                    break;
                  case GUI:
                    {
                        log("Remove GUI");
                        final String id = cmd.getId();
                        // Must have id field.
                        if (null == id) {
                            log("Remove GUI missing id");
                            break;
                        }

                        // find the GUI item to remove.
                        final Item guiItem = _guiMap.get(id);
                        if (null == guiItem) {
                            // No item to remove.
                            break;
                        }

                        ItemSource.disposeItem(guiItem);

                        // Remove it from the GUI item list.
                        _guiMap.remove(id);
                    }
                    break;
                  default:
                    // Unrecognized category.
                    log("Remove from unrecognized category: " + category.name);
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
