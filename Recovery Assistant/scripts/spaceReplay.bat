@echo off
setlocal EnableDelayedExpansion

call setenvredolog.bat
rem set spaceName=dataExampleSpace

ReadRedoLogContents.exe --spaceName=%spaceName% --lookupLocators=%lookupLocators% --lookupGroups=%lookupGroups% --redoLogYaml=%redoLogYaml% --assemblyFileName=%assemblyFileName%