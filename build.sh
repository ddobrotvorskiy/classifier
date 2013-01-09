#!/bin/bash

BASEDIR=`dirname $0`

PROJECT_PATH=`cd $BASEDIR; pwd`

[ -f $PROJECT_PATH/scripts/find-java.sh ] && . $PROJECT_PATH/scripts/find-java.sh -javac

export ANT_OPTS="-Xmx256m"
ant -emacs -f $PROJECT_PATH/classifier.xml $@
