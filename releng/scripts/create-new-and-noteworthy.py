#!/usr/bin/env python3
###############################################################################
# Copyright (c) 2019, 2024 Ericsson
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0
###############################################################################

import io
from os import getlogin
import subprocess
import re
import argparse
import datetime
from datetime import date

def valid_date(s):
    try:
        # Try to parse the string into a date time with the format YYYY-MM-DD
        datetime.datetime.strptime(s, "%Y-%m-%d")
        return s
    except ValueError:
        msg = "not a valid date: {0!r}".format(s)
        raise argparse.ArgumentTypeError(msg)

date_time = date.today().strftime("%Y-%m-%d")
report = dict()
parser = argparse.ArgumentParser(description = 'Generates a new and noteworthy in markdown from a git tree using two dates (yyyy-MM-dd).')
parser.add_argument('-a','--after',  help = 'Include commits after and including this specific date (yyyy-MM-dd)', required = True, type=valid_date)
parser.add_argument('-b','--before', help = '(Optional) Include commits before and including this specific date (yyyy-MM-dd)', required = False,  default = date_time, type=valid_date)

args = parser.parse_args()

tagPattern = re.compile("^\[([A-Za-z]*)\]\s*(.*\S)\s*")

notTags = ["main"]
startHead = "date:"
isItStartHead = False
commitMessage = ""
hasTags = False

def update_entry(entry, line):
    if entry not in report:
        report[entry] = list()

    report[entry].append(line)   

def process_line(line):
    global isItStartHead
    global startHead
    global commitMessage
    global hasTags
    global endOfCommit

    # This mark the end of a commit header, the next non-empty line is the commit message
    if line.lower().startswith(startHead):
        if hasTags is False and len(commitMessage) != 0:
            update_entry("unknown", commitMessage)

        isItStartHead = True
        hasTags = False
        commitMessage = ""
        return

    # This is the commit message, store in case no tags are found
    if isItStartHead and len(line) != 0:
        commitMessage = line
        isItStartHead = False
        return 

    # Check if the line contains some tags
    matcher = tagPattern.match(line.strip())
    if matcher is not None:
        tag = matcher.group(1).strip()

        # If the line contains a valid tag, add it to the report
        if tag not in notTags:
            update_entry(tag, matcher.group(2))
            hasTags = True

if __name__=='__main__':
    after = args.after
    before = args.before
    cmd = ['git', '--no-pager','log', '--after', after, '--until', before]
    proc = subprocess.Popen(cmd, stdout = subprocess.PIPE)
    commit = ""

    for line in io.TextIOWrapper(proc.stdout, encoding = "utf-8"):
        try:
            line = line.strip()
            if (line.startswith("commit")):
                commit = line[len("commit "):].strip()
            process_line(line)
        except UnicodeDecodeError as e:
            print ("Error {0} in {1}, could not parse commit message {2}".format(e, commit, line))
    print ("New and Noteworthy for {0} to {1}\n".format(after, before))
    print ("# NewInxx.y")
    for entry in report:
        print ("\n## " + entry.title() + "\n")
        for line in report[entry]:
             print("- " + line)
