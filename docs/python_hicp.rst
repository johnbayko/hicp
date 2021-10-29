=============================
Python HICP interface library
=============================

Introduction
============

This library
allows HICP messages to be sent and received in Python to produce an
interactive app on a client (user agent), either locally on the same sytem, or
over a network..

HICP
====

::

  import os
  import pathlib
  from hicp import HICP, App, AppSpec

  class App1(App):
    @classmethod
    def get_app_name(cls):
      return 'one'
    def connected(self, hicp):
      # Create GUI objects and add to hicp.

  class App2(App):
    @classmethod
    def get_app_name(cls):
      return 'two'
    def connected(self, hicp):
      ...

  class Authenticator:
    def authenticate(self, message):
      # Extract authentication information
      # Perform authentication

      if ...failure...:
        return False

      return True

  in_stream = ...from socket or some other stream
  out_stream = ...from socket or some other stream

  app_path = pathlib.Path(os.getcwd())
  app_list = {
      App1.get_app_name(): AppSpec(App1, app_path),
      App2.get_app_name(): AppSpec(App2, app_path)
  }
  default_app = App1.get_app_name()

  authenticator = Authenticator()

  hicp = HICP(
      in_stream=in_stream,
      out_stream=out_stream,
      app_list=app_list,
      [default_app = default_app,]
      [text_group = ...default text group,]
      [text_subgroup = ...default text subgroup,]
      [authenticator = authenticator] )

  hicp.start()

The HICP class manages the initial messages to connect and authenticate. After
that is done, if the connection event include an app name, that app is selected
from ``app_list``. If there is no app specified, or the name can't be found in
app_list, then ``default_spp`` is used as the app name. If that can't be found
in the list, the first app in the list is selected.

Knowledge of this class is not needed for just creating an app. This would be
used by the server which manages the stream creation and applet loading.

``in_stream`` is a file object which implements ``read()``, ``out_stream`` is a
file object that implements ``write()``. Typically using sockets, a file object
can be created from a connected socket by calling ``makefile()``, and used for
both ``in_stream`` and ``out_stream``.

The apps in ``app_list`` are described below.
The ``AppSpec`` objects in the app list include the app class, and a path which
where the app will be run, to allow the app to bundle other resources that it
needs.

``text_group`` and ``text_subgroup`` are optional, and selects the name of a
default text group, normally, a language code like "en", "en"+"uk", "fr",
"fr"+"ca", "es", etc.. The value is
only used as a key, so could be used for other things (e.g. a "Star Wars" vs.
"Star Trek" theme). It's optional if no text group features are used, but
defaults to a useful but annoying "en"+"ca" (Canadian English, with some UK and
some US spellings, but no way to tell which). Text group can be set explicitly
within ``connected()`` instead.

``authenticator`` is an object that implements ``authenticate(self, message)``,
and returns ``True`` or ``False``. ``message`` is the authentication event. The
content can be accessed by ``message.get_header("header name")``, with header
names defined in the ``Message`` class. Useful authentication headers are:

Message.METHOD
    Authentication method. Can be:

    Message.PLAIN
        Plain unencrypted username and password.  Not at all secure, unless
        the underlying connection has been secured first (e.g. ssh). Headers
        used for plain authentication are:

        Message.USER
            Plain text user name.

        Message.PASSWORD
            Plain text password.

If there is no authenticator specified, then ``authenticate(self, hicp,
message)`` is called on the selected app, which would allow the app to open a
use interface to interrogate the user (used the same way as ``connect(self,
hicp)``).

HICP provides the general interface to the user agent (the client process
that receives commands to display the user interfafce, and sends events based
on user actions).

For testing, this is all handled by the ``hicpd`` server described below.

HICP start()
------------

::

  hicp.start()

``start()`` triggers the HICP object to perform the connection, authentication,
and app activation as described above. It's the only action performed outside
the app's ``connect()`` (or ``authenticate()``) function.

HICP text_direction()
---------------------

::

  hicp.text_direction(hicp.RIGHT, hicp.DOWN)

``text_direction()`` can change the layout and text direction to be used.
Default is ``hicp.RIGHT`` (left to right) and ``hicp.DOWN`` (top to bottom),
basically normal English text. The second value is often ignored when the first
direction is horizontal, but when text is vertical it can indicate whether
layout goes right to left or left to right.  Defined values are:

- hicp.LEFT
- hicp.RIGHT
- hicp.UP
- hicp.DOWN

HICP set_text_group()
---------------------

::

  hicp.set_text_group("es")

  hicp.set_text_group("es", "mx")

