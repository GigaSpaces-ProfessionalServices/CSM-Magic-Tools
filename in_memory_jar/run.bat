@echo off
SETLOCAL
if not defined GS_HOME set GS_HOME=%~dp0..\..
call %GS_HOME%\bin\gs.bat pu run %~dp0\target\in_memory_jar-0.1.jar %*