@echo off
setlocal EnableDelayedExpansion

call setenvredolog.bat
rem set spaceName=dataExampleSpace
rem set sourcePath="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\Work\redo-log\" %spaceName%
rem set targetPath="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\backup\work\redo-log\"%spaceName%\
rem set deserailizeFullFilePath="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\backup\deserializeRedolog1"


set "search_dir=%targetPath%"
set "pattern=*container*"
set "exclude1=wal$"
set "exclude2=shm$"
set "exclude3=map$"
set "exclude4=_1$"

for /f "delims=" %%i in ('dir /b /s %search_dir%%pattern% ^| findstr /v /i /r /c:"%exclude1%" /c:"%exclude2%" /c:"%exclude3%" /c:"%exclude4%"') do (
	rem set file1=%%i 
	rem  echo aa!file1!b 
	for %%f in ("%%i" "%%i_1") do (
	  if not defined largest (
		set "largest=%%~nxf"
		set /a "size=%%~zf"
	  ) else (
		set /a "current=%%~zf"
		if !current! GTR !size! (
		  set "largest=%%~nxf"
		  set "size=!current!"
		)
	  )
	)
	echo !largest!

	set var1=!largest!
	set var2=!var1:*%spaceName%=!
	echo %spaceName%!var2!
	rem java -jar redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar DeserializeRedoLog --spaceName=%spaceName% --containerName=%spaceName%!var2! --outputFileName=%deserailizeFullFilePath%	
	
	java -Dcom.gs.home="C:\GigaSpaces\smart-cache.net-16.2.1-x64\NET v4.0\backup" -jar redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar DeserializeRedoLog1 %spaceName% %spaceName%!var2! %deserailizeFullFilePath%!var2!	
		
)



rem contruct filename map n_1-> partion n and without _ in filename
rem EG dataExampleSpace~1_1 -> sqlite_storage_redo_log_dataExampleSpace_container1
rem	dataExampleSpace~1_2 -> sqlite_storage_redo_log_dataExampleSpace_container1_1

rem primary space partitions only required

rem THen run deserailize by going to each host and pass parameter spacename, container part constructed above, targetFilename 


rem Later Next to run ProcessRedoLog of .net version with param from each host
pause