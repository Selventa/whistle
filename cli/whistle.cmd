@echo off

if not defined JAVA_OPTS (
    set JAVA_OPTS=-Xmx1024m -Dderby.stream.error.field=org.openbel.framework.common.enums.DatabaseType.NULL_OUTPUT_STREAM
) else (
    set JAVA_OPTS=%JAVA_OPTS% -Dderby.stream.error.field=org.openbel.framework.common.enums.DatabaseType.NULL_OUTPUT_STREAM
)

set WHISTLE_HOME=%~dp0
java %JAVA_OPTS% -jar "%WHISTLE_HOME%\whistle-${project.version}.jar" %*
