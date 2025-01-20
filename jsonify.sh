#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2020, 2025 Ericsson
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
#*******************************************************************************

if [ -z "$1" ] || [ -z "$2" ] ; then
    echo "usage: jsonify.sh input output"
    exit 1
fi
if [ ! -f "$1" ]; then
    echo "$1 file not found!"
    exit 1
fi
if [ ! -f "jsonify.py" ]; then
    wget https://raw.githubusercontent.com/eclipse-tracecompass/trace-event-logger/refs/heads/main/jsonify.py
fi
if [ ! -x "jsonify.py" ]; then
    chmod +x jsonify.py
fi
./jsonify.py $1 $2
