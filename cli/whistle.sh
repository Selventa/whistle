#!/usr/bin/env bash

if [ -z "${JAVA_OPTS}" ]; then
    JAVA_OPTS="-Xmx1024m -Dderby.stream.error.field=com.selventa.belframework.common.enums.DatabaseType.NULL_OUTPUT_STREAM"
else
    JAVA_OPTS="$JAVA_OPTS -Dderby.stream.error.field=com.selventa.belframework.common.enums.DatabaseType.NULL_OUTPUT_STREAM"
fi

java $JAVA_OPTS -jar whistle-${project.version}.jar "$@"
