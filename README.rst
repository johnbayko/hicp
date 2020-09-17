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

