@echo off
setlocal EnableDelayedExpansion

call setenvredolog.bat
rem set gsHome="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\"
rem set spaceHostsFileName=spaceHosts.txt
rem set redoLogScriptName=copyRedoLogScript.bat

rem for /F %i in (%spaceHostsFileName%) do start /B cmd /C "psexec \\%i -c %redoLogScriptName%"
echo %spaceHostsFileName%
echo %redoLogScriptName%
rem for /F %i in (%spaceHostsFileName%) do start "echo %redoLogScriptName%"

rem psexec @%spaceHostsFileName% -c  %redoLogScriptName%


rem for /f "usebackq delims=" %%i in (`jq ".[]" %spaceHostsFileName%`) do (
rem  for /f "tokens=1,2,3" %%a in ('echo %%i ^| jq ".host, .username, .password"') do (
rem    set host=%%a
  rem  set username=%%b
    rem set password=%%c
   rem  echo "a--%host%--"
   rem psexec \\%host% -u %username% -p %password% ipconfig
  )
 rem  psexec \\%host% -u %username% -p "%password%" ipconfig
rem )

rem for /f "usebackq tokens=1-3 delims=,:{} " %%a in (`jq -c ".[] | {host,username,password}" %spaceHostsFileName%`) do (
rem  set host=%%~a
rem  set username=%%~b
rem  set password=%%~c
rem  echo "b--%username%--"
rem )


for /f %%i in ('jq length %spaceHostsFileName%') do set jsonLen=%%i

set /a jsonLen=%jsonLen%-1

for /l %%i in (0,1,%jsonLen%-1) do (
  for /f %%j in ('jq -r ".[%%i].host" %spaceHostsFileName%') do set host=%%j
  
  for /f %%j in ('jq -r ".[%%i].username" %spaceHostsFileName%') do set username=%%j
  
  for /f %%j in ('jq -r ".[%%i].password" %spaceHostsFileName%') do set password=%%j
	psexec \\!host! -u !username! -p "!password!" cmd /c ""%scriptLocation%%redoLogScriptName%" "%scriptLocation%""
echo ---- %targetPath% ---
 rem  psexec \\!host! -u !username! -p "!password!" cmd /c "if not exist "%scriptLocation%" (md "%scriptLocation%")" 
 rem  psexec \\!host! -u !username! -p "!password!" xcopy "%scriptLocation%*" "%scriptLocation%" /C /H /E /K /Y

  rem psexec \\!host! -u !username! -p "!password!" cmd /c "C:\Windows\System32\cmd.exe /c "\\EC2AMAZ-HFABBHO\"%scriptLocation%\copyRedoLogScript.bat" "%targetPath%"""

 rem working psexec \\!host! -u !username! -p "!password!" cmd /c "if not exist "%targetPath%" (md "%targetPath%") && xcopy "%sourcePath%" "%targetPath%" /s /e /h" 
)




rem psexec \\EC2AMAZ-HFABBHO -u Administrator -p "bw(w.zYnof5u79a?*!4jScMO-xU=.zxP" ipconfig
rem psexec \\EC2AMAZ-IQ4R6LO -u Administrator -p "yo!32SOlsfUjwvBTEgM&&M@-lNg-JY)Z" ipconfig
rem psexec \\EC2AMAZ-85QQJ3B -u Administrator -p "7tECUN@c42L$%Gfbd=2s&mFcfx;2-bP(" ipconfig

