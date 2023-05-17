@echo off
setlocal EnableDelayedExpansion

call setenvredolog.bat

set "gsHome=%gsHome:\=\\%"
set "scriptLocation=%scriptLocation:\=\\%"
set "targetPath=%targetPath:\=\\%"
set "targetPathBaseDir=%targetPathBaseDir:\=\\%"
set "deserializeFullPath=%deserializeFullPath:\=\\%"

java -jar RemoteConnection-1.0-SNAPSHOT-jar-with-dependencies.jar DeserializeRedoLog --spaceName=%spaceName% --gsHome="%gsHome% " --scriptLocation="%scriptLocation% " --targetDir="%targetPath% " --targetPathBaseDir="%targetPathBaseDir% " --deserializeFullPath="%deserializeFullPath% "

rem java -jar RemoteConnection-1.0-SNAPSHOT-jar-with-dependencies.jar DeserializeRedoLog --spaceName=dataExampleSpace "--gsHome=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\" "--scriptLocation=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\automation-redo-log\\" "--targetDir=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\work\\redo-log\\dataExampleSpace\\" "--targetPathBaseDir=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\" "--deserializeFullPath=C:\\GigaSpaces\\smart-cache.net-16.2.1-x64\\NET v4.0\\backup\\"
