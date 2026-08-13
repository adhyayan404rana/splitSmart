@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@ECHO OFF
@SETLOCAL EnableAvailableExpansion

IF "%1"=="-h" GOTO help
IF "%1"=="--help" GOTO help

SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO BASEDIRSET

SET MAVEN_PROJECTBASEDIR=%CD%
:findBaseDir
IF EXIST "%MAVEN_PROJECTBASEDIR%\.mvn" GOTO BASEDIRSET
FOR %%i IN ("%MAVEN_PROJECTBASEDIR%") DO SET MAVEN_PROJECTBASEDIR=%%~dpi
SET MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO findBaseDir

:BASEDIRSET
SET MAVEN_WRAPPER_SETTING=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties

IF NOT EXIST "%MAVEN_WRAPPER_SETTING%" (
  ECHO Could not find %MAVEN_WRAPPER_SETTING%
  EXIT /B 1
)

FOR /F "tokens=1,2 delims==" %%A IN ('TYPE "%MAVEN_WRAPPER_SETTING%"') DO (
  IF "%%A"=="wrapperUrl" SET MAVEN_WRAPPER_URL=%%B
  IF "%%A"=="distributionUrl" SET MAVEN_DISTRIBUTION_URL=%%B
)

IF "%MAVEN_WRAPPER_URL%"=="" SET MAVEN_WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar

IF EXIST "%WRAPPER_JAR%" GOTO RUN

ECHO Downloading %MAVEN_WRAPPER_URL%...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%MAVEN_WRAPPER_URL%', '%WRAPPER_JAR%')"

:RUN
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Error downloading %WRAPPER_JAR%
  EXIT /B 1
)

IF "%JAVA_HOME%"=="" (
  SET JAVA_EXE=java
) ELSE (
  SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

%JAVA_EXE% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*

EXIT /B %ERRORLEVEL%

:help
ECHO Usage: mvnw [options] [goal] [phase]
