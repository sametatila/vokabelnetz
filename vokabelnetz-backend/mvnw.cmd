@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@echo off
setlocal EnableDelayedExpansion

set "MAVEN_PROJECTBASEDIR=%~dp0"
@REM Remove trailing backslash
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
    echo Maven Wrapper JAR not found at %WRAPPER_JAR%
    exit /b 1
)

set "JAVA_EXE=java"
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java"
)

%JAVA_EXE% --enable-native-access=ALL-UNNAMED --add-opens=java.base/sun.misc=ALL-UNNAMED -Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR% --class-path "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

endlocal
