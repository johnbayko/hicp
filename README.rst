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

This is meant to be a quick-and-dirty sample implementation, it's not error
tolerant, doesn't have proper unit testing, and is not really maintainable. So
please don't use it as an example of my best work, it's not (though there is
some good stuff in there).

This repository contains:

  docs
    - Documentation for the protocol itself.
    - An overview of the Python HICP package and how to use it.

  java
    - A library of general classes, mainly HICP messages.
    - A library of classes for implementing an HICP client.
    - An HICP test viewer (client).

  python
    - A module of HICP classes, mainly HICP GUI components and messages.
    - An HICP test server application.

Usage
=====

Run from repository
-------------------

Requires:

- Python 3

  - Python 3 pip (pip3)

- Java (14 works for sure)
- ant

On MacOS these are easiest to install with Homebrew. Install Homebrew first,
obviously, but since these can be installed without Homebrew, it's not in the
"requires" list.

To run the sample server and client:

- Compile hv with "ant dist". Run using the "hv" script. A window will open to
  accept connection parameters.
- Run the receptionx.py server with the "go" script (MacOS, Linux, Unix). It
  will print the socket port number allocated to it.
- Add parameters to the hv window.

  - Host of the system receptionx.py is being run on, "localhost" or blank if
    it's the same host.
  - Port number printed by receptionx.py.
  - Test users and passwords are in the very low security
    python/reception/users file. Default is "user1"/"password1".
  - The example server has "test" and "testml", or leave the application field
    blank to run the default application, which will let you choose which
    application to run.

Installation packages
---------------------

Python
~~~~~~

A python distribution package for hicp can be created with the ``makedist``
script.

Java
~~~~

The ``ant`` build script can make an installation package using ``jpackage``
[https://openjdk.java.net/jeps/343]. The requirements are the same as for
``jpackage`` (e.g. for Windows, the "Wix" utility), plus:

  MacOS
    - ``iconutil`` utility installed wth Xcode.

