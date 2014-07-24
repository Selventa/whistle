#!/usr/bin/env bash

if [ -z "${JAVA_OPTS}" ]; then
    JAVA_OPTS="-Xmx1024m -Dderby.stream.error.field=org.openbel.framework.common.enums.DatabaseType.NULL_OUTPUT_STREAM"
else
    JAVA_OPTS="$JAVA_OPTS -Dderby.stream.error.field=org.openbel.framework.common.enums.DatabaseType.NULL_OUTPUT_STREAM"
fi

export WHISTLE_HOME="$(dirname $0)"
java $JAVA_OPTS -jar "$WHISTLE_HOME/whistle-${project.version}.jar" "$@"
