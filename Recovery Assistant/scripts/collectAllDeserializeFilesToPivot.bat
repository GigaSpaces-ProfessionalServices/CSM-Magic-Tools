@echo off
setlocal EnableDelayedExpansion

call setenvredolog.bat

set "gsHome=%gsHome:\=\\%"
set "scriptLocation=%scriptLocation:\=\\%"
set "targetPath=%targetPath:\=\\%"
set "targetPathBaseDir=%targetPathBaseDir:\=\\%"
set "deserializeFullPath=%deserializeFullPath:\=\\%"

java -jar RemoteConnection-1.0-SNAPSHOT-jar-with-dependencies.jar DownloadRemotefiles --spaceName=%spaceName% --gsHome="%gsHome% " --scriptLocation="%scriptLocation% " --targetDir="%targetPath% " --targetPathBaseDir="%targetPathBaseDir% " --deserializeFullPath="%deserializeFullPath% "
