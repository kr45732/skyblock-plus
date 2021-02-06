@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  SkyblockPlus startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and SKYBLOCK_PLUS_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\SkyblockPlus-0.0.1.jar;%APP_HOME%\lib\JDA-4.2.0_227.jar;%APP_HOME%\lib\spring-boot-starter-web-2.4.2.jar;%APP_HOME%\lib\spring-boot-starter-data-jpa-2.4.2.jar;%APP_HOME%\lib\spring-boot-starter-aop-2.4.2.jar;%APP_HOME%\lib\spring-boot-starter-jdbc-2.4.2.jar;%APP_HOME%\lib\spring-boot-starter-json-2.4.2.jar;%APP_HOME%\lib\spring-boot-starter-2.4.2.jar;%APP_HOME%\lib\spring-boot-starter-logging-2.4.2.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\gson-2.8.6.jar;%APP_HOME%\lib\Nedit-1.0.0.jar;%APP_HOME%\lib\postgresql-42.2.18.jar;%APP_HOME%\lib\jda-utilities-examples-3.0.5.jar;%APP_HOME%\lib\jda-utilities-command-3.0.5.jar;%APP_HOME%\lib\jda-utilities-menu-3.0.5.jar;%APP_HOME%\lib\jda-utilities-oauth2-3.0.5.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\annotations-19.0.0.jar;%APP_HOME%\lib\spring-data-jpa-2.4.3.jar;%APP_HOME%\lib\HikariCP-3.4.5.jar;%APP_HOME%\lib\spring-data-commons-2.4.3.jar;%APP_HOME%\lib\log4j-to-slf4j-2.13.3.jar;%APP_HOME%\lib\jul-to-slf4j-1.7.30.jar;%APP_HOME%\lib\slf4j-api-1.7.30.jar;%APP_HOME%\lib\nv-websocket-client-2.10.jar;%APP_HOME%\lib\okhttp-3.14.9.jar;%APP_HOME%\lib\opus-java-1.0.4.pom;%APP_HOME%\lib\commons-collections4-4.1.jar;%APP_HOME%\lib\trove4j-3.0.3.jar;%APP_HOME%\lib\jackson-datatype-jdk8-2.11.4.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.11.4.jar;%APP_HOME%\lib\jackson-module-parameter-names-2.11.4.jar;%APP_HOME%\lib\jackson-databind-2.11.4.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\jda-utilities-doc-3.0.5.jar;%APP_HOME%\lib\jda-utilities-commons-3.0.5.jar;%APP_HOME%\lib\lombok-1.18.16.jar;%APP_HOME%\lib\jakarta.transaction-api-1.3.3.jar;%APP_HOME%\lib\jakarta.persistence-api-2.2.3.jar;%APP_HOME%\lib\hibernate-core-5.4.27.Final.jar;%APP_HOME%\lib\spring-aspects-5.3.3.jar;%APP_HOME%\lib\spring-boot-starter-tomcat-2.4.2.jar;%APP_HOME%\lib\spring-webmvc-5.3.3.jar;%APP_HOME%\lib\spring-web-5.3.3.jar;%APP_HOME%\lib\checker-qual-3.5.0.jar;%APP_HOME%\lib\okio-1.17.2.jar;%APP_HOME%\lib\opus-java-api-1.0.4.jar;%APP_HOME%\lib\opus-java-natives-1.0.4.jar;%APP_HOME%\lib\jackson-annotations-2.11.4.jar;%APP_HOME%\lib\jackson-core-2.11.4.jar;%APP_HOME%\lib\json-20160810.jar;%APP_HOME%\lib\spring-boot-autoconfigure-2.4.2.jar;%APP_HOME%\lib\spring-boot-2.4.2.jar;%APP_HOME%\lib\spring-context-5.3.3.jar;%APP_HOME%\lib\spring-aop-5.3.3.jar;%APP_HOME%\lib\aspectjweaver-1.9.6.jar;%APP_HOME%\lib\spring-orm-5.3.3.jar;%APP_HOME%\lib\spring-jdbc-5.3.3.jar;%APP_HOME%\lib\hibernate-commons-annotations-5.1.2.Final.jar;%APP_HOME%\lib\jboss-logging-3.4.1.Final.jar;%APP_HOME%\lib\javassist-3.27.0-GA.jar;%APP_HOME%\lib\byte-buddy-1.10.19.jar;%APP_HOME%\lib\antlr-2.7.7.jar;%APP_HOME%\lib\jandex-2.1.3.Final.jar;%APP_HOME%\lib\classmate-1.5.1.jar;%APP_HOME%\lib\dom4j-2.1.3.jar;%APP_HOME%\lib\jaxb-runtime-2.3.3.jar;%APP_HOME%\lib\spring-tx-5.3.3.jar;%APP_HOME%\lib\spring-beans-5.3.3.jar;%APP_HOME%\lib\spring-expression-5.3.3.jar;%APP_HOME%\lib\spring-core-5.3.3.jar;%APP_HOME%\lib\jakarta.annotation-api-1.3.5.jar;%APP_HOME%\lib\snakeyaml-1.27.jar;%APP_HOME%\lib\tomcat-embed-websocket-9.0.41.jar;%APP_HOME%\lib\tomcat-embed-core-9.0.41.jar;%APP_HOME%\lib\jakarta.el-3.0.3.jar;%APP_HOME%\lib\jna-4.4.0.jar;%APP_HOME%\lib\jakarta.xml.bind-api-2.3.3.jar;%APP_HOME%\lib\txw2-2.3.3.jar;%APP_HOME%\lib\istack-commons-runtime-3.0.11.jar;%APP_HOME%\lib\jakarta.activation-1.2.2.jar;%APP_HOME%\lib\spring-jcl-5.3.3.jar;%APP_HOME%\lib\log4j-api-2.13.3.jar

@rem Execute SkyblockPlus
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SKYBLOCK_PLUS_OPTS%  -classpath "%CLASSPATH%" com.skyblockplus.Main %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SKYBLOCK_PLUS_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SKYBLOCK_PLUS_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
