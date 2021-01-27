@echo off

rem Script to run the Workflow Component Creator.
rem Two optional args:
rem    1. the directory that contains the Templates and CommonLibraries directories [default is cwd]
rem    2. the name (with path) of the properties file which specifies info for new component [default is ./Templates/wcc.properties]
rem Look at ./Templates/wcc.properties for how to configure the properties file.

set USAGE="runWCC.bat [dir] [propFile]"

set PWD=%~dp0

set DIR=%1
set PROPFILE=%2

if "%DIR%"=="-help" (
    echo Usage: %USAGE%
    exit /b
)

if "%DIR%"=="" (
    echo Using default value for Directory
    set DIR=%PWD%
) else set DIR=%DIR%\

echo Directory is: %DIR%

if "%PROPFILE%"=="" (
    echo Using default value for properties file
    set PROPFILE=%DIR%Templates\wcc.properties.R
)

echo Using properties file: %PROPFILE%

cd %DIR%

set CP=%DIR%CommonLibraries\datashop.jar;%DIR%CommonLibraries\commons-io-1.2.jar;%DIR%CommonLibraries\commons-lang-2.2.jar;%DIR%CommonLibraries\log4j-1.2.13.jar;%DIR%CommonLibraries\xercesImpl.jar
echo CP is: %CP%

set TEMPLATESDIR=%DIR%Templates
set CLASSNAME=edu.cmu.pslc.datashop.extractors.workflows.WorkflowComponentCreator
echo Classname is: %CLASSNAME%

echo "C:/Program Files/Java/jdk1.8.0_261/bin/java.exe" -cp %TEMPLATESDIR%;%CP% %CLASSNAME% -file %PROPFILE% -dir %DIR%
"C:/Program Files/Java/jdk1.8.0_261/bin/java.exe" -cp %TEMPLATESDIR%;%CP% %CLASSNAME% -file %PROPFILE% -dir %DIR%
