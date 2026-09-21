@echo off
setlocal
call npm ci
if errorlevel 1 exit /b 1
call npx cap sync android
if errorlevel 1 exit /b 1
pushd android
call gradlew.bat assembleRelease
set "build_result=%errorlevel%"
popd
exit /b %build_result%

