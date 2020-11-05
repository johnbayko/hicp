=============================
Python HICP interface library
=============================

Introduction
============

This allows HICP messages to be sent and received in Python to produce an
interactive app.

HICP
====

::

  from hicp import HICP

  class App1:
    def connected(self, hicp):
      # Create GUI objects and add to hicp.

  class App2:
    def connected(self, hicp):
      ...

  class Authenticator:
    def authenticate(self, message):
      # Extract authentication information
      # Perform authentication

      if ...failure...:
        return False

      return True

  in_stream = ...from socket or some other steam
  out_stream = ...from socket or some other steam

  app_list = { "One": App1(), "two": App2() }
  default+app = "One"

  authenticator = Authenticator()

  hicp = HICP(
      in_stream=in_stream,
      out_stream=out_stream,
      app_list=app_list,
      [default_app = default_app,]
      [text_group = ...default text group,]
      [authenticator = authenticator] )

  hicp.start()

The HICP class manages the initial messages to connect and authenticate. After
that is done, if the connection event include an app name, that app is selected
from ``app_list``. If there is no app specified, or the name can't be found in
app_list, then ``default_spp`` is used as the app name. If that can't be found
in the list, the first app in the list is selected.

``in_stream`` is a file object which implements ``read()``, ``out_stream`` is a
file object that implements ``write()``. Typically using sockets, a file object
can be created from a connected socket by calling ``makefile()``, and used for
both ``in_stream`` and ``out_stream``.

The apps in ``app_list`` all need to implement ``connected(self, hicp)``. That
is called after a connection is made and an app is selected. and is where the
components are created and added together, along with event handlers. The app
continues to listen for events and call the handlers until one of the handlers
calls ``hicp.disconnect()``.

An app can also implement ``authenticate(self, hicp, message)``, which is
called if there is no authenticator specified.

