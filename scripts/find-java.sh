#!/bin/bash

#
# Copyright 2004 AcademSoft Ltd.
# Author: Alexander Litvinov
#

#
# This script will try to determinate JAVA_HOME
#

# List of java homes. Taken from SuSE 9.1 and manual java rpm instalation
# BAD variants :
# /usr/lib/SunJava2 (may point to 1.3.x java)
# /usr/lib/java2 (may point to 1.3.x java)
# /usr/lib/java (may point to 1.3.x java)
STANDART_JAVA_HOMES="
/usr/java/jdk1.5.0_16/
"

NEED_JAVAC=no
case "$1" in
    -javac)
        NEED_JAVAC=yes
        ;;
esac

#
# functions
#
function is_java_home() {
    if [ -d "$1" -a -x "$1/bin/java" ]; then
        if "$1/bin/java" -version 2>&1 | grep 'java version' &> /dev/null; then 
	    if "$1/bin/java" -version 2>&1 | grep -i 'gcj'; then
	    	return 1;
	    fi
            if [ "$NEED_JAVAC" = "yes" ]; then
                if [ -x "$1/bin/javac" -a -f "$1/lib/tools.jar" ]; then
                    return 0
                else
                    return 1
                fi
            else
                return 0
            fi
        fi
    fi

    return 1
} 

function set_java_home() {
    export JAVA_HOME=`cd "$1"; pwd`
    export PATH=$JAVA_HOME/bin:$PATH
}

#
# MAIN code
#


# check if it already exists
if [ -n "$JAVA_HOME" ]; then
    if ! is_java_home "$JAVA_HOME"; then
        unset JAVA_HOME
    fi
fi

# may be in PATH ?
if [ -z "$JAVA_HOME" ]; then
    JAVA=`which java 2> /dev/null`
    if [ -n "$JAVA" ]; then
        JAVA_BINDIR=`dirname $JAVA`
        if is_java_home "$JAVA_BINDIR/.."; then
            set_java_home $JAVA_BINDIR/..
        fi
    fi
fi

# check list of pre defined paths
if [ -z "$JAVA_HOME" ]; then
    for test_home in $STANDART_JAVA_HOMES; do
        if is_java_home $test_home; then
            set_java_home $test_home
            break
        fi
    done
fi

if [ -z "$JAVA_HOME" ]; then
    echo JAVA_HOME can not be located
fi

