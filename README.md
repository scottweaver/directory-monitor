# Scala Coding Test:

Here’s a short write up of the directory watcher problem:

## Problem:
Given a list of directories, set up a system to watch those directories
for new or modified files.  Files are named as follows “<customer
name>-<timestamp>.txt".

Whenever a file is created or modified in a watched directory, an alert
should be sent out with the directory, file name, and event that occurred
(create or modified).
Only the top level directory indicated should be watched (don’t
recursively watch sub directories).

## Requirements:
An external library may be used for actually watching directories.
Alerts should be logged to a log file.
The list of directories should be stored in a config file, which can
change (with directories added, removed, or changed) changes should be
picked up and adjust what we are watching without a restart.

We expect to get a file from our customers every 1 minute.  List of our
customers is available as a configuration file (assume any format you
want).  We would like to get an alert if you don’t see a new file or an
update to an existing file from one of our customers in expected time
period.