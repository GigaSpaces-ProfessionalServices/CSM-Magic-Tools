set spaceName=dataExampleSpace
set lookupLocators=EC2AMAZ-PUUQMQH
set lookupGroups=xap-16.2.1
set redoLogYaml="C:\Users\Administrator\Documents\redologcontents.yaml"

set gsHome=C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\

set sourcePath=%gsHome%Work\redo-log\%spaceName%
set targetPath=%gsHome%backup\work\redo-log\%spaceName%\
set targetPathBaseDir=%gsHome%backup\
set deserializeFullPath=%gsHome%backup\

set spaceHostsFileName=spaceHosts.txt
set redoLogScriptName=copyRedoLogScript.bat
set deserializeScriptName=deserializeScript.bat

set scriptLocation=C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\automation-redo-log\

set deserailizeFullFilePath="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\backup\deserializeRedolog"

set assemblyFileName="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\Deploy\DataProcessor\GigaSpaces.Examples.ProcessingUnit.Common.dll"