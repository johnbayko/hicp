=========================================================================
An informal description of the Holistic Interface Control Protocol (HICP).
=========================================================================

Introduction
------------

This document is not a formal standard, it just describes what I did for
anyone interested.

Intent and scope of the protocol
--------------------------------

The Holistic Interface Control Protocol, or HICP is a protocol similar
in intent to TELNET, between a user agent and a server. It sends
commands (the conventional term for ANSI terminal escape sequences) from
a server application to a user agent, and events from the user agent to
the server. The commands from the server generally define a user
interface, and transfers data to be presented or edited.  The events
from the user agent generally represent actions by the user such as
mouse clicks or keystrokes, but can be generated in response to commands
from the server, or from other sources.

  Q: How is HICP pronounced?

  A: If you're asking if you can pronounce it with a "u" in the second
  syllable, go ahead. I pronounce the letters.

  Q: Why "Holistic"?

  A: After a long, unpredictable, and improbably long path, it ends up
  being a tribute to the Douglas Adams book "Dirk Gently's Holistic Detective
  Agency".

The protocol is an "unreliable" protocol, in the sense that in most
cases, the user agent is under no obligation to do anything with the
commands it receives, and the application is under no obligation to do
anything about the events it receives.  This is why messages which
normally generate a response are still referred to as commands and
events, rather than requests and responses (which would imply a
relationship between the two). There are a few exceptions related to
handshaking during starting up.

  Q: What good is an unreliable protocol?

  A: "Unreliable" doesn't mean useless. The ANSI terminal protocol is also
  unreliable in the same sense that the application can't guarantee that
  characters sent out ever do anything, but in practice they do what is
  expected, and so have provided decades of effective user interfaces.
  Errors are detected and resolved manually. It also allows some backward
  compatibility tricks for non-standard extensions, as described below.

The protocol describes only the communication over an existing serial
connection, and doesn't cover the establishing of such a connection. It
can be an encrypted TCP socket, or an RS-232 serial link, the only requirement
is that it must support the transmission of 8 bit bytes.

It is undefined what happens when a connection is closed, however the
server is under no obligation to provide support for perserving sessions
between connections, though individual applications might.

I did a web search and found that this idea has also been described in
the proposal for XUP - Extensible User Interface Protocol, submitted by
MartSoft Corporation to the W3C in 2002. XUP is a SOAP-based protocol
using existing UI descriptions (XUP or XAML) for the same purpose as
HICP. It's more complex than I'm interested in. See:

http://www.w3.org/TR/2002/NOTE-xup-20020528/

Newer:

http://www.martsoft.com/xup/v2-spec/

This is also funny - and a distant, indirect inspiration:

telnet to 193.202.115.241

Format of messages
------------------

Messages are JSON format.

  Q: Wouldn't an all-binary format be more efficient?

  A: Binary messages still need martialling and byte re-ordering, the
  difference isn't that great. I also think there are advantages to having
  a human-readable message format.

Framing protocol
----------------

[I thought this would be useful for something, but I don't think so now.
It can be deleted]

A framing protocol is optional, and may be used for unreliable
transmission media such as an RS-232 serial connection. This section
describes a proposal - maybe I'll change my mind before it's implemented
and tested.

If an expected start of message begins with a frame start character
sequence, then the message is received as framed data. If one side
starts listening to a connection in the middle of a message, data which
looks like a frame start sequence may be detected, so there must be a
pause after initial connection before a frame start sequence is
considered valid, and it must be the first characters received.

There may be some edge cases unaddressed, but really, this will likely
only ever be used for an RS-232 connection, in which the sequence is:

- Make the physical connection, while the server is running and
  listening.

- Start the user agent. The user agent will send a CONNECT event
  in a frame because the user selected a serial port as a connection,
  or clicked on a "Use frames" checkbox.

- Server will respond with messages in frames.

Any other sequence of making a connection can't really be expected to
work, so just be grateful if it does.

A frame may contain one message, or part of a message, or be empty.
However a frame may not contain more than one message, or parts of more
than one message (e.g. tail of one message and beginning of the next).
This is to prevent the sequence SYN STX in a binary data block from
being mistaken for a frame header.

Frames use the following ASCII control characters::

  STX 0x02
  ETX 0x03
  EOT 0x04
  ENQ 0x05
  SYN 0x16

A frame begins with the sequence SYN STX, but the sequences SYN SYN STX
or SYN STX SYN are not valid frame beginnings.

The contents of the frame terminate with one of the sequences::

  ENQ ENQ
  ETX ETX
  EOT EOT

Any bytes within the frame matching the frame control characters must be
escaped by inserting a SYN immediately after.

The sequence EOT EOT indicates the end of the frame.

The sequence ENQ ENQ indicates that the receiver must respond with
either an ACK, a NAK, or a sequence of CAN xx xx xx xx:

ACK 0x06
    Frame was received without detected error.

NAK 0x15
    An error was detected. This only applies to the frame data, if
    a message had an incorrect format or other high level error, that
    must not cause a NAK.

CAN 0x18
    Frame was too large. The bytes that follow indicate the
    maximum frame size accepted (most significant byte first). A maximum
    frame size of 0 indicates the respondent doesn't want to reveal the
    maximum frame size for some reason, and a smaller one should be
    tried. It's acceptable for a sender to give up at this point, but
    it's more polite to make an attempt. Of course, if a frame of 1 byte
    is still rejected, there's not any point in trying anything smaller.

The sequence ETX ETX must be followed by a checksum computed by adding
all characters from the initial SYN to the last ETX, inclusive, and
discarding bits above 16. The checksum is sent most significant byte
first. After the checksum, EOT or ENQ is sent to terminate the message.
If ENQ is sent, then response must be sent by the receiver, as above.

If a response is expected and not received, a timeout occurs and is
interpreted as a NAK. The timeout period should not exceed 1/10 of a
second, because really, with processing power available in even low end
electronics these days, it should not take more than 1/10 of a second to
compute a checksum, and there is no reason to use frames over a
long-latency network, which will have its own error correction protocol.

A diagram describing this protocol follows.

::

       <Start>
          |
         SYN  <- Ignore if SYN SYN STX or SYN STX SYN
         STX
          |
          :
  Data -> :  <- Insert SYN following: STX ETX EOT ENQ SYN
          :
      ____|____
     /    |    \
   EOT   ENQ   ETX
   EOT   ENQ   ETX
    |     |   CK-Lo
    |     |   CK-Hi
    |     |    / \
    |     |<-ENQ EOT
    |     |       |
    |     |       |
    |   [ACK/     |
    |    NAK/     |
    |    CAN      |
    |  response]  |
     \____|______/
          |
        <End>

