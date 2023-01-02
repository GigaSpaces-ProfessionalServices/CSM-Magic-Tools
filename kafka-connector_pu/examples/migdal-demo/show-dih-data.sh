#!/usr/bin/env bash

$GS_HOME/bin/gs.sh space query demo testDB.dbo.orders 2>/dev/null
$GS_HOME/bin/gs.sh space query demo testDB.dbo.customers 2>/dev/null
$GS_HOME/bin/gs.sh space query demo testDB.dbo.products 2>/dev/null
$GS_HOME/bin/gs.sh space query demo testDB.dbo.cities 2>/dev/null