This applies only when using HICP to manage language groups, so will affect
HICP ``add_text_get_id()``, ``add_groups_text_get_id()``,
``add_text_selector_get_id``, or component
``set_text()`` or ``set_groups_text()`` described below. This selects the
specified group and subgroup, then updates the user agent text library to the
new text strings. This has the effect of updating all text displayed to the
user.

HICP get_text_group()
---------------------

::

  (group, subgroup) = hicp.get_text_group()

Returns the current text group and subgroup, or ``None`` when not set.

HICP add_text() and add_all_text()
----------------------------------

::

  hicp.add_text(1, "Name:")

  hicp.add_all_text({ 1: "Name:", 2: "Position:" })

There are two ways of setting text in a component that supports it:

- Add the text with an ID number, then set the component text using
  ``set_text_id()``.
- Let HICP handle assigning text IDs automatically, described in the components
  section below.

The second is easier and should for simple cases.  It's important not to mix
the two methods unless you really know what you're doing.

``add_text()`` Adds a text string with a sp[ecific ID number for the current
group and subgroup, and to the user agent text library. The ID can be used by a
component added later.

``add_all_text()`` specifies multiple IDs and values to add to the user agent
text library.

Text is added to the current group and subgroup, text for other groups being
used will be empty. If the groups or subgroup are changed, the text will not
change since it's the only option.

You might want to use these if you need to replace text from some external
source (either a group of text IDs, or all text), or to manage large numbers of
ted groups. For small number of groups, or when groups aren't needed, it's
probably easier to specify the text directly using hicp ``add_text_get_id()``
or ``add_grups_text_get_id()``, or component ``set_text()`` or
``set_groups_text()``. The downside to that is that setting text directly will
accept typos without question, but using text IDs will ensure the same spelling
is always used everywhere the text ID is specified.

See the "Components supporting text" section for more on using text IDs and
text groups.

HICP add_text_get_id(), add_groups_text_get_id(), add_text_selector_get_id()
----------------------------------------------------------------------------

::

  NAME_ID = hicp.add_text_get_id("Name")

  NAME_ID = hicp.add_text_get_id("Name", "en", "us")

  NAME_ID = hicp.add_groups_text_get_id([("Name", "en"), ("Nom", "fr")])

  app_info = ...
  NAME_ID = hicp.add_text_selector_get_id(app_info.display_name)

``add_text_get_id()`` adds a text string (to the specified group and subgroup,
to the current text group and subgroup if not specified), but returns an ID for
it, rather than requiring an ID to be specified. This is useful for dynamically
generated strings. The ID can be used with a components ``set_text_id()``
method.

``add_groups_text_get_id()`` ("groups" is plural in this name) stores the given
texts for the specified text groups, and returns an ID to refer to all of them.
When the HICP text group is changed, the user agent is updated with the correct
texts for the new group.

``add_text_selector_get_id()`` stores the given ``TextSelector`` object and
returns an ID for it. This is useful in cases where a ``TextSelector`` object
is provided, such as ``AppInfo`` ``display_name`` and ``description`` fields.
When the HICP text group is changed, the user agent is updated with the correct
texts for the new group.

The ID returned by ``add_groups_text_get_id()`` is determined by matching the
text for the current group, and allocating a new one if no match is found. If
the same text for the current group is specified in another call to this
method, it will match that ID, and the strings for other groups in the new call
will replace the previously added strings. For example ``("Back", "Dos")``
(compared to "Front") will replace ``("Back", "Arrière")`` (direction), which
would not be what you want.

HICP get_text()
---------------

::

  s = hicp.get_text(NAME_ID)

Returns the text string for the given ID, for the current text group and
subgroup.

HICP sort()
-----------

::

  unsorted_ingredients = []

  for ingredient in recipe:
    name_id = hicp.add_text_get_id(ingredient.name)
    unsorted_ingredients.append((name_id, ingredient))

  sorted_ingredients = hicp.sort(unsorted_ingredients)

``sort()`` takes a list of tuples of the form ``(text_id, object)`` and sorts
them based on the text strings for the current text group and subgroup. For
example "en" would sort based on "egg", "flour", "milk", while "fr" would sort
based on "farine", "lait", "oeuf".

HICP add()
----------

::

  from hicp import Window

  w = Window()
  w.set_visible(True)
  hicp.add(w)

Adds a component that's not contained in another component to the user agent
displayed interface. That's pretty much just a ``Window`` object.

HICP remove()
-------------

::

  hicp.remove(w)

Remove a component that was added using ``add()``.

HICP get_all_app_info()
-----------------------

::

  all_apps = hicp.get_all_app_info()

  name = all_apps['cals'].display_name.get_text()
  description = all_apps['cals'].description.get_text()

Returns a dictionary of ``AppInfo`` objects, indexed by the
``AppInfo.app_name`` value.
``AppInfo`` objects are explained in the App section below.

HICP switch_app()
-----------------

::

  hicp.switch_app('calc')

Stops the current app, and starts a new one with the given name. If the name is
not an actual app, this is treated as a disconnect request.

HICP disconect()
----------------

::

  hicp.disconnect()

Sends a disconnect command to the user agent. Does not preemptively close the
connection, this allows the user agent time to do any cleanup it wants to, then
send a disconnect event when it's ready.

TextSelector
------------

::

  from hicp import TextSelector

  ts = TextSelector( [
    ('Beep'),  # Default, assume robot.
    ('Hello', 'en'),
      ('Bonjour', 'fr'),
      ('Bonjour-Hi', 'fr', 'ca'),  # Regional variation.
    ] )

A ``TextSelector`` object holds a set of texts with the same meaning, for
different groups and subgroups. It's created with a list of tuples, each field
containing:

- The text for the given group and subgroup.

- The group code, if specified.

- The subgroup code, if specified.

The group and subgroup are used by the ``get_text()`` method.

An ``AppInfo`` object will return these for the user visible app name and
description, to allow the correct one to be presented for the current group and
subgroup.

TextSelector add_text()
-----------------------

::

  from hicp import TextSelector

  ts = TextSelector( [
      ('Hello', 'en'),
      ('Bonjour', 'fr'),
    ] )
  ts.add_text('Bonjour-Hi', 'fr', 'ca') # Regional variation

Add or replace a text for the given optional group and subgroup, to the given
text selection object. Group and subgroup arguments are optional.

TextSelector add_all_text()
---------------------------

::

  from hicp import TextSelector

  ts = TextSelector( [('Beep')] )
  ...
  ts.add_all_text( [
      ('Hello', 'en'),
      ('Bonjour', 'fr'),
      ('Bonjour-Hi', 'fr', 'ca'),
    ] )

Add or replace all text from the given list of tuples. The list is the same
format as for the constructor.

TextSelector get_text()
-----------------------

::

  greeting = ts.get_text()

  greeting = ts.get_text('en')

  greeting = ts.get_text('en', 'ca')

Get the text stored in this ``TextSelector`` for the given group and subgroup.
The selector will try to find the best match, from most specific to least. For
example, if there is no variation for group 'en' and subgroup 'ca', then group
'en' with no subgroup is used. If there is no text for that group without a
subgroup, an arbitrary subgroup will be selected.

Similarly if a specified group has no text, the default text will be used, and
if there is no default text, an arbitrary group, or group with subgroup, will
be selected.

Apps
====

An ``App`` class is just a class which extendcs ``hicp.App``, and overrides the
methods it specifies. Those are:

``get_app_name(cls)``
  A class method which returns the name that the app expects to be referred to
  in the initial ``CONNECT`` message. If not overridden, this will return the
  class name. Generally not visible to the user.

``get_app_info(cls)``
  A class method which returns an ``AppInfo`` object. ``AppInfo.__init__()``
  parameters are:

  - app_name
  - display_name
  - description

  It contains these fields:

  ``app_name``
    The app name.

  ``display_name``
    A ``TextSelector`` with the name to display to users, if needed.

  ``description``
    A ``TextSelector`` with the description of the app.

  ``display_name`` and ``description`` are ``TextSelector`` objects to allow
  strings identified by group and subgroup. The
  parameters for these
  can be a string, a tuple (text, group, subgroup), a list of
  tuples, or an actual ``TextSelector`` object.

  If not overridden, this will use the result of ``get_app_name()`` for both
  name and description.

``connected(self, hicp)``
  Called after a connection is created and the app to run is identified and
  instantiated. This is where components are created and added to form a
  hierarchy that ends in windows added to the ``hicp`` object. Components must
  have event handlers to respond to use events, or the app won't do anything
  except display whatever is added here.

  Normally one handler (e.g. a "Quit" button, or the window close handler) will
  call ``hicp.disconnect()`` to exit the app.

  If not overridden, this just calls ``hicp.disconnects()``.

An app can also implement ``authenticate(self, hicp, message)``, which is
called if there is no authenticator specified. The app could put up a window to
log in, though that window would be insecure so is not generally a good idea
unless no security is needed.

The ``App`` class also has the convenience method:

``new_app_window()``
  This creates a ``Window`` with a close handler that disconnects the app when
  closed.

Message
=======

::

  m = Message()
  m.set_type(Message.EVENT, Message.CLICK)
  m.add_header(Message.COMPONENT, add_button.component)
  m.add_header(Message.ID, add_button.component_id)

A message is either a command or event, with a type value, and a list of header
values, which are indexed by the header name.

For the most part, messages don't need to be created in an app, with an
exception when a fake event needs to be created. Message header values do
sometimes need to be read in event handlers, using ``get_header()`` described
below.

Message set_type(), get_type(), and get_type_value()
----------------------------------------------------

::

  m = Message()
  m.set_type(Message.EVENT, Message.CLICK)

  ...

  if m.get_type() != Message.EVENT:
    return

  handler = all_handlers[m.get_type_value()]
  handler.handle(m)

``set_type()`` sets whether a message is an event (from the user agent to the
app) or a command (sent to the user agent from a component). ``get_type()``
returns the message type, ``get_type_value()`` returns which event or command
it is.

Command messages are created and sent by components. Events messages are
available from the ``event`` parameters to handler methods. The handler is
selected based on the event type value, so the value doesn't need to be checked
in the handler. Event types are described in the ``EventType`` section below.

Message add_header(), get_header(), and clear()
-----------------------------------------------

::

  m = Message()
  m.add_header('a', 'one')

  v = m.get_header('a')

  m.clear()

``add_header()`` adds a string value with a string key, much like a dictionary,
``get_header()`` returns that value (or ``None`` if it hasn't been added).
``clear()`` removes all headers and values. The strings can be anything, but
standard header names are defined as constants in the ``Message`` class.

Event
=====

::

  def update(self, hicp, event, component):
    m = event.message
    t = event.event_type
    c = event.component
    tm = event.event_time

Event objects are passed to handler methods, and have fields for information
relevant to the event:

``message``
  The message received to create this event, or ``None`` if there is no
  associated message. It can be used to get header values related to the event.

``event_type``
  A value of ``EventType`` from the message type value, described below.

``component``
  The component that this event was generated by, if there was one, otherwise
  ``None``. This will be the same as the ``component`` parameter of the handler
  methods for component event handlers (event handlers for non-component events
  do not have that parameter).

``event_time``
  For time events, this is the time the event was scheduled for, not the time
  it was generated, although the difference should normally be insignificant.

  For message based events, this is the time the event was received. This is
  not the time the event message was generated by the user agent, though the
  network delay is normally small enough that it doesn't make a difference.

EventType
=========

An enumeration class of event types. The events have these fields:

``event_id``
  An automatically generated ID.

``event_name``
  The event name (same as the corresponding ``Message`` constant).

``from_component``
  A boolean indicating whether this type of event is generated by a component.

The defined event types relevant to event handlers are:

CHANGED
  A component state has changed, such as a text field content being modified.

CLICK
  A component (normally a button) has been clicked.

CLOSE
  A component (normally a window) has been closed.

CONNECT
  Never passed to a handler, indicates the user agent has connected.

DISCONNECT
  Indicates the app has disconnected.

TIME
  Indicates a time event has occurred.


Event handling
==============

Component event handling
------------------------

::

  from hicp import EventType

  class UpdateButtonHandler:
    def feedback(self, hicp, event, component):
        ...optional event feedback...

    def process(self, event, component):
        ...optional long term processing...

    def update(self, hicp, event, component):
        ...optional update results

  update_button.set_handler(EventType.CLICK, UpdateButtonHnadler())

Events are handled in three stages:

Feedback
  Update the user agent display with an indication that the event was received
  and is being processed. Only really needed if processing might take a long
  time, like updating a database or making an API call to a remote server, can
  be omitted if not needed.

Process
  Any long term operation is handled in a separate thread, allowing any other
  events to be handled meanwhile. Can be omitted if not needed.

  The process stage does not interact with the client at all, so there is no
  ``hicp`` parameter.

Update
  Update the user agent display with the results of the event processing, if
  there are any. This is normally where windows would be opened or closed,
  component contents would be updated, and so on, but there might be rare cases
  where there's no update necessary, so this is also optional.

``feedback()`` and ``update()`` stages are handled in the same thread, while all
``process()`` stages are in a separate thread. This means:

- All event ``feedback()``, ``process()``, and ``update()`` stages always
  happen in that order.

- All event ``feedback()`` stages are run in the order they are received.

- All event ``process()`` stages are run in the order they are received.

- ``update()`` stages might run in a different order than received.
  Specifically events with no ``process()`` handler will skip directly to
  ``update()`` while the previous event is busy.

- No two ``feedback()`` or ``update()`` stages from any event will run at the
  same time.

- No two ``process()`` stages from any event will run at the same time.

- ``process()`` stages might run at the same time as another event's
  ``feedback()`` or ``update()`` stages (but never its own).

Disconnect event handling
-------------------------

::

  class DisconnectHandler:
    def __init__(self, ...stuff that needs to be cleaned up...):
      ...Add stuff to clean up to self

    def process(self, event):
      ...clean up stuff

  hicp.set_disconnect_handler(DisconnectHandler(...stuff...))

A disconnect handler can be added to ``hicp``. The ``process()`` method will be
called when a disconnect message is received, or there is a communication
disconnection that terminates the application. The handler cannot interact with
the client (so no ``feedback()`` or ``update()`` stages), and should be very
short.

It's meant to allow cleanup, such as closing connections or saving data, but
there is never any guarantee that the event will be received, so any important
data should be saved immediately rather than waiting for the disconnect
handler.

Timeout handling
----------------

::

  from hicp import TimeHandler

  class EveryThreeSeconds(TimeHandler):
    def __init__(self, clock_text):
        ...initialization...
        self.time_info = TimeHandlerInfo(3, is_repeating=True)

    def get_info(self):
        return self.time_info

    def feedback(self, hicp, event):
        ...optional event feedback...

    def process(self, event):
        ...optional long term processing...

    def update(self, hicp, event):
        ...optional update results

  hicp.add_time_handler(EveryThreeSeconds())

Adds a handler to be called after a specific number of seconds has passed,
optionally repeating. A time event is not associated with a component, but
operates in the same way.

Unlike other handlers, a time handler must extend the ``TimeHandler`` class,
and override the ``get_info()`` method to return a ``TimeHandlerInfo`` object.
That object specifies the number of seconds the timer is for, and an optional
flag indicating whether the event should repeat (``False`` by default).

Fake events
-----------

::

  from hicp import Button, Message

  button = Button()
  ...configure and add button...

  event = Message()
  event.set_type(Message.EVENT, Message.CLICKED)
  event.add_header(Message.ID, str(button.component_id))

  hicp.fake_event(event)

Adds an event message to the processing queue to appear as if it was received
from the user agent.

One use for this would be if a selection was changed by the app, the user agent
would not send a change event because it assumes the app is aware of the
change it made. It can simplify things if the change is inserted as an event to
be processed the normal way, rather than factoring the code out into a new
method (managing the relevant state as well) to handle it.

Components
==========

Components supporting text
--------------------------

Components which support text can have the text id set once they've been
created, with ``set_text_id()``, ``set_text()``, or ``set_groups_text()``.
Those components are:

- Window
- Label
- Button

Component set_text_id()
-----------------------

First way of setting text:

::

  lc.set_text_id(5)
  window.add(lc, 2, 3)

  lc.set_text_id(6)
  lc.update()

This sets the component displayed text to the text in the user agent text
library indicated by the ID number, which can be a static number added
previously using HICP ``add_text()`` or ``add_all_text()``, or a dynamically
assigned number from ``add_text_get_id()`` or  ``add_groups_text_get_id()``.

A static ID doesn't support text groups, so shouldn't be mixed with
``set_text()`` or ``set_groups_text()``, but an assigned ID will work.

Component set_text() and set_groups_text()
------------------------------------------

Second (easier) way of setting text:

::

  lc.set_text("Name:", hicp)
  window.add(lc, 2, 3)

  lc.set_groups_text([("Name", "en"), ("Nom", "fr")], hicp)
  window.add(lc, 2, 3)

  lc.set_groups_text([("New Name", "en"), ("Nuveau Nom", "fr")], hicp)
  lc.update()

``set_text(t, hicp)`` is the equivalent to ``set_groups_text( [(t)], hicp )``,
it adds text for the current text group and no others. Can be used if there is
no multilingual support needed.

In ``set_groups_text()``, "groups" is plural, don't forget. It stores the given
texts for all text groups (automatically assigns the same ID for them all), and
updates user agant with the text for the current group. When the HICP text
group is changed, the user agent is updated with the correct texts for the new
group.

See HICP ``set_groups_text_get_id()`` for more information.

Component set_size()
--------------------

::

  from hicp import Label, Button, EventType

  l = Label()
  l.set_text("Options:")
  l.set_size(3, 1)  # Label is wide as three option buttons below it
  w.add(l, 0, 0)

  b1 = Button()
  b1.set_text("One")
  b1.set_handler(EventType.CLICK, OptionOneHandler())
  w.add(b1, 0, 1)

  b2 = ...Option 2 button...
  w.add(b2, 1, 1)

  b3 = ...Option 3 button...
  w.add(b3, 2, 1)

Components that are contained in another (everything except windows) have a
size, which is the number of positions it should take up in a specific
direction, horizontal or vertical.

A component size larger than 1 is only a suggestion, if there is a component
that this one would cover, the size is shortened (similarly, if adding a
component would cover part of an existing component, that component's size is
also shortened). The special case of size 0 means extend the component as far
as possible without making the window any bigger (limited by the same size
rules). Default is ``(0, 0)``.

Window
======

::

  from hicp import Window, Button, EventType

  wc = WelcomeCloser()

  w = Window()
  w.set_text("Welcome")  # Window frame title.
  w.set_handler(EventType.CLOSE, wc)
  hicp.add(w)

  bc = ...a close button...

  w.add(bc, 0, 0)
  w.set_visible(true)
  w.update()

A window is a top level display component that is added directly to an HICP
object, and contains other components that are added to it.

Window text sets the window frame title.

Window add()
------------

::

  w.add(close_button, 3, 4)

``add()`` adds a component to the specified grid position (horizontal and
vertical). Any component at that position is replaced by the new one if
supported by the user agent. If not supported, the older component is not
replaced. The window size and other component positions might be shifted
around automatically.

Positions start at 0 and go up to 255.

There should be a ``remove()``, but I haven't done that yet.

Window set_visible()
--------------------

::

  w.set_visible(True)

When set to ``True`` makes the user agent display the window and contents. The
window can be added and constructed, then made visible when it's complete. When set to ``False`` the window will not be displayed.

Window set_handler()
--------------------

::

  from hicp import EventType

  class CloseHandler:
    def feedback(self, hicp, event, component):
        ...optional event feedback...

    def process(self, event, component):
        ...optional long term processing...

    def update(self, hicp, event, component):
        hicp.remove(component)

  w.set_handler(EventType.CLOSE, CloseHandler())

A handler can be added to a ``Window`` for these events:

``EventType.CLOSE``
    When a window's "close" control on the frame is clicked, it sends a "close"
    event. This handler should remove the window (closing it) or make it
    invisible if it might be opened again. Closing the last window can also
    disconnect the application (call ``hicp.disconnect()``).

Panel
=====

::

  from hicp import Button, Label, Panel

  # Lights panel.
  pl = Panel()

  lights = Label()
  lights.set_text("Lights")
  pl.add(lights, 0, 0)

  ...Add more things...

  w.add(pl, 0, 0)

  # Sound panel
  ps = Panel()

  sound = Label()
  sound.set_text("Sound")
  ps.add(sound, 9, 0)

  ...Add more things...

  w.add(ps, 1, 0)

  close = Button()
  ...
  w.add(close, 1, 1)

A panel provides the same layout as a window, but is added within a window or
another panel to provide flexible layout options.

Label
=====

::

  from hicp import Label

  l = Label()
  l.set_text("Welcome")

  w.add(l, 0, 0)

A label just displays text. It must be added to a container component (like a
window or panel).

Button
======

::

  from hicp import Button, EventType

  hb = ...button click handler...

  b = Button()
  b.set_text("Activate")
  b.set_handler(EventType.CLICK, hb)

  w.add(b, 0, 1)

A button can be clicked to send an event that the specified handler processes.
The text is just displayed on the button. It must be added to a container
component (like a window or panel).

Button set_handler()
--------------------

::

  from hicp import EventType

  class ActivateHandler:
    def feedback(self, hicp, event, component):
        ...optional event feedback...

    def process(self, event, component):
        ...optional long term processing...

    def update(self, hicp, event, component):
        ...optional update results

  b.set_handler(EventType.CLICK, ActivateHandler())

A handler can be added to a ``Button`` for these events:

``EventType.CLICK``
  The event handler is called when a button's click event is received, as
  described above.

Button set_events()
-------------------

::

  b.set_events(Button.DISABLED)

When set to ``Button.DISABLED``, button events are not sent, events are sent
when set to ``Button.ENABLED`` (default).

TextField
=========

::

  from hicp import TextField, EventType

  user = ...an object with a .name string...

  htc = ...handler for text field changed content...

  tf = TextField()
  tf.set_content(user.name)
  tf.set_handler(EventType.CHANGED, htc)

  w.add(tf, 1, 2)

A text field displays a single line of text content, and allows it to be edited
by the user. When editing is finished, a changed event is sent.

The text contents of a text field can have various attributes set, but user
agents typically don't support text with attributes for fields, only for multi
line text panels, but if there are attributes they are guaranteed to be
preserved and correct after the text content is changed.

TextField set_content() and get_content()
-----------------------------------------

::

  tf.set_content("0.0")

  price = tf.get_content()

The content is the text data to edit. Data is not part of the interface so
isn't handled like component text (no text ID or text group).

``get_content()`` is mostly useful in the change handler described below.

TextField set_attribute(), get_attribute_string(), set_attribute_string()
-------------------------------------------------------------------------

::

  from hicp import TextField

  tf.set_attribute(TextField.UNDERLINE, 5, 2)

Attributes are usually not displayed for text fields, but can still be set and
will be preserved as the text is edited. They're covered more for text panels,
which does display attributes normally.

``get_attribute_string()`` returns the attributes in a string form that is sent
in the hicp message protocol, and isn't normally useful except for debugging.
``set_attribute_string()`` sets attributes based on the same format of string,
and is less useful, except maybe for testing.

TextField set_handler()
-----------------------

::

  from hicp import EventType

  class PriceEnteredHandler:
    def __init__(transaction):
      self.transaction = transaction

    def feedback(self, hicp, event, text_field):
        ...optional event feedback...

    def process(self, event, text_field):
        ...optional long term processing...

    def update(self, hicp, event, text_field):
        try:
          price_str = text_field.get_content()
          self.transaction.price = int(price_str)
        except:
          # Not a valid price or content not changed, do not update.
          pass

  tr = ...an object with a .price field...

  tf.set_handler(EventType.CHANGED, PriceEnteredHandler(tr))

A handler can be added to a ``TextField`` for these events:

``EventType.CHANGED``
  Once editing finishes, a changed event is sent which contains the changed
  text and current attributes, if any. This is normally the entire text content
  once editing is complete, not individual changes.

  The text field is updated with the changed contents and attributes before the
  handler functions are called, so ``get_content()`` etc. behaves as expected.

Selection
=========

::

    from hicp import Selection, SelectionItem

    a = Able()
    b = Baker()
    c = Charlie()

    s = Selection()
    s.add_items( {
        1: SelectionItem(1, a.name, hicp, a),
        2: SelectionItem(2, b.name, hicp, b),
        3: SelectionItem(3, c.name, hicp, events=Selection.DISABLED, c),
      } )
    s.set_presentation(Selection.SCROLL)
    s.set_selection_mode(Selection.MULTIPLE)
    w.add(s, 1, 2)

A selection displays several items that the user can select or deselect,
presented in different ways.

The items are displayed as strings, using the specified text ID, with the
specified item ID. Item IDs are arbitrary, and must be unique to the selection
component they are added to (another component can use the same ID for its
items). An item can be disabled by setting its ``events`` field, which won't
allow the user to select or deselect them, while the others are unaffected.

A selection component can present the selection items in several ways described
in the ``set_presentation()`` section, and can allow single or multiple
selecitons as described in the ``set_mode()`` section. Some presentation aspects
can be specified as well as some selection behaviour.

Selection set_items()
---------------------

::

  from hicp import Selection, SelectionItem

  contact_items = []
  contact_id = 0
  for contact in contacts:
    name = contact.name
    name_id = hicp.add_text_get_id(name)

    if contact.category == 'work':
      events = Selection.ENABLE
    else:
      events = Selection.DISABLE

    contact_items.append(
        Selectionitem(contact_id, name_id, events=events, contact) )

  s.set_items(contact_items)

Replaces all current items with those from the specified list of
``SelectionItem`` objects. No previous items remain.

A ``Selectionitem`` has these parameters:

``item_id``
    An arbitrary integer identifying the selection item.

``text``
    Can be the text ID of the text to display for the item, a string, or a list
    of string, group, and subgroup tuples. If it's a string or list of tuples,
    and the ``hicp`` parameter is specified, then the string or tuples are
    added to the ``hicp`` object and the text ID is taken from that.

``hicp``
    Optional, used if a string is passed as a text parameter, unused otherwise.

``events``
    Optional, specify if the item can be selected or unselected to generate
    changed events. Can be:

    ``Selection.ENABLED``
        (Default) Item can be selected or unselected.

    ``Selection.DISABLED``
        Item cannot be selected or unselected.

    ``Selection.UNSELECT``
        Item can be unselected if selected, but unselected items cannot be
        selected.

``item``
    An arbitrary object associated with this item, so it can be selected
    without needing to look up an object using the item ID.

The ``SelectionItem`` has these fields that can be read and modified:

- ``item_id``

- ``text_id``

- ``events``

- ``item``

No reference to the items list itself is saved, but individual
``SelectionItems`` are not duplicated, so shouldn't be modified accidentally.

Selection add_items()
---------------------

::

  from hicp import Selection, SelectionItem

  s = Selection()

  contact_id = 0
  for contact in contacts:
    name = contact.name
    name_id = hicp.add_text_get_id(name)
    if contact.category == 'work':
      events = Selection.ENABLE
    else:
      events = Selection.DISABLE

    s.add_items( [
        Selectionitem(contact_id, name_id, events=events, congtact)
      ] )

Adds items from the specified list of ``SelectionItem`` objects.
Existing items with the same item ID are replaced, but any
others will remain.

The list can be modified afterwards, but the
``SelectionItems`` are not duplicated, so shouldn't be modified accidentally.

Selection del_items()
---------------------

::

  s.del_items([1, 3, 4])

Deletes all items with the item IDs in the specified list.

Selection get_item()
--------------------

::

  si = s.get_item(3)

Returns the ``SelectionItem`` for the given item ID. She returned item is the
original, not a copy, so shouldn't be modified accidentally.

Selection copy_items()
----------------------

::

  items = s.copy_items()

Returns a dictionary of the current ``SelectionItems``.
A copy is passed so the returned
dictionary can be modified, but the ``SelectonItems`` are the original and
shouldn't be changed accidentally.

Selection set_selection_mode()
------------------------------

::

  s.set_selection_mode(Selection.SINGLE)

Sets whether multiple items can be selected at once. Mode can be:

``Selection.SINGLE``
    Only a single item can be selected at a time, although no selection is also
    valid, depending on the presentation.

``Selection.MULTIPLE``
    Any number can be selected, including none. This is the default.

Selection set_presentation()
----------------------------

::

  s.set_presentation(Selection.SCROLL)

Sets how the items will be presented to the user for selection.

``Selection.SCROLL``
    All items are displayed in a list in which they can be selected or
    unselected by clicking on them. The items can be scrolled if there are
    too many for the list height. This is the default for
    multiple selection.

``Selection.TOGGLE``
    Items are displayed as individual items on a panel which the user agent
    determins based on the GUI style and other attributes, such as single /
    multi selection or scroll height settings, That could be check boxes,
    switches, or radio buttons (normally arranged vertically, not
    scrollable).

``Selection.DROPDOWN``
    For single selection lists only (selection mode is ignored), a dropdown
    tool presents the available items for the user to select, with only the
    selected item visible when not being changed. This is the default for
    single selection.

Selection set_selected_list()
-----------------------------

::

  s.set_selected_list([1, 3])

Sets the items that are selected, based on the item IDs specified in the
parameter.

Selection del_selected_list()
-----------------------------

::

  s.del_selected_list([1, 3])

Removed items from those that are selected, based on the item IDs specified in
the parameter.

Selection set_selected_string()
------------------------------

::

  s.set_selected_string("1, 3")

Same as ``set_selected_list()``, but the items are specified as a string of
integers separated by commas.  If the string is not valid, the selection will
be cleared (no items selected).

Selection copy_selected_list()
------------------------------

::

  selected = s.copy_selected_list()

Returns a copy of the list of selected item IDs.

Selection get_selected_item_list()
----------------------------------

::

  selected = s.get_selected_item_list()

Returns a list of the ``SelectionItem`` objects that are selected. The list can
be modified, but the ``SelectionItems`` are the originals, so should not be
modified accidentally.

Selection set_height()
----------------------

::

  s.set_height(10)

The interpretation depends on the presentation.

For ``Selection.SCROLL``, indcates the desired number of items visible in the
selection area, with the rest available by scrolling.

For ``Selection.TOGGLE``, indicates the desired numnber of itemd arranged
vertically before starting a new column of items.

The actual result depends on the user agent implementation. It may be ignored
for other presentations.

Selection set_width()
---------------------

::

  s.set_width(2)

The interpretation depends on the presentation.

For ``Selection.TOGGLE``, indicates the desired numnber of columns displayed.

The actual result depends on the user agent implementation. It may be ignored
for other presentations.

Selection set_handler()
-----------------------

::

  from hicp import EventType, Selection, TextField

  class SelectionHandler:
    def __init__(address_field):
      self.__address_field = address_field

    def feedback(self, hicp, event, selection):
        ...optional event feedback...

    def process(self, event, selection):
        ...optional long term processing...

    def update(self, hicp, event, selection):
      items = selection.get_selected_item_list()

      if 0 < len(items):
        # Should only be one.
        contact_item = items[0]
        contact = contact_item.item
        address = contact.address
      else:
        address = ''

      self.__address_field.set_content(address)
      self.__address_field.update()

  address_field = TextField()
  ...set up and add address field...

  s = Selection()
  ...
  s.set_handler(EventType.CHANGED, SelectionHandler())

A handler can be added to a ``Selection`` for these events:

``EventType.CHANGED``
  When one or more items have been selected or deselected, a changed event is
  sent which contains the current selection. The selection component is updated
  from the event and can be used to get the current selection information.

hicpd
=====

A test server ``hicpd.py`` (in the ``hicpd`` directory) allows testing of apps.
It reads the environment variable ``HICPPATH`` for the base directory (or the
current directory where it is run if that's not set), and looks for any
application classes in any subdirectories under the ``apps`` directory, and the
specific ``default_app`` directory. Any classes extending the ``App`` classs
will be loaded and made available to a connecting client, and any app class in ``default_app`` will be used as the default app.

A new app can be added into a new subdirectory under ``apps``, and can be run
by including the app name in the initial ``CONNECT`` message.

Details on using the server is in the hicpd README file.

