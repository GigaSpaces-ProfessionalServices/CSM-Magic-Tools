#!/usr/bin/env bash
GS_HOME=${GS_HOME=`(cd ../../; pwd )`}
$GS_HOME/bin/gs.sh pu deploy bll target/in_memory_jar-0.1.jar $*