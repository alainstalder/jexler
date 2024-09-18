
# https://www.artecat.ch/jexler/

# Dockerfile for "plain" Docker.

# The container will start as root but then switch via entrypoint
# to user "jexler" in group "jexler"; UID and GID can be overridden
# via env JEXLER_UID and JEXLER_GID.

FROM tomcat:jre8-alpine

ENV JEXLER_UID=9090
ENV JEXLER_GID=9090

ENV JEXLER_VERSION=4.1.0
ENV JEXLER_HOME=/jexler
ENV JEXLER_JEXLERS=$JEXLER_HOME/jexlers

ENV JEXLER_WEBAPP_DIR=$CATALINA_HOME/webapps/ROOT
ENV JEXLER_WEBAPP_JEXLERS_DIR=$JEXLER_WEBAPP_DIR/WEB-INF/jexlers

# Directories with files/dirs owned by 9090:9090
ENV JEXLER_9090_DIRS="$JEXLER_HOME $JEXLER_WEBAPP_DIR"

USER root

# Needed for entrypoint (usermod, groupmod, su-exec)
RUN apk add --no-cache shadow su-exec

# Create group and user "jexler"
RUN addgroup --gid "$JEXLER_GID" --system jexler
RUN adduser  --uid "$JEXLER_UID" -H -D -S jexler -G jexler

# Create jexler base directory
RUN X=$JEXLER_HOME && mkdir $X && chown -R jexler:jexler $X

# Create and configure Grape related stuff
RUN X=$JEXLER_HOME/grape && mkdir $X && chown -R jexler:jexler $X && chmod -R u+w $X
ENV JAVA_OPTS="$JAVA_OPTS -Dgrape.root=/jexler/grape/grapes"
RUN X=$JEXLER_HOME/grape/grapes && mkdir $X && chown -R jexler:jexler $X && chmod -R u+w $X
ENV JAVA_OPTS="$JAVA_OPTS -Dgrape.config=/jexler/grape/grapeConfig.xml"
ADD grape/grapeConfig.xml $JEXLER_HOME/grape/grapeConfig.xml
RUN X=$JEXLER_HOME/grape/grapeConfig.xml && chown -R jexler:jexler $X
ADD grape/defaultGrapeConfig.xml $JEXLER_HOME/grape/defaultGrapeConfig.xml
RUN X=$JEXLER_HOME/grape/defaultGrapeConfig.xml && chown -R jexler:jexler $X

# Give write access to Tomcat temp directory
# (needed by Grape, or by Jexler Groovy scripts that would use common Java calls to get a temp file/dir)
RUN X=$CATALINA_HOME/temp && chown -R jexler:jexler $X && chmod -R u+w $X

# Download and unzip the jexler JAR to ROOT webapp
RUN rm -rf $CATALINA_HOME/webapps/*
RUN mkdir $JEXLER_WEBAPP_DIR
RUN wget -q https://github.com/alainstalder/jexler/releases/download/$JEXLER_VERSION/jexler-$JEXLER_VERSION.war -O /tmp/jexler.war
RUN unzip -q -o /tmp/jexler.war -d $JEXLER_WEBAPP_DIR
RUN rm /tmp/jexler.war
RUN X=$JEXLER_WEBAPP_DIR && chown -R jexler:jexler $X

# Move jexlers dir to $JEXLER_JEXLERS, make writeable, create symlink back
RUN mv $JEXLER_WEBAPP_JEXLERS_DIR $JEXLER_JEXLERS
RUN X=$JEXLER_JEXLERS && chown -R jexler:jexler $X && chmod -R u+w $X
RUN ln -s $JEXLER_JEXLERS $JEXLER_WEBAPP_JEXLERS_DIR

ADD entrypoint.sh /entrypoint.sh
RUN chmod 500 /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
