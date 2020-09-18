===================================
Holistic Interface Control Protocol
===================================

Overview
========

Holistic Interface Control Protocol (HICP) just communicates events and
commands between a server and a user agent (client) over a persistent
connection, to let a server application create and interact with a user
interface on the client system in a way that looks local. It's intended to be
like a graphical interface version of ssh + ANSI terminal communicaiton.

The idea is not new, but usually is implemented at a much lower level, such as
X windows or remote desktop systems. Instead this is defined at the level of
typical user interface controls and layouts, which lets the interface be
defined more abstractly, and gives the client more freedom of interface
presentation. It's also a lot less communication intensive.

This is in contreast to HTML and Javascript based applications where the
application is downloaded to a client and runs there, but is heavily integrated
with one or more servers to function. This usually results in a large up front
download, plus additional communications with multiple connections opened and
closed, which has additional overhead.

Contents
========

This repository contains:

  docs
    - Documentation for the protocol itself.

  java
    - A library of general classes, mainly HICP messages.
    - A library of classes for implementing an HICP client.
    - An HICP test viewer (client).

  python
    - A module of HICP classes, mainly HICP GUI components and messages.
    - An HICP test server application.

Usage
=====

Requires:

- Python 3.
- Java (14 works for sure)
- ant

On MacOS these are easiest to install with Homebrew. Install Homebrew first,
obviously, but since it can be installed without Homebrew, it's not in the
"requires" list.

To run the sample server and client:

- Compile hv with "ant dist". Run using the "hv" script. A window will open to
  accept connection parameters.
- Run the receptionx.py server with the "go" script. It will print the socket
  port number allocated to it.
- Add parameters to the hv window:
  - Host of the system receptionx.py is being run on, "localhost" if it's the
    same host.
  - Port number printed by receptionx.py.
  - Test users and passwords are in the very low security
    python/reception/users file. Default is "user1"/"password1".
  - The example server doesn't have any applications yet, so leave the
    application field blank to run the default application ("reception").

