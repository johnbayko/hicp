package hicp_client.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hicp.MessageExchange;
import hicp.message.command.CommandInfo;
import hicp.message.command.GUIInfo;
import hicp_client.gui.ContainerItem;
import hicp_client.gui.Item;
import hicp_client.gui.ItemSource;
import hicp_client.gui.RootItem;
import hicp_client.text.TextLibrary;

public class GUIController {
    private static final Logger LOGGER =
        Logger.getLogger( GUIController.class.getName() );

    protected MessageExchange _messageExchange;

    protected TextLibrary _textLibrary = new TextLibrary();
    protected Map<String, Item> _guiMap = new HashMap<>();

    protected RootItem _root = null;

    public GUIController(final MessageExchange messageExchange) {
        _messageExchange = messageExchange;

        _root = new RootItem();
    }

    public GUIController dispose() {
        _messageExchange = null;

        // Dispose of any opened GUI objects.
        _root.dispose();
        _guiMap.clear();

        return this;
    }

    // TODO replace CommandInfo with ItemInfo.
    public GUIController receivedCommand(final CommandInfo commandInfo) {
        switch (commandInfo.command) {
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
                            LOGGER.log(Level.FINE, "Add text missing id");
                            break;
                        }
                        _textLibrary.update(commandInfo);
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
                                    commandInfo, _textLibrary, _messageExchange
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
                    LOGGER.log(Level.FINE, "Modify without category");
                    break;
                }

                switch (itemInfo.category) {
                  case TEXT:
                    {
                        // Must have id field.
                        if (null == itemInfo.id) {
                            LOGGER.log(Level.FINE, "Modify text missing id");
                            break;
                        }
                        _textLibrary.update(commandInfo);
                    }
                    break;
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
                        guiItem.modify(commandInfo);
                    }
                    break;
                  default:
                    // Unrecognized category.
                    LOGGER.log(Level.FINE, "Add to unrecognized category: " + itemInfo.category.name);
                }
            }
            break;
          case REMOVE:
            {
                final var itemInfo = commandInfo.getItemInfo();
                if (null == itemInfo.category) {
                    // No category, ignore incomplete message.
                    LOGGER.log(Level.FINE, "Remove without category");
                    break;
                }

                switch (itemInfo.category) {
                  case TEXT:
                    {
                        // Must have id field.
                        if (null == itemInfo.id) {
                            LOGGER.log(Level.FINE, "Remove text missing id");
                            break;
                        }
                        _textLibrary.remove(itemInfo.id);
                    }
                    break;
                  case GUI:
                    {
                        LOGGER.log(Level.FINE, "Remove GUI");
                        // Must have id field.
                        if (null == itemInfo.id) {
                            LOGGER.log(Level.FINE, "Remove GUI missing id");
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
                    LOGGER.log(Level.FINE, "Remove from unrecognized category: " + itemInfo.category.name);
                    break;
                }
            }
            break;
        }
        return this;
    }
}