Description of the user agent
-----------------------------

The user agent has different categories where items can be added -
libraries for text, images, audio or video clips, and possibly other media in
the future, and the user interface root. Items in these areas are identified
by integers included in commands from the server, and a new item added
with the same identifier as an existing item replaces the existing one.

  Q: Why not identify items by strings so people can read them?

  A: Numbers can be represented by symbolic names (constants,
  enumerations) in source code. Doing this gives compile-time or run-time
  checks against typos.

  Q: Why not replace header tags and keywords with integers?

  A: I'm tempted, but I think that's going too far. Those don't change,
  and are mostly hidden from application developers, so there's less
  chance that they will be misspelled.

"Windows" (could actually be a screen, tab, or other top level component) can
be added to the user interface. Other user interface items
can be added to windows, or to panels within the windows. User interface
item labels never contain literal text or images, only references to
items in libraries (data such as text to be edited is specified
directly).

  Q: Why not allow items like labels to specify the text to display
  instead of an ID number?

  A: Ask me that again in Spanish.

  Q: What about images?

  A: Images may also contain text or localization (e.g. traffic signs).
  It's more efficient to refer to an image from a pre-loaded image library
  for a control than re-send the image each time that control is added.

Relative positions are "above", "below", "before", and "after". In some
languages, "before" would be left, in others it would be right. The
server can send a message with a direction preference, which is a hint
for displaying text direction as well as for panel layout. Horizontal
grid positions start (position "0") on the "before" side of a panel,
window, etc. 

Helpful hint: If you have a user agent that supports non-standard
components used by an application, but want standard user agents to also
work, you can add a standard component (say, a button to open up a
window to provide a different interface for editing, or a label telling
the user to upgrade the software), then replace it with the non-standard
component. User agent software which doesn't recognize the second
component will ignore the new message and leave the original component,
user agents which do will replace it with the new component.

The user agent is not required to include password or host name/IP
address management, but may as a convenience. The server can refuse to
support a user agent which doesn't provide authentication.

Messages
--------
::

    {
        "message" : <message type>,
        <message type> : {
            ...
        }
    }

  Q: Isn't this kind of redundant? Shouldn't the message body be something like
  "body"?

  A: The intent is that the name matches the content, so if you get the
  structure for "command", you always know what fields will be in it (excepting
  incorrect messages).

Required fields
+++++++++++++++

"message"
  <message type> can be:

  "command"
    Command to request the user agent do something.

  "event"
    Event generated by the user agent, generally by a user action. This means
    events have a single category (implicitly "gui") which is not specified in
    the event message.

Command messages
----------------
::

    "command" : {
        "command" : <command type>,
        ...
    }

Required fields
+++++++++++++++

"command"
  <command type> can be:

  "authenticste"
    Request additional identifying information if wanted.

  "disconnect"
    Indicates that the application has finished, and no other events will be
    recognized until a "connect" event is received.

  "add"
    Defines an item to add to a category.

  "modify"
    Modifies an item in a category.

  "remove"
    Remove an item from a category.

Authenticate
------------
::

    "command" : "authenticste",
    "authenticate" {
        "method" : [
            <method list>
        ],
        "password" : <password info>
    }

When a new connection occurs, normally the user agent sends a "conect" event
first. The server may respond with an "authenticate" command to request
additional identifying information.

The user agent must always respond with an "authenticate" or "disconnect"
event. If the user agent sends an event other than "authenticate" or
"disconnect", that message is discarded by the server and another
"authenticate" command is sent. Extra "authenticate" events must be ignored by
the server.

The user agent may retrieve authentication information from a file, directory,
database, etc., or may present a dialog to the user, or may send an
"authenticate" event without any additional information. If authentication
information is not present, the application may still accept the connection and
begin with a login or sign-up window, but this depends on the application.

It is not permitted for the user agent to send an "authenticate" event before
the "authenticate" command (indicating authentication methods acceptable to
the server), because this is a potential security lapse. The server must
respond with a "disconnect" message if this happens.

Required fields
+++++++++++++++

"method":
  A list of authentication methods that can be used. <method list> may include:

  "plain"
    Simple username and unencrypted password is sent by the
    user agent in an AUTHENTICATE event.
  
  Additional authentication methods may be added later, but the user
  agent is not required to support any other than "plain".

  The server should always include "plain" unless it believes the
  connection is insecure and unencrypted data should not be
  transmitted. If the user agent doesn't support any method in the list,
  it will respond with a "disconnect" event.

Optional fields
+++++++++++++++

"password":
  This can contain a "seed" or some other data used in an authentication. The
  exact meaning depends on the authentication method used.

  This field is not used for "plain" authentication.

Disconnect
----------
::

    "command" : "disconnect"

A "disconnect" command indicates that the application has finished, and no
other events will be recognized until a "connect" event is received. What to do
as a result of a "disconnect" command is up to the user agent, but terminating
the connection (if controlled by the user agent), removing all items from the
libraries and user interface, and displaying a message to the user are all good
options.

If the connection is terminated by the server, it's the equivalent of receiving
a "disconnect" command.

Add
---
::

    "command" : "add",
    "add" : {
        "categry" : <category>,
        "id" : <integer>,
        <category> : {
            ...
        }
    }

An "add" command adds an item to a category, or another item already added to
the category.

Required fields
+++++++++++++++

"category"
  These are the categories something can be added to:

  "gui"
    User interface windows (a.k.a. frames) are added, and other components are
    added to those.

  "text"
    Text strings used for labels in GUI components.

  "image"
    I'll add this later.

"id"
  Every item has an integer identifier. If an existing component has the same
  identifier as an "add" message, it is replaced. If a non-integer is used, the
  user agent must ignore the message.

Add GUI
-------
::

    "gui" : {
        "component" : <component>,
        "parent" : <parent id>,
        "position" : {
            "horizontal" : <integer>,
            "vertical" : <intewger>
        },
        "size" : {
            "horizontal" : <integer>,
            "vertical" : <intewger>
        },
        <component> : {
            ...
        }
    }

Required fields
+++++++++++++++

