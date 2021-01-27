=============================
Python HICP interface library
=============================

Introduction
============

This allows HICP messages to be sent and received in Python to produce an
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

``in_stream`` is a file object which implements ``read()``, ``out_stream`` is a
file object that implements ``write()``. Typically using sockets, a file object
can be created from a connected socket by calling ``makefile()``, and used for
both ``in_stream`` and ``out_stream``.

The apps in ``app_list`` should extend ``App``, and all need to implement
``connected(self, hicp)``. That is called after a connection is made and an app
is selected, and is where the
components are created and added together, along with event handlers. The app
continues to listen for events and call the handlers until one of the handlers
calls ``hicp.disconnect()``.

An app can also implement ``authenticate(self, hicp, message)``, which is
called if there is no authenticator specified. The app could put up a window to
log in, though that window would be insecure so is not generally a good idea
unless no security is needed.

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

HICP add_text() and add_all_text()
----------------------------------

::

  hicp.add_text(1, "Name:")

  hicp.add_all_text({ 1: "Name:", 2: "Position:" })

There are two ways of setting text in a component that supports it. The first
way is to add the text and an ID number, then set the component text using
``set_text_id()``. The other method handles assigning text IDs automatically,
and is described below. That's an easier way and should be used unless there's
a need to do it this way. It's important not to mix the two methods unless you
really know what you're doing.

``add_text()`` Adds a text string with a sp[ecific ID number to the user agent
text library, to be used by a component to be added later.

``add_all_text()`` specifies multiple IDs and values to add to the user agent
text library. Text can be added in multiple parts, previously added text is not
removed.

When using text ID numbers, HICP does not keep track of text group, so they
should not be mixed. To use the text group, the text and group information is
added directly to each component (described below).

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

This applies only to the second way of setting component text (component
``set_text()`` or ``set_groups_text()`` described below), this selects the
specified group, then updates the user agent text library to the new text
strings. This has the effect of updating all text displayed to the user.  It's
important not to mix the two methods unless you really know what you're doing.

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
library indicated by the ID number (added previously using HICP ``add_text()``
or ``add_all_text()``).

This doesn't support text group, so shouldn't be mixed with ``set_text()`` or
``set_groups_text()``.

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

Component set_size()
--------------------

::

  from hicp import Label, Button

  l = Label()
  l.set_text("Options:")
  l.set_size(3, 1)  # Label is wide as three option buttons below it
  w.add(l, 0, 0)

  b1 = Button()
  b1.set_text("One")
  b1.set_handle_click(OptionOneHandler())
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

Positions start at 0 and go up to 255.

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

TextField set_handle_changed()
------------------------------

::

  class PriceEnteredHandler:
    def __init__(transaction):
      self.transaction = transaction

    def feedback(self, hicp, event_message, text_field):
        ...optional event feedback...

    def process(self, event_message, text_field):
        ...optional long term processing...

    def update(self, hicp, event_message, text_field):
        try:
          price_str = text_field.get_content()
          self.transaction.price = int(price_str)
        except:
          # Not a valid price or content not changed, do not update.
          pass

  tr = ...an object with a .price field...

  tf.set_handle_changed(PriceEnteredHandler(tr))

Once editing finishes, a changed event is sent which contains the changed text
and current attributes, if any. This is normally the entire text content once
editing is complete, not individual changes.

The text field is updated with the changed contents and attributes before the
handler functions are called.

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

