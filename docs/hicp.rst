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

Messages consist of data in the form of modified RFC822 style headers,
and data sections, terminated with <CR><LF> combination (although since there's
no body, they're really message fields, not headers, but I'm keeping the
terminology to reduce confusion for people familiar with RFC822 and similar
things). The headers differ from RFC822 in the following ways:

- The header name is encoded as US-ASCII, while header field following the
  colon is any sequence of bytes, terminated by an end-of-line sequence. The
  header name must be composed of printable characters.

- Headers may only appear once. If a header appears more than once, the result
  is undefined (usually first or last value is used).

- Header folding (using a <CR><LF>< 1-or-more-space-or-tab > sequence to denote
  a single <space>) is not supported. Any data containing an end-of-line
  sequence will be specified as a data section (below).

  This means that all characters between the ": " sequence and
  end-of-line are part of the header field. Spaces are not trimmed in
  any way.

- Messages may have one or more data sections rather than a message body. A
  data section header name is terminated by a double colon and space ":: "
  rather than the single colon and space of a simple header.  The tag which
  follows includes the termination criterion, which can be a byte count
  ("length=" and an integer in text form, which can all be treated as US-ASCII
  characters), or byte sequence ("boundary=" and a number of bytes which may
  have any value apart from end-of-line), both followed by end-of-line. This
  ensures that there is no unterminated data block in a message.

  A length delimited block contains all and only the number of bytes following
  the end-of-line, but an additional end-of-line sequence is required, and
  discarded, for human readability. For example:: 

    dataheader:: length=29
    Hi, this is 29 bytes of data.

    next_message_header: abcd
    
  A terminating byte sequence must include at least one byte, so there is a
  special case if the "=" is immediately followed by an end-of-line sequence,
  the bytes (including the end-of-line sequence) up to (and including) the next
  end-of-line sequence are the terminating byte sequence. The use for this is
  shown below.
    
  Except for the word "boundary", this is not at all similar to MIME encoding
  (as described in RFC1521), so stop thinking about that. For one thing, the
  boundary is only for a single block, and there is no "--" prefix or suffix
  used since there is no multipart data.  However, a useful convention when
  using "boundary" termination criteria is to use an end-of-line plus "--" byte
  sequence::

    dataheader:: boundary=
    --
    Hi, this is a bunch of data.
    --

  The data consists of the string without the end-of-line characters ("Hi, this
  is a bunch of data."), since the end-of-line is part of the termination
  sequence ( [CR, LF, '-', '-'] boundary, and [CR, LF] end of line).

  The data block may be any data and is interpreted by context, such
  as image or audio format.

  As a human-readability courtesy, data blocks must be followed by an
  end-of-line sequence, before the next header.

  If a data block is ended by "boundary", then within a data block the escape
  character ESC (0x1B) indicates that the next character cannot be used as a
  matching character to the terminating sequence, or is a single escape, and
  the initial escape character is discarded. This means that the terminating
  sequence::

    'X', 'X', 'X'

  will not match the data sequences::

    ESC, 'X', 'X', 'X', 'Y'
    'X', ESC, 'X', 'X', 'Y'

  Those data sequences will be received as::

    'X', 'X', 'X', 'Y'

  The terminating sequence will match::

    'X', 'X', 'X', 'Y'
    'X', 'X', 'X', ESC, 'Y'

  In which case the EXC or 'Y' will not be part of the data block
  (this would also be an error, as an end-of-line sequence is required
  there). The sequence::

    ESC, ESC

  will be received as::

    ESC

  Maximum length of a data block depends on the data. Data beyond the
  maximum length should be discarded.

A header can always be read as a sequence of bytes terminated by an
end-of-line, then decoded as US-ASCII, UTF-8, or binary parts as
indicated by the header name or value contents.  Data blocks can always
be read as a sequence of bytes terminated by length or by boundary
bytes, followed by an end-of-line.

Messages are terminated by a blank line that is not part of a data
block. 

  Q: Why not UTF-8 field names?

  A: It allows programmers to process the names without being aware of
  Unicode, and treat the rest as data. Years from now, all programmers
  will understand Unicode and its representations, and this won't matter,
  but until then, it's safer this way.

  Q: Why not use XML, JSON, or YAML?

  A: When I started, JSON and YAML didn't exist. XML is complex and best for
  representing hierarchical data. These messages are not hierarchical, so that
  adds nothing but complexity.

  JSON is simpler and unambiguous, so if I were starting over, I'd probably use
  that. but my original thought was to model after the format of email
  messages, with the possibility that existing email processing code might be
  usable. That didn't turn out to be the case.

  Q: But a UI definition is hierarchical, that's why XUL was invented.

  A: This protocol doesn't define a UI, it communicates commands and
  events. Some of those can be used to construct a GUI display, but others
  are unrelated.

  Q: Wouldn't an all-binary format be more efficient?

  A: Binary messages still need martialling and byte re-ordering, the
  difference isn't that great. I also think there are advantages to having
  a human-readable message format.

  Also when I started, there was no standard binary format available. There is
  now, but I still think it's not worth the complexity.

  Q: Binary data in a header value isn't human-readable.

  A: No, but the overall message is. All modern editors can represent
  binary data as symbols, the rest is text. Besides, encoded binary data
  like base64 isn't any more human readable than editor-displayed binary.

  Requiring binary data to be encoded would require handling text (e.g.
  UTF-8 encoded Unicode) differently from binary data. The requirements of
  handing an arbitary block of text are almost the same for handling
  binary data, so it's simpler to treat all data the same.

  The only difference with text is that the terminator can be a known
  non-text sequence and it would not need to be escaped for transmission,
  but this would require the implementing code to make a special case for
  the non-text terminating bytes (e.g. the code can't assume the input
  bytes are all UTF-8 encoded characters), and the code would then need to
  treat the data block as binary data anyway.

  Treating all data as binary simplifies the processing slightly (and
  avoids the overhead of encoding data) at the expense of escaping the
  terminating sequence. The terminating sequence doesn't need to be
  escaped if the data is text and the sequence is non-text, so the result
  in that case is the same anyway.

  Q: You could require the termination sequence to also be text.

  A: Shut up.

The blocks will be treated as the format needed for that header, and may
be specified as encoded if necessary.

Framing protocol
----------------

[I thought this would be useful for something, but I don't think so now.
Delete it]

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

Windows can be added to the user interface. Other user interface items
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

Command and event headers
-------------------------

Headers here are described by "<name>: <field format>", where <name> is
the literal string used for the header name. This is just describing the
headers, not the message format, so don't confuse the ":" with the the header
separator described above.

I'm also being a bit informal about the exact format. Assume there may
be spaces around "," separators or keywords, for example.

Command definitions
-------------------

Every message sent by the server must begin with the "command" field,
followed by the message name. The message name may be any string,
including spaces, but leading and trailing white space is removed. This
means that message names use normal space separated words, never
hyphenated-words, camelCase, or underscore_separated_words.

AUTHENTICATE
++++++++++++

When a new connection occurs, normally the user agent sends a CONNECT
event first. The server may respond with an AUTHENTICATE command to request
additional identifying information.

The user agent must always respond with an AUTHENTICATE or DISCONNECT event. If
the user agent sends an event other than AUTHENTICATE or DISCONNECT, that
message is discarded by the server and another AUTHENTICATE command is sent.
Extra AUTHENTICATE events must be ignored by the server.

The user agent may retrieve authentication information from a file, directory,
database, etc., or may present a dialog to the user, or may send an
AUTHENTICATE event without any additional information. If authentication
information is not present, the application may still accept the connection and
begin with a login or sign-up window, but this depends on the application.

It is not permitted for the user agent to send an AUTHENTICATE event
before the AUTHENTICATE command, (indicating authentication methods
acceptable to the server), because this is a potential security lapse.
The server must respond with a DISCONNECT message if this happens.

Required headers
~~~~~~~~~~~~~~~~

command: "authenticate"

method: <keyword> [ "," <keyword> ]*
  A list of comma and whitespace separated tokens indicating the
  authentication method to use. Tokens may be:

  "plain"
    Simple username and unencrypted password is sent by the
    user agent in an AUTHENTICATE event.
  
  Additional authentication methods may be added later, but the user
  agent is not required to support any other than "plain".

  The server should always include "plain" unless it believes the
  connection is insecure, and unencrypted data should not be
  transmitted. If the user agent doesn't support any method in the list,
  it will respond with a DISCONNECT event.

Optional headers
~~~~~~~~~~~~~~~~

password: <string>
  This can contain a "seed" or some other data used in an
  authentication. The exact meaning depends on the authentication
  method used.

  This field is ignored for the "plain" authentication method.

DISCONNECT
++++++++++

A DISCONNECT command indicates that the application has finished, and no
other events will be recognized until a CONNECT event is received. What
to do as a result of a DISCONNECT command is up to the user agent, but
terminating the connection (if controlled by the user agent), removing
all items from the libraries and user interface, and displaying a
message to the user are all good options.

If the connection is terminated by the server, it's the equivalent of
receiving a DISCONNECT command.

Required headers
~~~~~~~~~~~~~~~~

command: "disconnect"

REDIRECT
++++++++

A REDIRECT command acts like a DISCONNECT command, but includes
information that the user agent can use to make a different connection.

Required headers
~~~~~~~~~~~~~~~~

command: "redirect"

Optional headers
~~~~~~~~~~~~~~~~

Actually, no optional headers per se are defined for the protocol,
because the protocol does not actually handle the establishing of
connections. However, a REDIRECT message may contain any additional
headers which can be interpreted by the user agent as needed, to define
such things as a phone number for modem connection, IPv4 or IPv6
address, domain name, or even serial port number and bit rate if the
connection is through one of several cable links using a protocol like
RS-232.

The user agent can choose to treat a REDIRECT command as a DISCONNECT.

ADD and MODIFY
++++++++++++++

Add is used to add something to something else.

Modify is used to change the value of something that has already been
added.

If an attempt is made to modify something that doesn't exist (hasn't
been added), the user agent is free to ignore the message, or add the
item (with defaults for fields not specified), or perform any other
action it feels like (but the user might not like being disconnected,
for example).  I'd recommend ignoring the message, since default values
will likely be useless and not actually make the broken application
work, or more seriously may have undesired side-effects (though some
things, like adding text, should be safe).

