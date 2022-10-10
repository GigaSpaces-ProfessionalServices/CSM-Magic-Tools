#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
cp_print: print related collection of functions
"""

import os
import pyfiglet
from colorama import Fore, Style


def print_header():
    """ print menu header - figlet and VERSION """

    SPC = ' ' * 2
    VERSION = "ODS Cockpit 2022, v1.1 | Copyright Gigaspaces Ltd"
    #os.system("clear")
    print(pyfiglet.figlet_format("ODS Cockpit", font='slant'))
    print(f"{SPC}{VERSION}\n\n")


def pretty_print(string, color, style=None):
    """
    pretty print
    :param string: the string to pretify
    :param color: the color to print
    :param style: the style to apply
    """

    color = eval('Fore.' + f'{color}'.upper())
    if style is None:
        print(f"{color}{string}{Style.RESET_ALL}")
    else:
        style = eval('Style.' + f'{style}'.upper())
        print(f"{color}{style}{string}{Style.RESET_ALL}")


def print_locations(selections, dictionary):
    """
    print locations line accordding to menu positions
    :param selections: the selections list
    :param dictionary: dictionary of menu items
    :return:
    """
    index = ""
    location = "@:: MAIN".upper()
    for item in selections:
        index += f"[{str(item)}]"
        location += " :: " + str(eval(f"dictionary{index}['id']")).upper()
    print_header()
    pretty_print(f'{location}\n', 'green', 'bright')
