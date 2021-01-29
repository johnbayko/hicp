===================
Python hicpd daemon
===================

``hicpd`` is a daemon for testing the Python HICP implementation. It can be run
from ``bash`` or similar shell using the ``go`` script.

``hicpd`` uses the ``HICPPATH`` environment variable to determine the root for
any relative paths, including finding app classes, or ``.`` if it's not set.

An app consists of any class found either in the ``default_app`` direcrory, or
any subdirectory of ``apps`` directory, which extends the ``App`` hicp class
(as expected, any app in ``detault_app`` is used as the default, if there is
none then another is chosen arbitrarily). It's possible to have more than one
app in a directory by having more than one class extending ``App``, but if
that's done in the ``default_app`` directory then an arbutraty app is chosen as
the default.

When an app is added to the ``apps`` directory, the server needs to be
restarted to find and load the new app.

When an app is run, the current directory is changed to the app directory,
allowing the app to find any related resources such as configuratio files,
text, and other types (e.g. images) in the future.

As a test server, only one app runs at a time. It has a command line interface
to exit.

