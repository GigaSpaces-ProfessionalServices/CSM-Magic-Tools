#!/usr/bin/env bash
GS_HOME=${GS_HOME=`(cd ../../; pwd )`}
$GS_HOME/bin/gs.sh pu deploy bll target/bll-0.1.jar $*