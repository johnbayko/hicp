=========
Why HICP?
=========

Sometimes it seems people try to avoid solving a problem by adding complexity
until it looks like something else, just hiding the problem underneath. That's
what it looked like to me since I first saw web applications trying to be
interactive, back in the mid 1990s.

The initial solution was "apps" in the form of a plugin for a complex
interpreter like Flash or Java, and a binary downloaded and run on a virtual
machine. The runtime was typically large with support for a lot of functions
not used by any particular app, making page loads very slow, startup fairly
slow, and execution also slow. Bigger problems were ensuring plugin versions
were up to date, avoiding malware pretending to be plugin updates, and general
overall system compatibility differences. Apps couldn't count on features being
available on all browsers, and users were constantly pestered to upgrade or
install a plugin when only trying to view a new web site.

By contrast, web pages were mostly designed to be static forms, with fields,
buttons, and some controls which could submit a request to a server, and
receive an updated page back. Displayed data would become out of date if the
user did not submit something or manually refresh the page. And any change had
to make a round trip to a server and back.

Basically, this was a return to batch processing of previous generations.
Specifically, it resembled a graphical version of the IBM 3270 terminal
protocol, except the IBM terminals had a constant connection and could be
updated by server events, web pages couldn't.

The other thing a consistent connection allowed was to identify that it was a
single user making the batch requests. Each web page request was a separate
connection, request, response, and disconnection. To identify that it's the
same user, identifying information had to be added as request parameters, or
using cookies to keep track, or other hidden data. Most had security issues,
but eventually server frameworks and tools were developed to do it correctly.

In other words, a large amount of effort was spent to make a batch protocol
look like a connection based protocol, rather than just switching to an
existing connection based protocol and avoiding the complexity and security
problems the various workarounds added. Even stranger, given that the batch
protocol (HTTP) used a connection based protocol (TCP) for its requests. So
that was:

- A connection based protocol (user sessions)
- On top of a batch protocol (HTTP)
- On top of a connection based protocol (TCP)
- On top of another batch protocol (IP)

Javascript didn't fix this, it just added another layer to hide the batch
nature from the users. It allowed a lot of processing to happen on the client,
like the plugin apps, but without the plugin problems. Communication with the
server was still in batch messages, but split up to the level of individual
displayed values, and polling the server for updates so the user didn't have
to.

The increased complexity of the code needed to do this had to be hidden again,
in Javascript libraries and frameworks. That helped developers (to a degree -
keeping the illusion up still requires challenging concepts like "observable"
values and functional processing streams), but it made for a lot more work for
the servers and networks, both in terms of the huge number of requests and the
much larger initial page download. But improvements in performance and capacity
meant at least these added problems were not noticeable like they were in the
past.

It's still adding complexity to hide problems, rather avoiding the fundamental
thing causing the problems in the first place. Rather than basing all this on a
batch style processing model, going back to a single persistent connection
would simplify things. Command line interfaces have done this since before the
1980s.

"Command line" is not accurate, since the interface lets applications take up
an entire terminal screen with interactive applications, over a simple protocol
that basically does these simple things:

- Indicate where to put characters on the screen.

- Put a character on a screen.

- Send user events (keys) back to the server.

Server side frameworks (like "curses") can provide more complex interfaces for
developers (curses allows for text based windows). The result is a variety of
full screen editors, games, mail utilities, and so on (in any programming
language), with very little system usage, and far less complexity than most
modern web pages.

HICP is meant to be a GUI version of this. A single persistent connection with
simple operations to construct a graphical display, and report events to the
server. A library in any programming language can implement the protocol, and
implement applications in a similar way.

I've started on a demo of this idea here (<https://github.com/johnbayko/hicp>),
I'll describe a bit more of it later.