Required headers
~~~~~~~~~~~~~~~~

command: ["add" | "modify"]

category: <keyword>
  These are the categories something can be added to:

  "default"
    Any values not defined in a gui item are taken from the item's parent, or
    if the item has no parent (like a window), the values are taken from this
    "default" item if they have been defined.
    
    Only "modify" messages really make sense for this (the "default"
    item implicitly already exists), but "add" messages can be treated
    like "modify" messages.

  "gui"
    User interface windows (a.k.a. frames) are added, and other components are
    added to those.

  "text"
    Text strings used for labels in GUI components.

  "image"
    I'll add this later.

"gui" "add" required headers 
''''''''''''''''''''''''''''

component: <keyword>
  Identifies the type of GUI component being added. The types are:

  "window"
    A window. Windows act like a panel component, but typically
    are moveable, have a title bar, and can contain a menu bar. Some
    windowing systems have a "close" control on the window title bar,
    which should generate a "close" event for the window, but otherwise
    do nothing - it is the server's job to perform any cleanup and then
    either hide or remove the window. The user agent can perform
    whatever actions it wants for any other title bar controls (e.g.
    minimise, raise, lower, etc.).

  "panel"
    A component which contains and lays out other components based on their
    "size" and "position" attributes.

  "button"
    A button control. It generates a "click" event when activated (mouse click
    with a pointer, finger tap).

    Buttons may display a text label or an image. The user agent may
    support displaying both at the same time, but does not need to, and
    can decide which to display (it may be a user configurable option).

    If a button can display both, then the text can be displayed either
    above or below the image (specified by another header). I prefer
    below as a default.

  "label"
    A text label. Labels generate no event.

    The user agent decides how to display the text if there is not
    enough room. For example, it may truncate the text, or increase the
    vertical size and wrap text to the next line, if space is available.
    However, the user agent does not need to support control characters
    (tab, newline, etc) and can filter them out.

    The user agent can also decide whether labels can be copied to a
    system clipboard.

  "selection"
    A list of items that can be selected by the user, and presented in
    different ways.

    A list of items can have several attributes that affect how they're
    displayed and selected:

    - Mode can be:

      - Single
      - Multiple

      See "mode" header for "selection" component below.

    - Presentation can be:

      - Scroll
      - Toggle
      - Dropdown

      See "presentation" header for "selection" component below.

    - Scroll height can modify how a "Scroll" presentation is displayed, or the
      number of items displayed in a dropdown list when selecting one.

    When an item is selected or unselected, the current list of items is sent
    as a changed event so the server does not have to keep track of what's been
    added or removed, in the same way a text change imply sends the current
    text.

  "textpanel"
    A multi-line text area which may be editable. If editable, will generate a
    "changed" event when editing is finished, which is usually when "return"
    or "enter" is typed, or editing focus changes to some other component.

    Text content and text attributes (both character and paragraph
    structure) are specified with separate headers ("content" and
    "attribute"). An end-of-line sequence (or other characters) within
    the text is not valid for representing a paragraph break.

  "textfield"
    A single line text field (which does not support paragraph structures in
    data) which may be editable. If editable, will generate a "changed" event
    when editing is finished, which is usually when "return" or "enter" is
    typed, or editing focus changes to some other component.

"gui" "add" non-"window" required headers
'''''''''''''''''''''''''''''''''''''''''

