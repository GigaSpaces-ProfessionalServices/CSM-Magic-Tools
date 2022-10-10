#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
classes: custom classes module
"""

import os
import sys
import threading
import itertools
import time
import sqlite3
from sqlite3 import Error

class Spinner:
    """
    spinner class to show spinner while executing something
    """
    def __init__(self, message, delay=0.1):
        self.spinner = itertools.cycle(['|', '/', '-', '\\'])
        self.delay = delay
        self.busy = False
        self.spinner_visible = False
        self._screen_lock = threading.Lock()
        self.thread = threading.Thread(target=self.spinner_task)
        sys.stdout.write(message)

    def write_next(self):
        """
        write the next char for spinner
        """
        with self._screen_lock:
            if not self.spinner_visible:
                sys.stdout.write(next(self.spinner))
                self.spinner_visible = True
                sys.stdout.flush()

    def remove_spinner(self, cleanup=False):
        """
        delete the spinner
        """
        with self._screen_lock:
            if self.spinner_visible:
                sys.stdout.write('\b')
                self.spinner_visible = False
                if cleanup:
                    sys.stdout.write(' ')       # overwrite spinner with blank
                    sys.stdout.write('\r')      # move to next line
                sys.stdout.flush()

    def spinner_task(self):
        """
        task routine: run -> wait -> remove
        """
        while self.busy:
            self.write_next()
            time.sleep(self.delay)
            self.remove_spinner()

    def __enter__(self):
        """
        context manager func
        """
        if sys.stdout.isatty():
            self.busy = True
            self.thread.start()

    def __exit__(self, _exception, _value, _tb):
        """
        context manager func
        """
        if sys.stdout.isatty():
            self.busy = False
            self.remove_spinner(cleanup=True)
        else:
            sys.stdout.write('\r')


class MySQLite:
    """
    custom SQLite class to allow working with sqlite3 database
    """
    def __init__(self, _db_file):
        self._db = _db_file
        self.home = os.path.dirname(os.path.realpath(self._db))
        if not os.path.exists(self.home):
            try:
                os.makedirs(self.home)
            except OSError as err:
                if 'Errno 13' in str(err):
                    print(f"\n{err}\n *try changing the path in config.yaml")
                else:
                    print(err)
                sys.exit(1)

    def connect(self):
        """
        connect to database
        """
        _c = None
        try:
            _c = sqlite3.connect(self._db)
        except Error as err:
            print(err)
        return _c

    def create(self, _sql):
        """
        execute database create calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql)
        except Error as err:
            print(err)

    def select(self, _sql):
        """
        execute database select calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql)
        except Error as err:
            print(err)
            return None
        else:
            return _c.fetchall()

    def insert(self, _sql, _data):
        """
        execute database insert calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql, _data)
        except Error as err:
            print(err)
            return None
        else:
            _conn.commit()
            return _c.lastrowid

    def delete(self, _sql):
        """
        execute database delete calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql)
        except Error as err:
            print(err)
            return None
        else:
            _conn.commit()
            return _c.lastrowid
