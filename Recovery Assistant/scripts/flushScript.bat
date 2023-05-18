@echo off
setlocal EnableDelayedExpansion

call "%~dp0\setenvredolog.bat"
rem set spaceName=dataExampleSpace
 
java -jar redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar FlushRedoLogToDisk --spaceName=%spaceName% 
