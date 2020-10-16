package hicp_client;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Pattern;

import hicp.MessageExchange;
import hicp.message.Message;
import hicp.message.command.Add;
import hicp.message.command.Command;
import hicp.message.command.CommandEnum;
import hicp.message.event.Event;

// Main controller for handling HICP communication.
public class Controller
    implements hicp.Controller
{
    private static final Logger LOGGER =
        Logger.getLogger( Controller.class.getName() );

    protected final Session _session;
    protected final Monitor _monitor;

    protected Pattern _commaRegex;
    protected MessageExchange _messageExchange;
    protected boolean _isConnected = false;

    protected Map<String, TextItem> _textMap = new HashMap<>();
    protected Map<String, GUIItem> _guiMap = new HashMap<>();

    protected GUIContainerItem _root = null;

    public Controller(Session session, Monitor monitor)
        throws UnsupportedEncodingException
    {
        session.getClass();
        monitor.getClass();

        _session = session;
        _monitor = monitor;

        _commaRegex = Pattern.compile(",");
        _messageExchange =
            new MessageExchange(_session.in, _session.out, this);

        _root = new GUIRootItem(null);
    }

// Called by owner.
    public Controller connect() {
        if (null == _messageExchange) {
            return this;
        }
        // Send connect message.
        {
            hicp.message.event.Connect connectEvent = Event.CONNECT;
            connectEvent.clear();
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
    public void receivedMessage(Command m) {
        // Action based on message command.
        switch (CommandEnum.getEnum(m.getName())) {
          case AUTHENTICATE:
            {
                final hicp.message.command.Authenticate authenticateCmd =
                    (hicp.message.command.Authenticate)m;
                final hicp.message.event.Authenticate authenticateEvent =
                    Event.AUTHENTICATE;

                authenticateEvent.clear();
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
                    (hicp.message.command.Add)m;
                if (null == addCmd.category) {
                    // No category, ignore incomplete message.
                    LOGGER.log(Level.FINE, "Add without category");
                    break;
                }
                if (addCmd.TEXT.equals(addCmd.category)) {
                    addModifyText(addCmd);
                } else if (addCmd.GUI.equals(addCmd.category)) {
                    // Must have id and component fields.
                    if ((null == addCmd.id) || (null == addCmd.component)) {
                        LOGGER.log(Level.FINE, "Add gui missing id or component");
                        break;
                    }
                    {
                        final GUIItem oldGUIItem =
                            (GUIItem)_guiMap.get(addCmd.id);

                        if (null != oldGUIItem) {
                            // Remove the old one.
                            oldGUIItem.dispose();
                            _guiMap.remove(addCmd.id);
                        }
                    }
                    {
                        final GUIItem guiItem;

                        // Set text.
                        {
                            final TextItem textItem =
                                (null != addCmd.text)
                                    ? (TextItem)_textMap.get(addCmd.text)
                                    : null;

                            guiItem =
                                GUIItem.newGUIItem(
                                    addCmd, textItem, _messageExchange
                                );
                        }

                        if (null != guiItem) {
                            _guiMap.put(addCmd.id, guiItem);

                            // If this should be added to a parent,
                            // determine the parent item and add to it.
                            if (Add.WINDOW.equals(guiItem.component)) {
                                guiItem.setParent(_root);
                            } else {
                                if (null != addCmd.parent) {
                                    final GUIContainerItem parentItem =
                                        (GUIContainerItem)
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
                    (hicp.message.command.Modify)m;
                if (null == modifyCmd.category) {
                    // No category, ignore incomplete message.
                    log("Modify without category");
                    break;
                }

                if (modifyCmd.TEXT.equals(modifyCmd.category)) {
                    addModifyText(modifyCmd);
                } else if (modifyCmd.GUI.equals(modifyCmd.category)) {
                    final GUIItem guiItem;
                    if (null != modifyCmd.id) {
                        // Get GUI item based on id field.
                        guiItem = (GUIItem)_guiMap.get(modifyCmd.id);
                    } else {
                        // No id, modify _root.
                        guiItem = _root;
                    }
                    if (null == guiItem) {
                        // No item to modify.
                        break;
                    }

                    // Text is the relation between items, so is
                    // handled here (not within an item).
                    TextItem textItem = null;
                    if (null != modifyCmd.text) {
                        // Set text.
                        textItem = (TextItem)_textMap.get(modifyCmd.text);
                    }

                    guiItem.modify(modifyCmd, textItem);

                } else {
                    // Unrecognized category.
                    log("Add to unrecognized category: " + modifyCmd.category);
                }
            }
            break;
          case REMOVE:
            {
                final hicp.message.command.Remove removeCmd =
                    (hicp.message.command.Remove)m;
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

                    TextItem textItem = (TextItem)_textMap.get(removeCmd.id);

                    if (null == textItem) {
                        // Nothing found to remove.
                        break;
                    }

                    if (textItem.hasTextListeners()) {
                        // Is used by a GUI item, set this to "".
                        if (false == "".equals(textItem.getText())) {
                            textItem.setText("");
                        }
                    } else {
                        // Can be safely removed.
                        _textMap.remove(removeCmd.id);
                    }
                } else if (removeCmd.GUI.equals(removeCmd.category)) {
                    log("Remove GUI");
                    // Must have id field.
                    if (null == removeCmd.id) {
                        log("Remove GUI missing id");
                        break;
                    }

                    // find the GUI item to remove.
                    final GUIItem guiItem = (GUIItem)_guiMap.get(removeCmd.id);
                    if (null == guiItem) {
                        // No item to remove.
                        break;
                    }

                    guiItem.dispose();

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
          default:
            log("Unrecognized command in message: " + m.getName());
            break;
        }
    }

    protected Controller addModifyText(hicp.message.command.AddModify addModifyCmd) {
        // Must have id and text fields.
        if ((null == addModifyCmd.id) || (null == addModifyCmd.text)) {
            log("Add text missing id or text");
            return this;
        }
        try {
            TextItem textItem = (TextItem)_textMap.get(addModifyCmd.id);

            if (null != textItem) {
                textItem.setText(addModifyCmd.text);
            } else {
                final int id = Integer.parseInt(addModifyCmd.id);
                textItem = new TextItem(id, addModifyCmd.id, addModifyCmd.text);
                _textMap.put(addModifyCmd.id, textItem);
            }
        } catch (NumberFormatException ex) {
            // Not an integer ID, ignore message.
            return this;
        }
        return this;
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