``text_group`` is optional, and selects the name of a default text group,
normally, a language code like "en", "en-uk", "fr", "es", etc.. The value is
only used as a key, so could be used for other things (e.g. a "Stqr Wars" vs.
"Star Trek" theme). It's optional if no text group features are used, but
defaults to a useful but annoying "en-ca" (Canadian English, with some UK and
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

HICP add_text() and add_all_text()
----------------------------------

::

  hicp.add_text(1, "Name:")

  hicp.add_all_text({ 1: "Name:", 2: "Position:" })

``add_text()`` Adds a text string with a sp[ecific ID number to the user agent
text library, to be used by a component to be added later.

``add_all_text()`` specifies multiple IDs and values to add to the user agent
text library. Text can be added in multiple parts, previously added text is not
removed.

When using text ID numbers, HICP does not keep track of text group, so they
should not be mixed. To use the text group, the text and group information is
added directly to each component.

You might want to use these if you need to replace text from some external
source (either a group of text IDs, or all text). Otherwise it's probably
easier to specify the text directly using component ``set_text()`` or
``set_groups_text()``. The downside to that is that it will accept typos
without question, but using text IDs will ensure the same spelling is always
used everywhere the text is specified.

See the "Components supporting text" section for more on using text IDs and
text groups.

HICP set_text_group()
---------------------

::

  hicp.set_text_group("es")

When text groups are used (component ``set_text()`` or ``set_groups_text()``,
instead of ``add_text()`` and ``add_all_text()`` above), this selects the
specified group, then updates the user agent text library to the new text
strings. This has the effect of updating all text displayed to the user.

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

HICP disconect()
----------------

::

  hicp.disconnect()

Sends a disconnect command to the user agent. Does not preemptively close the
connection, this allows the user agent time to do any cleanup it wants to, then
send a disconnect event when it's ready.

Event handling
==============

::

  class UpdateButtonHandler:
    def feedback(self, hicp, event_message, component):
        ...optional event feedback...

    def process(self, event_message, component):
        ...optional long term processing...

    def update(self, hicp, event_message, component):
        ...optional update results

  update_button.set_handle_click(UpdateButtonHnadler())

Events are handled in three stages:

Feedback
  Update the user agent display with an indication that the event was received
  and is being processed. Only really needed if processing might take a long
  time, like updating a database or making an API call to a remote server, can
  be omitted if not needed.

Process
  Any long term operation is handled in a separate thread, allowing any other
  events to be handled meanwhile. Can be omitted if not needed.

Update
  Update the user agent display with the results of the event processing, if
  there are any. This is normally where windows would be opened or closed,
  component contents would be updated, and so on, but there might be rare cases
  where there's no update necessary, so this is also optional.

``feedback()`` an ``update()`` stages are handled in the same thread, while all
``process()`` stages are in a separate thread. This means:

- All event ``feedback()``, ``process()``, and ``update()`` stages always
  happen in that order.

- All event ``feedback()`` stages are run in the order they are received.

- All event ``process() `` stages are run in the order they are received.

- ``update()`` stages might run in a different order than received.
  Specifically events with no ``process()`` handler will skip directly to
  ``update()`` while the previous event is busy.

- No two ``feedback()`` or ``update()`` stages from any event will run at the
  same time.

- No two ``process()`` stages from any event will run at the same time.

- ``process()`` stages might run at the same time as another event's
  ``feedback()`` or ``update()`` stages (but never its own).

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

::

  lc.set_text_id(5)
  window.add(lc, 2, 3)

  lc.set_text_id(6)
  lc.update()

This sets the component displayed text to the text in the user agent text
library indicated by the ID number (added previously using HICP ``add_text()``
or ``add_all_text()``).

This doesn't support text group, so shouldn't be mixed with ``set_text()`` or
``set_groups_text()``.

Component set_text() and set_groups_text()
------------------------------------------

::

  lc.set_text("Name:", hicp)
  window.add(lc, 2, 3)

  lc.set_groups_text({ "en": "Name", "fr": "Nom" }, hicp)
  window.add(lc, 2, 3)

  lc.set_groups_text({ "en": "New Name", "fr": "Nuveau Nom" }, hicp)
  lc.update()

``set_text(t, hicp)`` is the equivalent to ``set_groups_text({ default_group,
t}, hicp})``, it adds text for the current text group and no others. Can be
used if there is no multilingual support needed.

In ``set_groups_text()``, "groups" is plural, don't forget. It stores the given
texts for all text groups (automatically assigns the same ID for them all), and
updates user agant with the text for the current group. When the HICP text
group is changed, the user agent is updated with the correct texts for the new
group.

Window
======

::

  from hicp import Window, Button

  wc = WelcomeCloser()

  w = Window()
  w.set_text("Welcome")  # Window frame title.
  w.set_handle_close(wc)
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

There should be a ``remove()``, but I haven't done that yet.

Window set_visible()
--------------------

::

  w.set_visible(True)

When set to ``True`` makes the user agent display the window and contents. The
window can be added and constructed, then made visible when it's complete. When set to ``False`` the window will not be displayed.

Window set_handle_close()
-------------------------

::

  class CloseHandler:
    def feedback(self, hicp, event_message, component):
        ...optional event feedback...

    def process(self, event_message, component):
        ...optional long term processing...

    def update(self, hicp, event_message, component):
        hicp.remove(component)

  w.set_handle_close(CloseHandler())

When a window's "close" control on the frame is clicked, it sends a "close"
event. This handler should remove the window (closing it) or make it invisible
if it might be opened again. Closing the last window can also disconnect the
application (call ``hicp.disconnect()``).

Label
=====

::

  l = Label()
  l.set_text("Welcome")

  w.add(l, 0, 0)

A label just displays text. It must be added to a container component (like a
window or panel).

Button
======

::

  from hicp import Button

  hb = ...button click handler...

  b = Button()
  b.set_text("Activate")
  b.set_handle_click(hb)

  w.add(b, 0, 1)

A button can be clicked to send an event that the specified handler processes.
The text is just displayed on the button. It must be added to a container
component (like a window or panel).

Button set_handle_click()
-------------------------

::

  class ActivateHandler:
    def feedback(self, hicp, event_message, component):
        ...optional event feedback...

    def process(self, event_message, component):
        ...optional long term processing...

    def update(self, hicp, event_message, component):
        ...optional update results

  b.set_handle_click(ActivateHandler())

The event handler is called when a button's click event is received, as
described above.

TextField
=========

::

  from hicp import TextField

  user = ...an object with a .name string...

  htc = ...handler for text field changed content...

  tf = TextField()
  tf.set_content(user.name)
  tf.set_handle_changed(htc)

  w.add(tf, 1, 2)

A text field displays a single line of text content, and allows it to be edited
by the user. When editing is finished, a changed event is sent.

The text contents of a text field can have various attributes set, but user
agents typically don't support text with attributes for fields, only for multi
line text panels, but if there are attributes they are guaranteed to be
preserved and correct after the text content is changed.

TextField set_content()
-----------------------

TextField set_attribute()
-------------------------

TextField set_handle_changed()
------------------------------


