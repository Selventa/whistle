@echo off

if not defined JAVA_OPTS (
    set JAVA_OPTS=-Xmx1024m -Dderby.stream.error.field=com.selventa.belframework.common.enums.DatabaseType.NULL_OUTPUT_STREAM
) else (
    set JAVA_OPTS=%JAVA_OPTS% -Dderby.stream.error.field=com.selventa.belframework.common.enums.DatabaseType.NULL_OUTPUT_STREAM
)

java %JAVA_OPTS% -jar whistle-${project.version}.jar %*