position: <integer> "," <integer>
  Components are added to a flexible grid, much like an HTML table (or
  Java GridBagLayout). The horizontal axis starts on the same side as
  text direction. The integers indicate the horizontal, then vertical
  cell coordinates. If one integer is missing, the user agent can ignore
  the message, or assume the missing integer is 0, or do something else.
  Additional integers should be ignored.
  
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

"gui" "add" non-"window" optional headers
'''''''''''''''''''''''''''''''''''''''''

parent: <integer>
  The ID number of the graphical component to add this to. If the parent
  component is not one that can contain other components ("window",
  "panel") then this header can be ignored.

  A component can have only one parent.

  If this header is missing or the parent ID doesn't exist, then the
  user agent should store the information (or create the component
  without adding it), in hopes that a future MODIFY command may add it
  to a real component.

  This field is ignored for windows, all windows implicitly have the GUI
  root as a parent.

mode: <string>
  If specified, this is used by these components:

  "selection"
    Indicates the selection mode of the list. The defined modes are:

    "single":
      Selecting an item in a single selection list unselects any other selected
      item.

    "multiple":
      Any number of items can be selected in the list. This is the default.

presentation: <string>
  If specified, this is used by these components:

  "selection"
    Indicates how the list of items should be presented for user selection. The
    defined presentation methods are:

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

"gui" "add" required or "modify" optional headers
'''''''''''''''''''''''''''''''''''''''''''''''''

id: <integer>
  This is required for all messages except those to set a default value,
  such as text direction.

  The component identifier can be any integer. If an existing component has
  the same identifier as an "add" message, it is replaced.

  If a non-integer is used, the user agent must ignore the message (do
  not use it as a default setting).

"gui" "add" or "modify" optional headers
''''''''''''''''''''''''''''''''''''''''