"component"
  Identifies the type of GUI component being added. The types are:

  "window"
    A window. Windows act like a panel component, but typically
    are moveable, have a title bar, and can contain a menu bar.

  "panel"
    A component which contains and lays out other components based on their
    "size" and "position" attributes.

  "button"
    A button control. It generates a "click" event when activated (mouse click
    with a pointer, finger tap).

  "label"
    A text label. Labels generate no event.

  "selection"
    A list of items that can be selected by the user, and presented in
    different ways.

  "textfield"
    A single line text field (which does not support paragraph structures in
    data) which may be editable. If editable, will generate a "changed" event
    when editing is finished, which is usually when "return", "enter", or
    "escape" is typed, or editing focus changes to some other component.

  "textpanel"
    A multi-line text area which may be editable. If editable, will generate a
    "changed" event when editing is finished, which is usually when "escape"
    is typed, or editing focus changes to some other component.

Optional contained component fields
+++++++++++++++++++++++++++++++++++

A contained component is added to a container such as a window or panel. That's
generally any component that's not a window.

If either a "parent" field is not specified, the component is not added, and
the definition must be retained until all values are added with a "modify"
message.

"parent"
  <[parent ID> is the ID number of the graphical component to add this to. If
  the parent component is not one that can contain other components ("window",
  "panel") then this field can be ignored.

  A component can have only one parent.

  If the parent ID doesn't exist, then the user agent may store the information
  until a component with the matching ID is added, but is not required to and
  can discard the message.

  This field is ignored for windows, all windows implicitly have the GUI
  root as a parent.

"position"
  The position to add the component to the specified parent ID. If the parent
  ID is not specified and the user agent creates the component or saves the
  values for later, the position fields should be included for when the
  component is eventually added to a parent.

  Components are added to a flexible grid, much like an HTML table (or
  Java GridBagLayout). The horizontal axis starts on the same side as
  text direction. The horizontal and vertical fields are cell coordinates. If
  one is missing, the user agent can ignore the message, or assume the missing
  integer is 0, or do something else.
  
  The user agent must support values for a position between 0-255.

    Q: Why a limit at all?

    A: If a limit is not defined here, it will be defined by some
    implementation or another - either explicitly or by the point at which
    it crashes. Better to have a number, even if arbitrary, that guarantees
    uniformity. It can be increased in a future version.

  The server can have no awareness of a window geometry (size or
  position) because these are up to the user agent to determine. The
  user agent may decide the position of the window based on pointer
  position, or based on other window positions such as choosing the
  lowest density, or it may record positions for a given application and
  window ID from last time it was run, and use that. User agents on
  restricted display hardware may choose to display a window as a tab or
  some other format, in which case a "window" has no geometry anyway.

  All components have a minimum physical size (defined by different
  properties, not the number of locations occupied as specified by the
  "size" property). Cells cannot be smaller than the component they
  contain, but may be larger. Alignment is determined by the user agent
  policy. Windows which are smaller than the sum of the components they
  contain should provide a way to scroll the contents, but could just
  truncate the display.

"size"
  Indicates the number of horizontal and vertical layout positions this object
  should occupy. If there is an existing component, the new component's size
  must be truncated truncated to fit the following way:

    - Include the component's row and all rows below until there is
      overlap with the size taken by another component.

    - If there are no complete vertical rows available, then use the
      current row and all horizontal cells until there is overlap
      with the size taken by another component. There will be at least
      1 cell because any component at the location is replaced.

    - If horizontal size was adjusted, expand vertical size to include
      all vertical rows where there is no overlap, using the new
      horizontal size.

  Conversely, if this component overlaps the size of an existing component,
  that component must be truncated in the same way.

    Q: I'm not sure that looks good.

    A: To make it look good, the components shouldn't overlap in the first
    place. This is just to handle the situation if they do. These rules
    ensure that the components are truncated the same way no matter which
    order they are added, while still being functional.
    
  If a component is later removed, any components which were truncated because
  of it should be expanded to the new available limits.

  The value "0" indicates the component should take as much space as
  is available to it in that direction, without expanding the window
  or truncating any components with a defined size (expanded
  components of size "0" will truncate other expanded components of
  size "0"). If the both values are "0", the component should fill the
  space horizontally first, then fill as many empty rows of that size
  below it as are available.  However a user agent does have the
  option of treating "0" as "1", if the implementor is a wimp or is
  using a wimpy GUI toolkit.

  One way to implement this is to calculate the size of all components
  with a defined size (treating "0" as "1" for this step), then
  expanding all the "0" sizes of the components in a separate step.

  The default size is "0,0".


Add GUI window
--------------
::

    "window" : {
        "text" : <text id>,
        "text-direction" : {
            "first" : <direction>,
            "second" : <direction>
        },
        "visible" : [ "true" | "false" ]
    }

Optional fields
+++++++++++++++

"text"
  If specified, <text id> is used to select text to display in the title bar.
  The user agent can choose a default, such as application name and window ID.

"text-direction"
  Indicates the direction text should be displayed and components should be
  laid out. <direction> may be one of:
    
    - "left"
    - "right"
    - "up"
    - "down"
    
  "first" is direction per line. Default is from the containing component
  (panel or window), or from the user agent or system configuration, or "right"
  if there is none. "second" indicates how text lines wrap if used, default is
  the same as "first", except for "down" instead of "right".

  The user agent must apply this to components when they are added, and may
  adjust the layout of components already added.
    
  Common combinations (The Java ComponentOrientation class documentation lists
  these) are:
    
  "right, down":
    English, Russian

  "left, down":
    Hebrew, Arabic

  "down, left":
    Chinese, Japanese

  "down, right":
    Apparently some Mongolian languages.

  When components are added, only the horizontal direction is used - vertical
  positions will always start at the top and increase downward. Components
  within a cell should be aligned towards the leading horizontal edge,
  vertially centered.

  User agents may have their own algorithm for accurately deciding on how to
  display text direction horizontally. This does not override that, but user
  agents which don't have this ability should use this to determine proper text
  direction. In that case it's expected that text for each single label will be
  a single direction.

  This may also be used to display text in an alternative or unusual
  orientation, such as horizonatal Chinese or vertical English labels for the
  vertical axis of a graph.  The text may be displayed as normal characters
  arranged in a different direction, or as horizontal text rotated 90 degrees
  (this may also depend on the language - Japanese cannot be rotated, and
  Arabic cannot be displayed as separate characters arranged vertically).

    Q: Why can't I specify the text direction of a label?

    A: The panel needs to know the text direction in order to align the label
    properly (left/right, up/down). You can add a panel into another panel's
    cell, set text direction there, then add a label to it.

"visible"
  Controls whether a window is visible or not. Default is "false", normally a
  window is added, then components are added to the window, and finally the
  window is made visible. If a window is set to visible when it is added, the
  user may see the display change as components are added in following
  messages.

  If a window is already visible but behind other windows, it may be moved on
  top of other windows.

Add GUI panel
-------------
::

    "panel" : {
        "text" : <text id>,
        "text-direction" : {
            "first" : <direction>,
            "second" : <direction>
        },
    }

Optional fields
+++++++++++++++

"text"
  If specified, <text id> is used to select text to displayed as part of a
  border around the panel. No border is displayed if there is no text
  specified.

"text-direction"
  This is the same as for "window".

Add GUI button
--------------
::

    "button" : {
        "events" : <events>,
        "text" : <text id>
    }

Optional fields
+++++++++++++++

"events"
  Indicates whether the button will generate events. <events> can be:

  "enabled":
    The user agent will generate events when the component is clicked. This is
    the default

  "disabled":
    The user agent will not generate events.

"text"
  If specified, <text id> is used to select text for the button label. The
  default is an empty string "".

Buttons may display a text label or an image. The user agent may support
displaying both at the same time, but does not need to, and can decide which to
display (it may be a user configurable option).

If a button can display both, then the text can be displayed either above or
below the image [I'll define that later]. I prefer below as a default.


Add GUI label
-------------
::

    "label" : {
        "text" : <text id>
    }

Optional fields
+++++++++++++++

"text"
  If specified, <text id> is used to select text displayed by the label.
  The default is an empty string "".

  The user agent decides how to display the text if there is not enough room.
  For example, it may truncate the text, or increase the vertical size and wrap
  text to the next line, if space is available.  However, the user agent does
  not need to support control characters (tab, newline, etc) and can filter
  them out.

  The user agent can also decide whether labels can be copied to a system
  clipboard.


Add GUI selection
-----------------
::

    "selection" : {
        "items" : [
            ...
        ],
        "selected" : [
            ...
        ],
        "events" : <events>,
        "mode" : <mode>,
        "presentation" : <presentation>,
        "height" : <height>,
        "width" : <width>
    }

Required fields
+++++++++++++++

"items"
  See section below.

Optional fields
+++++++++++++++

"events"
  Indicates the selection behaviour of items. <events> can be:

  "enabled":
    The user agent will allow the user to select or unselect an item, and
    will generate events for each. This is the default.

  "disabled":
    The user agent will not allow the user to select or unselect an item.
    Selected items should remain selected.

  "unselect":
    The user agent will only allow the user to unselect an item, and will
    generate an event when that is done.

"mode"
  Indicates the selection mode of the list. <mode> can be:

  "single":
    Selecting an item in a single selection list unselects any other selected
    item.

  "multiple":
    Any number of items can be selected in the list. This is the default.

"presentation"
  Indicates how the list of items should be presented for user selection.
  <presentation> can be:

  "scroll":
    All items are displayed in a list in which they can be selected or
    unselected by clicking on them. The items can be scrolled if there are
    too many for the list height ("height" header). This is the default for
    multiple selection.

  "toggle":
    Items are displayed as individual items on a panel which the user agent
    determins based on the GUI style and other attributes, such as single /
    multi selection or scroll height settings, That could be check boxes,
    switches, or radio buttons (normally arranged vertically, not
    scrollable).

  "dropdown":
    For single selection lists only (selection mode is
    ignored), a dropdown tool presents the available
    items for the user to select, with only the selected item visible when
    not being changed. This is the default for single selection.

"selected"
  See section below.

"width"
  For "toggle" presentation, <width> is a positive integet indicating how many
  columns are desired. Items are arranged vertically, and wrapped to the next
  column. Default is 1.

  This is a suggestion, and can be adjusted by the user agent, e.g if height is
  smaller than the number of items and the user agent wants to avoid scrolling
  this can be increased.

"height"
  <height> is a positive integer that modifies how "scroll" or "toggle"
  presentation is displayed, or the number of items displayed in a dropdown
  list without scrolling when selecting one.

  This is a suggestion, and can be adjusted by the user agent if it's large
  (e.g. past end of display) or small (e.g. minimum of 3).


Add GUI selection items
-----------------------
::

    "items" : [
        {
            "id" : <item id>,
            "text" : <text id>,
            "events" : <events>
        },
        ...
    ]

A list of items that can be selected. Items consist of an item ID and a text ID
for display. The user agent must be able to display 255 items.

Required fields
+++++++++++++++

"id"
  <item id> is an integer unique to the selection component identifying each
  item that is selected or unselected. Items should be displayed in the order
  of the item ID.

"text"
  <text id> is an integer identifying a text item to display.

Optional fields
+++++++++++++++

"events"
  Indicates whether this item can be selected or unselected. <events> can be:

  "enabled"
    This item can be selected or unselected. This is the default.

  "disabled"
    This item cannot be selected. In single selection mode it will be
    unselected when another item is selected, but in multiple selection mode it
    will remain selected and can't be unselected by the user.

Add GUI selection selected
--------------------------
::

    "selected" : [
        <item id>,
        ...
    ]

A list of items IDs, or empty to select no items. Any item not in the list is
unselected. Any ID not in the "items" list must be ignored.

If multiple items are in the list but the mode is single selection, the user
agent can decide how to deal with it in a way that makes sense (e.g. first,
last, highest, lowest, etc., but probably not changing the mode to "multiple").

If there is no selection but the presentation is "dropdown" (which normally
does not allow no selection), it's up to the user agent to decide how to handle
it (e.g. select 0 by default), and to communicate this to the server with a
"changed" message.

When an item is selected or unselected, the current list of items is sent as a
changed event so the server does not have to keep track of what's been added or
removed, in the same way a text change imply sends the current text.


Add GUI textfield
-----------------
::

    "textfield" : {
        "content" : {
            ...
        }
        "attributes" : [
            ...
        ],
        "events" : <events>,
        "width" : <width>
    }

Text content and text attributes are specified with separate fields ("content",
"attribute", and "structure"). An end-of-line sequence (or other characters)
within the content is not valid for representing a paragraph break.

Optional fields
+++++++++++++++

"attributes"
  See section below.

"content"
  See section below.

"events"
  Indicates the editing behaviour of the component. <events> can be:

  "enabled":
    The user agent will modify the content and attributes from user input,
    and send a changed event when done. This is the default.

  "disabled":
    The user input does not change the content.

    If the user is in the process of editing the content when this is
    received as part of a modify command, the user agent should discard any
    incomplete edits before making the component uneditable.

"width"
  Indicates the desired physical width of the component based on a sample
  string <width> in the default font, size, and other attributes. The default
  width must be at least wide enough to display a single character string with
  the widest character (usually "W" or "M").

  The user agent isn't required to support this.


Add GUI textfield content
-------------------------
::

    "content" : {
        "text" : <text>
    }

If specified, indicates the initial content of the text field.

Optional fields
+++++++++++++++

"text"
  Specifies the text contained by this component. There is no point in a
  content section without a text field, but it's optional to match the
  equivalent "modify" message.

  The <text> is in UTF-8 encoding, and consists only of printable characters,
  no control characters such as CR, LF, TAB, ESC, etc. Unlike a text panel,
  which is multiline, a text field is a single line, so has no "structure"
  field to specify paragraphs or other formatting.

  The user agent must support at lest 32,768 (32K) characters (not bytes) for
  content.

    Q: What should I do if there are control characters?

    A: If the rules aren't followed, there's no guarantee of anything. You
    could filter out the invalid characters, truncate the string, or refuse to
    do anything with them and treate it as an empty string. You must still
    handle the rest of the headers correctly - that means a text component must
    still be added, whatever you decide to do with the content.

Add GUI textfield attributes
----------------------------
::

    "attributes" : [
        {
            "attribute" : <attribute>,
            "position" : <position>,
            "ranges" : [
                {
                    "length" : <integer>,
                    <attribute> : {
                        ...
                    }
                },
                ...
            ]
        },
        ...
    ]

Specifies display attributes for the text contained by the component. Existing
attributes (specifically the user agent defaults) not covered by the attributes
are unchanged.

It's not actually an error to specify an "attributes" section without "content",
but without content to apply it to, it has no effect.

The user agent must store and keep track of the segments of all attributes,
even those it does not recognize, keep them consistent as the content is
edited, and return them with the content (in a "change" event when editing is
complete or a content request command is received). If the user agent adds any
attributes not passed in the "attributes" section, the application is not
obliged to identify or store them. If the attributes are visible, seeing them
disappear could annoy the user, so ought to be avoided.

  Q: Could I set an attribute with length 0, so that no text displays that
  attribute, but text added at that point (say, the end) will have it?

  A: The user agent is free to remove any 0 length attributes and join
  adjacent attributes that are the same value, even those at the end of the
  content, so don't count on it.  Wait for a "changed" event and set the
  attributes then.

The attribute list contains one item for each attribute type specified (e.g
"underline", "size"). Attributes can be binary (on/off) or multivalued.

Required fields
+++++++++++++++

"attribute"
  The attribute name. <attribute> can be:

  "underline"
    Text to be displayed with an underline.

  "italic"
    Text to be displayed in italics.

  "bold"
    Text to be displayed in bold.

  "font"
    Font to use for the text.

  "size"
    Size to use for the text.

"ranges"
  The list of lengths which each attribute value applies. Ranges have these
  fields:

  "length"
    The length of the attribute range. Any length extending past the end of the
    content must be ignored. Any range starting past the end of the content
    must be ignored.

  <attribute>
    Values which ranges for the attribute can have.

    Attributes can be binary or mutivalued, binary attributes have no
    <attributes> field.

    Binary attributes toggle between the default off state and the on state,
    with the first range indicating on. To set the first character to on,
    <position> would be 0. To explicitly set the first character to off instead
    of relying on the default, "position" can be 0 (or omitted) and the first
    (on) range would be 0. For example to set the first 10 characters to not be
    underlined::

        "attributes" : [
            {
                "attribute" : "underline",
                "ranges" : [
                    { "length" : 0 },
                    { "length" : 10 }
                ]
            }
        ]

    Multivalued attributes have values specified in a section with <attribute>
    as the key. If there is no <attribute> section, no changes are made for
    this attribute (basically skip the range):

    "font"
      If there is no "font" field for a "font" range, this indicates the
      default font. Font specification is::

          "font" : {
              "standard" : <standard font>,
              "custom" : <custom font>
          }

      <standard font> is a basic font the user agent must support. They are:

      "serif":
        Proportional serif.

      "sans-serif":
        Proportional sans-serif.

      "serif-fixed":
        Fixed width serif.

      "sans-serif-fixed":
        Fixed width sans-serif.

      Any actual font can be chosen for any of these standard fonts, though
      it should in some way resemble what the user expects.

      <custom font> can be interpreted any way the user agent wants, but is
      most usefully a non-standard font to be used if available. If not
      available, <standard font> must be used.

    "size"
      If there is no "size" field for a "size" range, this indicates the
      default size. Size specification is::

          "size" : {
              "size" : <size>,
              "scale" : <size>
          }

      The default size of a font is whatever the user agent prefers. It may
      take the size from the windowing system default, or it may be defined
      in a configuration file, or some other source. The size attribute
      indicates what amount to scale this default size by.

      <size> is an integer or floating point value. The "size" field
      indicates the value to multiply the default size by, <scale> indicates
      the value to divide by. A scale of 1 and size of 2 (1/2) is the same as
      size of 0.5 and no scale, but size of 2 and scale of 3 (2/3) has no
      equivalent size with no scale. The default for both is 1.

      The displayed size does not need to match the result exactly, and the
      format may be adjusted internally before it is output again in an event
      message - that is, don't expect to get back an exact match to what was
      sent out.

Optional fields
+++++++++++++++

"position"
  <position> is an integer indicating the first character within the content
  affected by the attribute specification. Characters between the start and
  <position> are unchanged. The default is 0.

Add GUI textpanel
-----------------
::

    "textpanel" : {
        "content" : {
            ...
        }
        "attributes" : [
            ...
        ],
        "structure" : [
            ...
        ],
        events: <events>,
        "width" : <width>,
        "height" : <height>
    }

Text content, text attributes, and structure are specified with separate fields
("content", "attribute", and "structure"). An end-of-line sequence (or other
characters) within the content is not valid for representing a paragraph break.

Optional fields
+++++++++++++++

"attributes"
  This is the same as for "textfield".

"content"
  This is the same as for "textfield".

  Content cannot contain any control characters such as CR, LF, TAB, ESC, etc.
  Paragraphs are specified by the "structure" field.

"events"
  This is the same as for "textfield".

"structure"
  See section below.

"width"
  This is the same as for "textfield".

"height"
  <height> is a positive integer indicating the desired physical height of the
  component should be enough to display this many lines of text in the default
  font, size, and other attributes. The default height is "1".

  The user agent isn't required to support this.


Add GUI textpanel structure
---------------------------
::

    "structure" : [
        {
            "section" : <section>,
            "length" : <length>
            <section> {
                ...
            }
        },
        ...
    ]

Specifies header, paragraph, list and other structure for the text contained by
the component.

The user agent has the option of not supporting all of the specified structure
specification, but the specification must be preserved while text is edited.

Reuired fields
++++++++++++++

"length"
  Number of characters in this section.

Optional fields
+++++++++++++++

"section"
  Indicates the type of section. If no section is specified, this length is a
  placeholder and no changes should be made to the document structure in this
  section. <section> can be:

  "header"
    Indicates this is a header. "header" specification is::

        "header" : {
            "level" : <level>
        }

    If specified, indicates the header level. This is an arbitrary integer
    starting at 1 for the highest level. The user agent may have a limit on the
    number of levels, but must keep track of at lease 256, and if displayed
    must display at least 9.

  "body"
    Indicates this is a non-header section of text. "body" specification is::

        "body" : {
            "level" : <level>,
            "prefix" : <prefix>,
            "value" : <value>
        }

    If <level> is specified, indicates the indent level and format of some
    prefixes. If not specified, the level is the same as the preceeding
    section.  Level specifier is the same as for "header", but the indentation
    level for sections is independend of header levels.

    If <prefix> is specified, indicates how to modify the format of a section.
    If not specified, the prefix is the same as the preceeding section.
    The user agent does not need to support all prefix specifiers. If it does
    not, it must still track the specifier internally to include in event
    messages, but can display the section in another way. <prefix specifier>
    can be:

      "none"
        Default section format. If no change is wanted, the prefix can normally
        be omitted.

      "indent"
        Indent the first line.

      "text"
        Indicates a definition list item. The text must be specified by the
        "value" specifier. If "value" is missing, the section should be treated
        as a "bullet" prefix. Normally the text is displayed one the first line
        (or more if needed), and the section itself is indented starting on the
        line after.

      "bullet"
        Indicates a bullet list item.

      "number"
        Indicates a numbered list item, starting at 1. If this is a level other
        than 1, normally the prefix of any imcrementing list item with the next
        higher level prior to this is used as a prefix to the number for this
        list item (e.g. second "number" section at level 2 following a level 1
        section prefixed with "3" would start with "3.2", for a previous
        section prefixed with "C" it would start with "C.2", for "III" it would
        sart with "III.2", etc.).

      "uppercase"
        Uses letters "A" to "Z" instead of digita "0" to "9".

      "lowercase"
        Like uppercase, but using lower case letters.

      "upper_roman"
        Indicates a numbered list using upper case roman numerals.

      "lower_roman"
        Indicates a numbered list using lower case roman numerals.

    If <value> is specified, it provides a value to be used for different
    prefix specifiers.

    For prefix of "number", "uppercase", "lowercase", "upper_roman", and
    "lower_roman", <value> is an integer which is decoded into the prefix
    format (e.g. 2 can be "2", "b", "II", etc.). If not specified, <value> is
    the value for the previous seciton incremented by 1.

    For prefix of "text" <value> is just the text to use for the prefix. If not
    speciefied, the same value as the previous section is used.

    A "body" with no fields is valid, and can be omitted. It just means this is
    the start a new paragraph or list item, and if the <value> for the section
    is used then it must be incremented.


Add text
--------
::

    "text" : {
        "text" : <string>
    }

Add a text string to the text library. If an existing text entry has the same
identifier, it is replaced and any GUI component which uses it is updated with
the new text.


Reuired fields
++++++++++++++

"text"
  The string to add to the text category. Text strings contain only
  printable characters. Formatting characters, such as horizontal tab,
  CR, or LF, cannot be included in a text string. If one sentence needs
  to start on a new line, add it as a new string and display it as a new
  label below the first.


Add image
---------


Modify
------
::

    "command" : "modify",
    "modify" : {
        "categry" : <category>,
        "id" : <integer>,
        <category> : {
            ...
        }
    }

Modify is used to change the value of something that has already been
added.

If an attempt is made to modify something that doesn't exist (hasn't
been added), the user agent is free to ignore the message, or add the
item (with defaults for fields not specified), or perform any other
action it feels like (but the user might not like being disconnected,
for example). I'd recommend ignoring the message, since default values
will likely be useless and not actually make the broken application
work, or more seriously may have undesired side-effects (though some
things, like adding text, should be safe).

Required fields
+++++++++++++++

"category"
  Same as for "add".

"id"
  Same as for "add".

Modify GUI
----------
::

    "gui" : {
        "parent" : <parent id>,
        "position" : <position>,
        "size" : <size>,
        <component> : {
            ...
        }
    }

The component to modify is identified by "id" and the category "gui".

Optional contained component fields
+++++++++++++++++++++++++++++++++++

"parent"
  Same as for "add". If the component has already been added to a different
  parent, it must be removed and added to the new one.

"position"
"size"
  Same as for "add". If the component has already been added with a different
  position or size, it must be removewd and added to the parent with the new
  position.


Modify GUI window
-----------------
::

    "window" : {
        "text" : <text id>,
        "text-direction" : {
            "first" : <direction>,
            "second" : <direction>
        },
        "visible" : [ "true" | "false" ]
    }

Optional fields
+++++++++++++++

"text"
  Same as for "add".

"text-direction"
  Same as for "add". If text direction is changed, all components will need to
  be removed and added again.

"visible"
  Same as for "add".

  If a window is already visible but behind other windows, it may be moved on
  top of other windows.


Modify GUI panel
----------------
::

    "panel" : {
        "text" : <text id>,
        "text-direction" : {
            "first" : <direction>,
            "second" : <direction>
        }
    }

Optional fields
+++++++++++++++

"text"
  Same as for "add".

"text-direction"
  Same as for "add". If text direction is changed, all components will need to
  be removed and added again.

Modify GUI button
-----------------
::

    "button" : {
        "events" : <events>
        "text" : <text id>
    }

Optional fields
+++++++++++++++

"events"
  Same as for "add".

"text"
  Same as for "add".

Modify GUI label
----------------
::

    "label" : {
        "text" : <text id>
    }

Optional fields
+++++++++++++++

"text"
  Same as for "add".


Modify GUI selection
--------------------
::

    "selection" : {
        "items" : [
            {
                "id" : <item id>,
                "text" : <text id>,
                "events" : <events>
            },
        ],
        "selected" : [
            ...
        ],
        "events" : <events>
    }

Optional fields
+++++++++++++++

"items"
  A subset of items that were added, identified by "id". "text" and "events"
  replace the items previous values. Items in the list with an unknown "id"
  must be ignored, new items cannot be added.

"selected"
  Same as for "add".

"events"
  Same as for "add".


Modify GUI textfield
--------------------
::

    "textfield" : {
        "content" : {
            ...
        },
        "attributes" : [
            ...
        ],
        events: <events>
    }

Optional fields
+++++++++++++++

"attributes"
  "attributes" format is the same as for "add". When specified with a "content"
  field, the attribute changes are applied after the content changes. The same
  conditions apply here as to "content" when a content is being edited: the
  change command must ignored, and a change event with current values generated
  in response.

  Existing attributes (specifically the user agent defaults when the content is
  first set) not covered by the new attributes are unchanged.

"content"
  See section below.

"events"
  Same as for "add".

Modify GUI textfield content
----------------------------
::

    "content" : {
        "text" : <text>,
        "position" : <position>,
        "length" : <length>,
        "direction" : <direction>
    }

Specifies a change to the content.

When modifying content, if the component is being edited there is no guarantee
the actual content matches the expected content, so the user agent must discard
content changes in that case. In response, the user agent must send a "changed"
event with the last unedited content and attributes to indicate the command was
rejected (a "changed" event at the end of editing can't be relied on because
the editing might be cancelled with no change instead).

  Q. Couldn't there be a list of content changes, rather than requiring one
  message per change?

  A. It could be done that way, but this is intended to match the "content"
  section of the "add" command.

Optional fields
+++++++++++++++

"text"
  If specified, indicates text to insert at the given position. This is the
  same as for the add textfield content field.

  If the new text would exceed the component's capacity, then excess text must
  be deleted based on the <direction>.

"position"
  <position> is an integer from 0 to the length of the existing text,
  indicating where the content change will occur. Any position outside the
  valid range may be be ignored, and no change will be made.

  If not specified, default depends on "direction":

  "after"
    Default position is the start of the text.

  "before"
    Default position is the end of the text.

  When text is added to the content, any corresponding attribute segments are
  incremented. The user agent can decide what to do when inserting text at the
  beginning of an attribute segment, but users will generally expect the
  previous segment to extended, except at the start of the text. For example,
  if the content is "I said n!", with the "n" italicized, then adding an "o"
  immediately after the "n" (producing the text "I said no!") should expand the
  italic segment so that the entire word "no" is italicized. If "But" is added
  before the "I", it should be included in the first attribute range.

"length"
  <length> is any positive integer (1 or larger). If specified, up to <length>
  characters are deleted following <position>, until the end of the existing
  text is reached. This is done before any text is added.

  When text is deleted from the content, the corresponding attribute segment is
  decremented. If the segment length reaches 0, then it is removed, and if the
  value for the segments on either side are the same (always the case for
  binary attributes), then they are merged.

"direction"
  Used when If the new text would exceed the component's capacity to allow the
  new text to be added. <direction> indicates whether text should be deleted
  from the beginning of end of the existing text.

  "after"
    Delete characters at the end of the text. This is the default.

  "before"
    Delete characters at the start of the text. This is to allow updates to be
    added to the end of the text with the remaining content scrollnig back.

  Deleted text may include the text being added. For example, if the text
  starting with "The experience ..."" has space for 2 new characters, and the
  text "amazing" is added to position 3 in the "before" direction, then the new
  text will start with "azing experience ..." (2 characters added, "The"
  replaced, for 5 new characters, the rest deleted).

The fields can be specified relatively independently to insert, replace, or
delete text.  For example:

  - Specify "text" alone to replace the entire text of the component.

  - Specify "text" and "position" to insert new text into the existing text.

  - Specify "text", "length" of 0, and optionally "direction" to insert text at
    beginning or end of the existing text, based on the value of "direction".

  - Specify "position" and "length" to delete text at a specific position.

  - Specify "text", "position", and "length" to replace a length of text.


Modify GUI textpanel
--------------------
::

    "textpanel" : {
        "content" : {
            ...
        },
        "attributes" : [
            ...
        ],
        "structure" : [
            ...
        ],
        events: <events>
    }

Optional fields
+++++++++++++++

"attributes"
  Same as for "add".

"content"
  This is the same as for "textfield".

"structure"
  See section below.

"events"
  Same as for "add".


Modify GUI textpanel structure
------------------------------
::

    "structure" : [
        {
            "position" : <position>,
            "section" : <section>,
            "length" : <length>
            <section> {
                ...
            }
        },
        ...
    ]

Applies changes to the structure of the content. Unlike the "add" command,
structure changes are applied to specific sections, they don't start at 0.

This generally follows the rules of the "attributes" section with regards to
the component's content, specifically when content modifications are part of
the command, and when content is being edited. Similarly, content structure not
covered by the new structure specification is unchanged.


Reuired fields
++++++++++++++

"position"
  <position> is an integer indicating the first character within the content
  affected by the structure specification. Characters between the start and
  <position> are unchanged. The default is 0.

"length"
  Same as for "add".

Optional fields
+++++++++++++++

"structure"
  Same as for "add".


Modify text
-----------
::

    "text" : {
        "text" : <string>
    }

If the text ID doesn't exist, the user agent may add it. In this case "modify"
and "add" commands effectively do the same thing.

Reuired fields
++++++++++++++

"text"
  Same as for "add".


Modify image
------------


Remove
------
::

    "command" : "remove",
    "remove" : {
        "categry" : <category>,
        "id" : <integer>
    }

A "remove" command removes and disposes of an item from a category. After being
removed, an item cannot be added to another component.

If some complex item has been constructed and you really don't want to displose
of it, you could add it to a non-visible window until you need it again.

Required fields
+++++++++++++++

"category"
  These are the categories something can be removed from:

  "gui"
    When removing a GUI item, The parent ids of any items added to it are
    cleared, but the items themselves are still defined, and can be added to
    another item.

  "text"
    When removing a text item, if that item is being displayed by
    a GUI item, then it should be replaced by an empty string "",
    retaining the same ID.

  "image"
    I'll add this later.

"id"
  This is the ID of the item to be removed.


Event messages
--------------
::

    "event" : {
        "event" : <event type>,
        <event type> : {
        }
    }

Required fields
+++++++++++++++

"event"
  <event type> can be:

  "connect"
    When a new connection is detected, a "connect" event is sent to the server
    (normally the user agent sends the first message).

  "authenticate"
    An "authenticate" event must always be sent in response to a "authenticate"
    command received from the server.

  "disconnect"
    The user agent will not respond to any further commands except for
    "connect". Can be user initiated, due to some other user agent condition,
    or in response to a "disconnect" command.

  "close"
    Generated by a closable component (currently only windows), indicating the
    user wants it to be closed.

  "click"
    Event generated when a component is activated by a user action - most
    simply, clicking on it, but could be applied to something else.

  "changed"
    Event generated by a component which contains data when the state or
    content of that data has been changed by the user agent (changes from
    server commands do not generate "changed" events because the app making the
    change is aware of the change, and can process it at that time). For
    example, a checkbox state or the text in a textfield.

Connect
-------
::

    "event" : "connect",
    "connect" : {
        "application" : <application name>
    }

If the server requires authentication, it may send an "authenticate" command,
otherwise the server must begin its task, which usually includes sending
commands to construct the user interface. The server may chose to present a
login window instead of sending an "authenticate" command - from the user
agent's point of view, this is equivalent to no authentication.

Optional fields
+++++++++++++++

"application"
  This may be used by the server to identify an application to start up,
  turning the connection over to it until the application terminates. Or
  it may be ignored.

Autheticate
-----------
::

    "event" : "authenticate",
    "authenticate" : {
        "method" : <method>
        <method> : {
            ...
        }
    }

Upon receiving a "authenicate" event, the server must begin its task, which
usually includes sending commands to construct the user interface, but it may
send a "disconnect" if it doesn't like the authentication information.

The application may send an "authenticate" command after the user agent has
already sent a "authenticate" event, in which case the user agent must send
another "authenticate" event with the authentication information if desired.

Required fields
+++++++++++++++

"method"
  Specifies the authentication method the event information is for. <method>
  can be:

  "plain"
    This simply sends unencrypted username and password strings. It is
    important to note that unless the connection is secure, this is a security
    risk since the password is passed without encryption.

  [Other methods will be added later]

  The user agent only needs to support "plain" authentication.

Authenticate plain
------------------
::

    "plain" : {
        "user" : <string>,
        "password" : <string>
    }

Required fields
+++++++++++++++

"user"
  The user identifier.

"password"
  The password for the user.


Disconnect
----------

    "event" : "disconnect"

This indicates that the user agent is no longer in a state that the server
application can rely on, and the application should terminate.  The application
is under no obligation to save any data or be graceful in any way, but
terminating the connection (if controlled by the server), is a reasonable
response. If the connection is not terminated, a server must correctly respond
to a subsequent "conect" event.

The server should not respond to any other events between a "disconnect" and
subsequent "conect". If the connection is terminated by the user agent, it's
the equivalent of receiving a "disconnect" event.

Close
-----
::

    "event" : "close"
    "close" : {
        "id" : <id>
    }

Component is determined by <id>. Can be generated by:

"window"
  Some windowing systems have a "close" control on the window title bar, which
  should generate a "close" event for the window, but otherwise do nothing - it
  is the application's job to perform any cleanup and then send commands to
  either hide or remove the window. The user agent can perform whatever actions
  it wants for any other title bar controls (e.g.  minimise, raise, lower,
  etc.).

  There is no expectation of what to do when closing a window, so the
  application may remove the window or make it invisible, open a new window (to
  ask the user to save or confirm, etc.), disconnect, or something else.


Required fields
+++++++++++++++

"id"
  <id> is the GUI ID of the window to be closed.

Click
-----
::

    "event" : "click"
    "click" : {
        "id" : <id>
    }

[Maybe could be replaced by a "changed" event. Leave that for the future to
consider]

Required fields
+++++++++++++++

"id"
  <id> is the GUI ID of the component which was activated.

Changed
-------
::

    "event" : "changed"
    "changed" : {
        "id" : <id>
        <component> : {
            ...
        }
    }

Required fields
+++++++++++++++

"id"
  <id> is the GUI ID of the component which has changed. <component> is
  determined by <id>.

Changed selection
-----------------
::

    "selection" : {
        "selected" : [
            ...
        ]
    }

A selection event is sent each time one or more items are selected or
unselected.

Required fields
+++++++++++++++

"selected"
  Same as for "Add GUI selection selected".

  If selection mode is "single" and multiple elements are in the list, the one
  with the lowest ID is taken as the selected item.

Changed textfield
-----------------
::

    "textfield" : {
        "content" : {
            ...
        },
        "attributes" : [
            ....
        ]
    }

Text is considered changed when it is no longer being actively edited, such as
if the cursor is moved to another text field (or focus moves away from this
component), not only from a terminating action such as hitting the "return"
key.

The event includes the results of all changes to content and attributes, not
individual changes.

Required fields
+++++++++++++++

"content"
  Same as "Add GUI textfield content".

Optional fields
+++++++++++++++

"attributes"
  Same as "Add GUI textfield content".


Changed textpanel
-----------------
::

    "textpanel" : {
        "content" : {
            ...
        },
        "attributes" : [
            ....
        ],
        "structure" : {
            ...
        }
    }

Text is considered changed when it is no longer being actively edited, such as
if the cursor is moved to another text field (or focus moves away from this
component). Unlike a textfield, the return key would normally start a new
paragraph.

The event includes the results of all changes to content, attributes, and
structure, not individual changes.

Required fields
+++++++++++++++

"content"
  Same as "Add GUI textpanel content".

Optional fields
+++++++++++++++

"attributes"
  Same as "Add GUI textpanel content".

"structure"
  Same as "Add GUI textpanel content".

