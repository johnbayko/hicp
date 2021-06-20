package hicp_client;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import hicp.MessageExchange;
import hicp.message.command.Add;
import hicp.message.command.Command;
import hicp.message.command.CommandEnum;
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
            Connect connectEvent = (Connect)EventEnum.CONNECT.newEvent();
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
    public void receivedMessage(CommandEnum commandEnum, Command c) {
        // Action based on message command.
        switch (commandEnum) {
          case AUTHENTICATE:
            {
                final hicp.message.command.Authenticate authenticateCmd =
                    (hicp.message.command.Authenticate)c;

                final hicp.message.event.Authenticate authenticateEvent =
                    (hicp.message.event.Authenticate)EventEnum
                        .AUTHENTICATE
                        .newEvent();
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
                final hicp.message.command.Add addCmd =
                    (hicp.message.command.Add)c;
                if (null == addCmd.category) {
                    // No category, ignore incomplete message.
                    LOGGER.log(Level.FINE, "Add without category");
                    break;
                }
                if (addCmd.TEXT.equals(addCmd.category)) {
                    _textLibrary.addModify(addCmd);
                } else if (addCmd.GUI.equals(addCmd.category)) {
                    // Must have id and component fields.
                    if ((null == addCmd.id) || (null == addCmd.component)) {
                        LOGGER.log(Level.FINE, "Add gui missing id or component");
                        break;
                    }
                    {
                        final Item oldItem = _guiMap.get(addCmd.id);

                        if (null != oldItem) {
                            // Remove the old one.
                            ItemSource.disposeItem(oldItem);
                            _guiMap.remove(addCmd.id);
                        }
                    }
                    {
                        final Item guiItem =
                            ItemSource.newItem(
                                addCmd, _textLibrary, _messageExchange
                            );

                        if (null != guiItem) {
                            _guiMap.put(addCmd.id, guiItem);

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
                } else {
                    // Unrecognized category.
                    LOGGER.log(Level.FINE,
                        "Add to unrecognized category: " + addCmd.category
                    );
                }
            }
            break;
          case MODIFY:
            {
                final hicp.message.command.Modify modifyCmd =
                    (hicp.message.command.Modify)c;
                if (null == modifyCmd.category) {
                    // No category, ignore incomplete message.
                    log("Modify without category");
                    break;
                }

                if (modifyCmd.TEXT.equals(modifyCmd.category)) {
                    _textLibrary.addModify(modifyCmd);
                } else if (modifyCmd.GUI.equals(modifyCmd.category)) {
                    final Item guiItem;
                    if (null != modifyCmd.id) {
                        // Get GUI item based on id field.
                        guiItem = _guiMap.get(modifyCmd.id);
                    } else {
                        // No id, modify _root.
                        guiItem = _root;
                    }
                    if (null == guiItem) {
                        // No item to modify.
                        break;
                    }
                    guiItem.modify(modifyCmd);
                } else {
                    // Unrecognized category.
                    log("Add to unrecognized category: " + modifyCmd.category);
                }
            }
            break;
          case REMOVE:
            {
                final hicp.message.command.Remove removeCmd =
                    (hicp.message.command.Remove)c;
                if (null == removeCmd.category) {
                    // No category, ignore incomplete message.
                    log("Remove without category");
                    break;
                }

                if (removeCmd.TEXT.equals(removeCmd.category)) {
                    // Must have id field.
                    if (null == removeCmd.id) {
                        log("Remove text missing id");
                        break;
                    }
                    _textLibrary.remove(removeCmd.id);
                } else if (removeCmd.GUI.equals(removeCmd.category)) {
                    log("Remove GUI");
                    // Must have id field.
                    if (null == removeCmd.id) {
                        log("Remove GUI missing id");
                        break;
                    }

                    // find the GUI item to remove.
                    final Item guiItem = _guiMap.get(removeCmd.id);
                    if (null == guiItem) {
                        // No item to remove.
                        break;
                    }

                    ItemSource.disposeItem(guiItem);

                    // Remove it from the GUI item list.
                    _guiMap.remove(removeCmd.id);
                } else {
                    // Unrecognized category.
                    log("Remove from unrecognized category: " + removeCmd.category);
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
