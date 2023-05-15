@echo off
setlocal EnableDelayedExpansion

echo %1 >> "C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\op1.txt"

call %1setenvredolog.bat
echo %targetPath% >> "C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\op1.txt"

rem set gsHome="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\"
rem set spaceName=dataExampleSpace
rem set sourcePath=%gsHome%"Work\redo-log\"%spaceName%
rem set targetPath=%gsHome%"backup\work\redo-log\"%spaceName%
if not exist %targetPath% (md %targetPath%)
echo %targetPath% >> "C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\op1.txt"
xcopy "%sourcePath%" "%targetPath%" /s /e /h

echo "hello2" >> "C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\op1.txt"
