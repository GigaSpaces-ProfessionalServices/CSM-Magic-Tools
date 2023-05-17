@echo off
setlocal EnableDelayedExpansion

call setenvredolog.bat
echo %spaceHostsFileName%
echo %redoLogScriptName%

for /f %%i in ('jq length %spaceHostsFileName%') do set jsonLen=%%i

set /a jsonLen=%jsonLen%-1

for /l %%i in (0,1,%jsonLen%-1) do (
  for /f %%j in ('jq -r ".[%%i].host" %spaceHostsFileName%') do set host=%%j
  
  for /f %%j in ('jq -r ".[%%i].username" %spaceHostsFileName%') do set username=%%j
  
  for /f %%j in ('jq -r ".[%%i].password" %spaceHostsFileName%') do set password=%%j
	psexec \\!host! -u !username! -p "!password!" cmd /c ""%scriptLocation%%redoLogScriptName%" "%scriptLocation%""

)