content: <content change> ":" <content_info>
  If specified, this is used by these components:

  "textfield", "textpanel":
    Specifies changes to the text contained by the component.
    For an "add" command, <content change> can be:

      - "set"

    For a "modify" command, <content change> can be one of:

      - "set"
      - "add"
      - "delete"

    "set":
      Replace any existing content. <content_info> is::

        <text>

      The <text> is in UTF-8 encoding, and consists only of printable
      characters, no control characters such as CR, LF, TAB, ESC, etc.  Line
      breaks are specified for "textpanel" components in the "attributes"
      header.

      The user agent must support at lest 32,768 (32K) characters (not bytes)
      for content of a "textfield" or "textpanel" component.

      Q: What should I do if there are control characters?

      A: If the rules aren't followed, there's no guarantee of anything. You
      could filter out the invalid characters, truncate the string, or refuse
      to do anything with them and treate it as an empty string. You must still
      handle the rest of the headers correctly - that means a text component
      must still be added, whatever you decide to do with the content.

    "add":
      Add text to any existing text. <content_info> is::

        <position> ":" <text>

      <position> is an integer from 0 to the length of the existing text,
      indicating where <text> is to be inserted. Any position outside the valid
      range above may be be ignored, and no text will be added.

      If the new text would exceed the comp[onent's capacity, then text must be
      deleted to make room based on the value of <position>.

      0:
        Delete characters at the end of the text.

      Any other value:
        Delete characters at the start of the text. This may include the text
        being added. For example, if the text starting with "The experience
        ..."" has space for 2 new characters, and the text "amazing" is added
        to position 3, then the new text will start with "ing experience ..."".

      When text is added to the content, the corresponding attribute segment is
      incremented. The user agent can decide what to do when inserting text at
      the beginning of an attribute segment, but users will generally expect
      the previous segment to extended. For example, if the content is "I said
      n!", with the "n" italicized, then adding an "o" immediately after the
      "n" (producing the text "I said no!") should expand the italic segment so
      that the entire word "no" is italicized.

      <text> is the same as for "set".

    "delete":
      Delete text from any existing text. <content_info> is::

        <position> ":" <length>

      <position> is an integer from 0 to length-1 of the existing text.
      <length> is any positive integer (1 or larger). Up to <length> characters
      are deleted following <position>, until the end of the existing text is
      reached.

      When text is deleted from the content, the corresponding attribute
      segment is decremented. If the segment length reaches 0, then it is
      removed, and if the value for the segments on either side are the same
      (always the case for binary attributes), then they are merged.

      Any position or length outside the valid range above may be be ignored,
      and no text will be deleted.

attributes: <attribute specifiers>
  If specified, this is used by these components:

  "textfield", "textpanel":
    Specifies display attributes for the text contained by the component.
    Existing attributes (specifically the user agent defaults when the content
    is first set) not covered by the new attributes are unchanged.

    If the component is editable, then the text contents may be different from
    what is expected, so this should only be part of a "modify" message with a
    content "set" header, or the "events" attribute is "disabled". The user
    agent can ignore an attribute header when the content is not included, or
    the content may have changed by the user. If there is a content "add" or
    "delete" header, the content change is applied before the attributes.

    The user agent must support at lest 32,768 (32K) characters (not
    bytes) for all attributes of a "textfield" or "textpanel" component.

    Not all GUI toolkits which support attributes for text panel also
    support them for text fields, so it's not a good idea to rely on
    that.

      Q: Wait, isn't this the sort of thing XML is for?

      A: Attributes are meant to indicate how text is to be displayed, it is
      not meant as a document model.

      In addition, the user agent has the option of not supporting the given
      attributes. This format allows even attributes which are not recognized
      to be preserved as text is edited.
    
    Attribute specifiers are separated by <end-of-line> sequences - the
    attributes header itself is terminated by an <end-of-line> sequence. This
    makes the string a data block which would be encoded with a boundary
    string, and would look like this::

      attributes:: boundary=
      --
      underline: 0: ...attribute ranges...
      font: 19: ...attribute ranges...
      --

    Attributes can be binary (on/off) or multivalued. Binary attributes
    are specified in this form::

      <attribute> ":" <position> ":" [ <integer> [ "," <integer> ]* ]

    Where <position> is the first character affected, the integers are number
    of characters affected by each change - first on, then off, alternating to
    the last change or the end of the text.  Spaces are ignored.  For example,
    in the string "It's not that far.", to indicate the word "that" (9th
    character) is underlined, the attribute would be specified as::

      underline: 9: 4

    To set attributes for the whole string ::

      underline: 0: 0, 9, 4, 5

    To underline the letter "I" as well as "that"::

      underline: 0: 1, 8, 4, 5

    Multivalued attributes are specified in this form::

      <attribute> ":" <position> ":" <value> "=" <integer> [ "," <value> "=" <integer> ]*

    Where <position> is the first character affected, <value> is the name of
    the particular attribute.  For example, in the string "Press enter to
    continue.", to indicate that "enter" should be fixed width serif, the
    change would be specified as::

      font: 6: serif-fixed=5

    To specify the font style for the whole string as sans-serif except for
    "enter", the font would be specified as::

      font: 0: sans-serif=6, serif-fixed=5, sans-serif=13

    Attributes past the last specified interval are not changed. An attribute
    interval that extends past the end of the content is truncated at the end
    of the content, any intervals that start past the end are ignored.

    Default values are up to the user agent, so all attributes should be
    specified to ensure actual attributes match what the app expects.

    An attribute may have any UTF-8 encoded name. Attributes include
    those defined here, but may also include any valid string that is
    not defined.
    
    The user agent must store and keep track of the segments of all
    attributes, even those it does not recognize, keep them consistent
    as the content is edited, and return them with the content (when
    editing is complete or a content request command is received). If
    the user agent adds any attributes not passed in the "attributes"
    header, the application is not obliged to identify or store them. If
    the attributes are visible, seeing them disappear could annoy the
    user, so ought to be avoided.

      Q: Could I set an attribute with length 0, so that no text displays that
      attribute, but text added at that point (say, the end) will have it?

      A: The user agent is free to remove any 0 length attributes and join
      adjacent attributes that are the same value, even those at the end of
      the content, so don't count on it. 
      Wait for a "changed" event and set the attributes then.

    The application may send all attributes which it supports, even
    if none of them are applied to the content, in order to allow the
    user agent to identify which attributes the application will accept,
    and provide mechanisms for changing those which are supported
    (buttons, menu items, etc.). This also prevents the user agent from
    enabling unwanted attributes.  For example, a login text field
    normally wouldn't allow multiple fonts or colours (though if you
    want to, go ahead, that would make passwords more interesting).

    Defined attributes are:

    "content"
      Reserved, so as not to interfere with content updates specified in
      the "change-list" header. Any attribute with this name should be
      received and ignored.

    "underline" (binary)
      Text to be displayed with an underline.

    "italic" (binary)
      Text to be displayed in italics.

    "bold" (binary)
      Text to be displayed in bold.

    "font"
      The attribute value consists of:
      
      <string> [ "/" <string> ]

      The first string indicates a standard font. The user agent must
      support four fonts:

      "serif":
        Proportional serif.

      "sans-serif":
        Proportional sans-serif.

      "serif-fixed":
        Fixed width serif.

      "sans-serif-fixed":
        Fixed width sans-serif.

      Any actual font can be chosen for any of these standard fonts,
      though it should in some way resemble what the user expects.

      The second string is unspecified, and can be interpreted any way
      the user agent wants. One way is to refer to a non-standard font
      name to use, with the first string specifying the font to use if the
      second is not available or recognised. For example,
      "sans-serif/hillbilly-grafitti".

    "size"
      The default size of a font is whatever the user agent prefers. It
      may take the size from the windowing system default, or it may be
      defined in a configuration file, or some other source. The size
      attribute indicates what amount to scale this default size by.

      The attribute value consists of a short numeric expression:

      <integer> [ "." <integer> ] [ "/" <integer> [ "." <integer> ] ]

      This is to be interpreted as an integer or decimal divided by
      another integer or decimal. For example, a font half the default
      size can be specified as "1/2" or "0.5", and two thirds the size
      can be "2/3" or "1/1.5". The displayed size does not need to match the
      result exactly.

    "layout"
      This only applies to "textpanel" components, which wrap text at the
      end of each line to continue on the next (as specified by the
      panel's text direction), and scrolling line-by-line. "textfield"
      components consist of a single line (normally scrolling in the
      primary text direction as needed), but this attribute must still
      be kept consistent as text is added or removed. This attribute
      specifies where and how to break up the text into paragraphs,
      lists, and so on. The defined attributes are:

      "block(" <integer> ")":
        A plain paragraph, where the next character is on the start of the next
        line.  The integer indicates how many levels of indentation, if
        supported.

      "indent-first(" <integer> ")":
        A paragraph where the first line is indented one level more than the
        rest.  The integer indicates how many levels of indentation, if
        supported.

      "indent-rest(" <integer> ")":
        A paragraph where the following lines are indented one level more than
        the rest.  The integer indicates how many levels of indentation, if
        supported.

      "list(" <integer> ")":
        A paragraph with a list indicator (such as a bullet or dash). The
        integer indicates how many levels of indentation, if supported. The
        list indicator character may be different for different indent levels.

events: [ "enabled" | "disabled" ]
  If specified, this is used by these components:

  "textfield", "textpanel":
    Indicates the editing behaviour of the component:

    "enabled":
      The user agent will modify the content and attributes from user input,
      and send a changed event when done. This is the default.

    "disabled":
      The user input does not change the content.

  "button":
    Indicates whether the button will generate events.

    "enabled":
      The user agent will generate events from user actions. This is the
      default

    "disabled":
      The user agent will not generate events from user actions.

  "selection"
    Indicates whether items can be selected or unselected.

    "enabled":
      The user agent will allow the user to select or unselect an item, and
      will generate events for each. This is the default.

    "disabled":
      The user agent will not allow the user to select or unselect an item.
      Selected items should remain selected.

    "unselect":
      The user agent will only allow the user to unselect an item, and will
      generate an event when that is done.

items: <item list>
  If specified, this is used by these components:

  "selection"
    A list of items. Items consist of an item ID and a text ID for display. The
    user agent must be able to display 255 items.

    List items are separated by <end-of-line> sequences - the attributes header
    itself is terminated by an <end-of-line> sequence. This makes the string a
    data block which would be encoded with a boundary string, and would look
    like this::

      items:: boundary=
      --
      1: text=12
      2: text=14
      3: text=20, events=disabled
      --

    Items are specified in this form::

      <integer> ":" <id type> "=" <value> [ "," <id type> "=" <value> ]*

    The first integer is the item id, which is included in the event message.

    The ID type determines what is shown to the user, the items should be
    displayed in the order of the item ID.

    Defined ID types are:

    "text"
      The text ID to display, where <value> must be an <integer>.

    "events"
      Indicates whether this item will generate events, where <value> must be
      one of:

      "enabled"
        This item can be selected or unselected. This is the default.

      "disabled"
        This item cannot be selected or unselected. In single selection mode, a
        disabled selected item will still be unselected when another enabled
        item is selected.

selected: [<integer> [ "," <integer> ]*]
  If specified, this is used by these components:

  "selection"
    A list of items IDs from the "items" header, or empty to select no items.
    Any item not in the list is unselected. Any ID not in the "items" header
    list must be ignored.

    If multiple items are in the list but the mode is
    single selection, the user agent can decide how to deal with it in a way
    that makes sense (e.g. first, last, highest, lowest, etc., but probably not
    changing the mode to "multiple").

    If there is no selection but the presentation is "dropdown" (which normally
    does not allow no selection), it's up to the user agent to decide how to
    handle it (e.g. select 0 by default), and to communicate this to the
    server with a "changed" message.

width: <string>
  If specified, this is used by these components:

  "textfield", "textpanel":
    Indicates the desired physical width of the component should be enough to
    display the specified text in the default font, size, and other attributes.
    The default width is at least wide enough to display a single character
    string with the widest character (usually "W" or "M").

    This value can be ignored.

  "selection":
    For "toggle" presentation, this is interpreted as an integet to indicate
    how many columns are desired. Items are arranged vertically, and wrapped to
    the next column. Non-positive or non-integer values are
    ignored.  Default is 1.

    This is a suggestion, and can be adjusted by the user agent, e.g if height
    is smaller than the number of items and the user agent wants to avoid
    scrolling this can be increased.

height: <integer>
  If specified, this is used by these components:

  "textpanel":
    Indicates the desired physical height of the component should be enough to
    display this many lines of text in the default font, size, and other
    attributes. The default height is "1".

    This value can be ignored.

  "selection":
    Modifies how "scroll" or "toggle" presentation is displayed, or the number
    of items displayed in a dropdown list without scrolling when selecting one.

    This is a suggestion, and can be adjusted by the user agent if it's large
    (e.g. past end of display) or small (e.g. minimum of 3).
    

"gui" "modify" optional headers
'''''''''''''''''''''''''''''''

parent: [ <integer> | "none" ]
  Adds this component to a parent, as in the "gui" "add" message except
  that the keyword "none" indicates this component should be removed
  from its existing parent.

  A component that is already added to a parent must be removed first.
  Components cannot have more than one parent.

  This field is ignored for windows, all windows implicitly have the GUI
  root as a parent.

"gui" optional headers
''''''''''''''''''''''

[What's the difference between this and "gui" "add" or "modify" optional
headers? I think these should be moved there]

text: <integer>
  The ID number of the text to use for this control. If the text item
  with this ID does not exist, it's safe to create a text item with that
  ID containing an empty string "".
  
  If specified, the text is used by these components:

  "button":
    The button label. Required unless an image is specified.

  "label":
    Displayed by the label. Required, there is no default.

  "panel":
    If specified, displayed as part of a border around the panel. No border is
    displayed if there is no text specified.

  "window":
    Used in the title bar. Default is should be something like the application
    name and window ID.

visible: ["true" | "false"]
  If specified, this is used by these components:

  "window":
    Controls whether a window is visible or not. Default is
    "false", normally a window is added, then components are added to
    the window, and finally the window is made visible. If a window is
    set to visible when it is added, the user may see the display change
    as components are added in following messages.

    If a window is already visible but behind other windows, it may be
    moved on top of other windows.

text-direction: <direction> "," <direction>
  Whitespace is ignored. If specified, this is used by these components:

  "window" "panel":
    Indicates the direction text and components should
    be laid out. Direction may be one of:
    
    - "left"
    - "right"
    - "up"
    - "down"
    
    The first direction is per line, the second indicates how lines wrap
    (if supported). If one direction is omitted (e.g. "left" or ", down"),
    the user agent should use the existing value (either value already
    set for this component, or the value for the parent component), but
    it may ignore the message, or try to infer the correct value from
    the text or font properties. Any additional directions may be
    ignored.

    This only applies to components that will be added, it does not
    affect components which have already been added. That would be a lot
    of work otherwise, wouldn't it, changing everything around like
    that? Of course, feel free to do it if you want to...
    
    Common combinations (The Java ComponentOrientation class
    documentation lists these) are:
    
    "right, down":
      English, Russian

    "left, down":
      Hebrew, Arabic

    "down, left":
      Chinese, Japanese

    "down, right":
      Apparently some Mongolian languages.

    When components are added, only the horizontal direction is used -
    vertical positions will always start at the top and increase
    downward. Components within a cell should be aligned towards the
    leading horizontal edge, vertially centered.

    User agents may have their own algorithm for accurately deciding on
    how to display text direction horizontally. This does not override
    that, but user agents which don't have this ability should use this
    to determine proper text direction. In that case it's expected that
    text for each single label will be a single direction.

    This may also be used to display text in an alternative or unusual
    orientation, such as horizonatal Chinese or vertical English labels
    for the vertical axis of a graph.  The text may be displayed as
    normal characters arranged in a different direction, or as
    horizontal text rotated 90 degrees (this may also depend on the
    language - Japanese cannot be rotated, and Arabic cannot be
    displayed as separate characters arranged vertically).

    If not specified, the component must inherit the text direction from
    the component that contains it. A default value (set by a GUI modify
    message with no component ID) is used for components which are not
    contained in another (such as windows). If not specified, the value
    "right, down" is used as the default.

      Q: Why can't I specify the text direction of a label?

      A: The panel needs to know the text direction in order to align the
      label properly (left/right, up/down). You can add a panel into another
      panel's cell, set text direction there, then add a label to it.

size: <integer> "," <integer>
  If specified, this is used by these components:

  Non-"window":
    If specified, the first number indicates the number of
    horizontal layout positions this object should occupy, the second
    indicates vertical. If there is an existing component, the new
    component's size must be truncated truncated to fit the following
    way:

      - Include the component's row and all rows below until there is
        overlap with the size taken by another component.

      - If there are no complete vertical rows available, then use the
        current row and all horizontal cells until there is overlap
        with the size taken by another component. There will be at least
        1 cell because any component at the location is replaced.

      - If horizontal size was adjusted, expand vertical size to include
        all vertical rows where there is no overlap, using the new
        horizontal size.

    Conversely, if this component overlaps with an existing component,
    that component must be truncated in the same way.

      Q: I'm not sure that looks good.

      A: To make it look good, the components shouldn't overlap in the first
      place. This is just to handle the situation if they do. These rules
      ensure that the components are truncated the same way no matter which
      order they are added, while still being functional.
    
    If a component is later removed, any components which were truncated
    because of it should be expanded to the new available limits.

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


"text" required headers
'''''''''''''''''''''''

id: <integer>
  The text identifier can be any integer. If an existing text entry has
  the same identifier, it is replaced and any GUI component which uses
  it is updated with the new text.

  items such as text labels or icons may contain ID numbers of text or
  image items in the libraries, and if the library item is replaced then
  the user interface items are updated automatically.

  If a non-integer is used, the user agent must ignore the message. If
  the user agent is in a bad mood, it might disconnect. So don't.

text: <string>
  The string to add to the text category. Text strings contain only
  printable characters. Formatting characters, such as horizontal tab,
  CR, or LF, cannot be included in a text string. If one sentence needs
  to start on a new line, add it as a new string and display it as a new
  label below the first.

REMOVE
++++++

Remove and dispose of a GUI or text item. After being removed, an item
cannot be added to another component.

If some complex item has been constructed and you really don't want to displose
of it, you could add it to a non-visible window until you need it again.

Required headers
~~~~~~~~~~~~~~~~

command: "remove"

category: <keyword>
  These are the categories of items that can be removed.

  "gui":
    When removing a GUI item, none of the items added to that item are
    removed, so they can later be added to another item.

  "text":
    When removing a text item, if that item is being displayed by
    a GUI item, then it should be replaced by an empty string "",
    retaining the same ID.

id: <integer>
  This is the ID of the text or GUI item to be removed.


Event definitions:
-----------------

Every message sent by the user agent must begin with the "event: " field,
followed by the message name. The message name may be any string,
including spaces, but leading and trailing white space is removed. This
means that message names never use camelCase or
underscore_separated_words.

CONNECT
+++++++

When a new connection is detected, a CONNECT event is sent to the
server (normally the user agent sends the first message).

If the server requires authentication, it may send an AUTHENTICATE
command, otherwise the server must begin its task, which usually
includes sending commands to construct the user interface.  The server
may chose to present a login window instead of sending an AUTHENTICATION
command - from the user agent's point of view, this is equivalent to no
authentication.

Required headers
~~~~~~~~~~~~~~~~

event: "connect"

Optional headers
~~~~~~~~~~~~~~~~

application: <string>
  This may be used by the server to identify an application to start up,
  turning the connection over to it until the application terminates. Or
  it may be ignored.

AUTHENTICATE
++++++++++++

An AUTHENTICATE event must also always be sent in response to a
AUTHENTICATE command received from the server.

Upon receiving a AUTHENTICATE event, the server must begin its task,
which usually includes sending commands to construct the user interface,
but it may send a DISCONNECT if it doesn't like the authentication
information.

Required headers
~~~~~~~~~~~~~~~~

event: "authenticate"
method: <keyword>
  The application may send an AUTHENTICATE command after the user agent
  has already sent a AUTHENTICATE event, in which case the user agent
  must send another AUTHENTICATE event with the authentication
  information if desired.

  The "plain" authentication method simply sends unencrypted username
  and password strings. It is important to note that unless the
  connection is secure, this is a security risk since the password is
  passed without encryption.

  The user agent only needs to support "plain" authentication.

  Different headers (and subsequent messages) may be required for different
  authentication methods.

"plain" required headers
~~~~~~~~~~~~~~~~~~~~~~~~

user: <string>
password: <string>


DISCONNECT
++++++++++

Occurs when the user has expressed displeasure at the functioning of the
user agent or the server application, and no longer wishes to continue.
Or in response to a DISCONNECT command.
This indicates that the user agent is no longer in a state that the
server application can rely on, and the application should terminate.
The application is under no obligation to save any data or be graceful
in any way, but terminating the connection (if controlled by the
server), is a reasonable response. If the connection is not terminated,
a server must correctly respond to a subsequent CONNECT event.

The server should not respond to any other events between a DISCONNECT
and subsequent CONNECT. If the connection is terminated by the user
agent, it's the equivalent of receiving a DISCONNECT event.

Required headers
~~~~~~~~~~~~~~~~

event: "disconnect"

CLOSE
+++++

Event generated by a window, indicating the user wants it to be closed.
The user agent should not alter the window when this event is sent, it
is entirely up to the application to perform the actual close.

There is no expectation of what to do when closing a window, so the
application may remove the window or make it invisible, open a new
window (to ask the user to save or confirm, etc.), or disconnect.

Required headers
~~~~~~~~~~~~~~~~

event: "close"

id: <integer>
  The GUI ID of the window to be closed.

CLICK
+++++

Event generated when a component is activated by a user action - most simply,
clicking on it, but could be applied to something else if you're creative.

Maybe to be general purpose, it should be a "changed" event. Leave that for
the future to consider.

Required headers
~~~~~~~~~~~~~~~~

event: "click"

id: <integer>
  The GUI ID of the component which was activated.

CHANGED
+++++++

Event generated by a component which contains data when the state or
content of that data has been changed by the user agent (changes from
server commands do not generate CHANGED events because the app making the
change is aware of the change, and can process it at that time). For example, a
checkbox state or the text in a textfield.

Required headers
~~~~~~~~~~~~~~~~

event: "changed"

id: <integer>
  The GUI ID of the component which has changed.

Optional headers
~~~~~~~~~~~~~~~~

content: <text>
  Generated by these components:

  "textpanel", "textfield":
    Format is the same as the "content" header of an "add" or "modify" command.
    Text is considered changed when it is no longer being actively edited, such
    as if the cursor is moved to another text field (or focus moves away from
    this component), not only from a terminating action such as hitting the
    "return" key (in most GUI toolkits, hitting "return" in a text panel just
    starts a new paragraph).

attributes: <attribute specifiers>
  Generated by these components:

  "textpanel", "textfield":
    Format is the same as the "attributes" header of an "add" or "modify"
    command.

selected: <integer> "," <integer>
  Generated by these components:

  "selection"
    Format is the same as the "selected" header of an "add" or "modify"
    command. An event is sent each time one or more items are selected or
    unselected. Any item not in the list is unselected.

    If selection mode is "single" and multiple elements are in the list, the
    one with the lowest ID is taken as the selected item.

