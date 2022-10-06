#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import sys
import threading
import itertools
import time
import sqlite3
from sqlite3 import Error

class Spinner:

    def __init__(self, message, delay=0.1):
        self.spinner = itertools.cycle(['|', '/', '-', '\\'])
        self.delay = delay
        self.busy = False
        self.spinner_visible = False
        sys.stdout.write(message)

    def write_next(self):
        with self._screen_lock:
            if not self.spinner_visible:
                sys.stdout.write(next(self.spinner))
                self.spinner_visible = True
                sys.stdout.flush()

    def remove_spinner(self, cleanup=False):
        with self._screen_lock:
            if self.spinner_visible:
                sys.stdout.write('\b')
                self.spinner_visible = False
                if cleanup:
                    sys.stdout.write(' ')       # overwrite spinner with blank
                    sys.stdout.write('\r')      # move to next line
                sys.stdout.flush()

    def spinner_task(self):
        while self.busy:
            self.write_next()
            time.sleep(self.delay)
            self.remove_spinner()

    def __enter__(self):
        if sys.stdout.isatty():
            self._screen_lock = threading.Lock()
            self.busy = True
            self.thread = threading.Thread(target=self.spinner_task)
            self.thread.start()

    def __exit__(self, exception, value, tb):
        if sys.stdout.isatty():
            self.busy = False
            self.remove_spinner(cleanup=True)
        else:
            sys.stdout.write('\r')


class MySQLite:

    def __init__(self, db_file):
        self.db = db_file
        self.home = os.path.dirname(os.path.realpath(self.db))
        if not os.path.exists(self.home):
            try:
                os.makedirs(self.home)
            except OSError as e:
                if 'Errno 13' in str(e):
                    print(f"\n{e}\n *try changing the path in config.yaml")
                else:
                    print(e)
                exit(1)

    def connect(self):
        conn = None
        try:
            conn = sqlite3.connect(self.db)
        except Error as e:
            print(e)
        return conn

    def create(self, sql):
        try:
            conn = self.connect()
            c = conn.cursor()
            c.execute(sql)
        except Error as e:
            print(e)
    
    def select(self, sql):
        try:
            conn = self.connect()
            c = conn.cursor()
            c.execute(sql)
        except Error as e:
            print(e)
        else:
            result = c.fetchall()
            return result

    def insert(self, sql, data):
        try:
            conn = self.connect()
            c = conn.cursor()
            c.execute(sql, data)
        except Error as e:
            print(e)
        else:
            conn.commit()
            return c.lastrowid

    def delete(self, sql):
        try:
            conn = self.connect()
            c = conn.cursor()
            c.execute(sql)
        except Error as e:
            print(e)
        else:
            conn.commit()
            return c.lastrowid
