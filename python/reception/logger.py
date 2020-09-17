import os
import time

class Logger:
    "A simple logger."

    def __init__(self, logFilePath):
        self.__logFilePath = logFilePath
        self.__timestampFormat = "%Y-%m-%d %H:%M:%S"

    def log(self, msg):
      f = None
      try:
        try:
          # Open file.
          f = open(self.__logFilePath, "a")

          # Write header to log file.
          f.write( "["
              + time.strftime(self.__timestampFormat, time.localtime(time.time()))
              + "]\n"
          )
          # Write message to log file.
          f.write(msg + "\n")
        except IOError:
          # This is the exception logger. If an exception occurs here,
          # there's no way to log an exception, so just give up.
          pass 
      finally:
        if f:
          try:
            # Close file.
            f.close()
          except IOError:
            pass

    def add(self, msg):
      f = None
      try:
        try:
          # Open file.
          f = open(self.__logFilePath, "a")

          # Write message to log file.
          f.write(msg + "\n")
        except IOError:
          # This is the exception logger. If an exception occurs here,
          # there's no way to log an exception, so just give up.
          pass 
      finally:
        if f:
          try:
            # Close file.
            f.close()
          except IOError:
            pass

    def removeLog(self):
        try:
          os.remove(self.__logFilePath)
        except OSError:
          # If the file's not there, that was the goal - not a real
          # exception. Although maybe this just doesn't have permissions
          # to remove it. Add stuff for that later if needed.
          pass

