@echo off
setlocal
set "JAVA_HOME=%JAVA_HOME_17%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0mvnw.cmd" %*
endlocal