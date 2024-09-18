#!/bin/bash
set -e

# set new IDs
usermod -u $JEXLER_UID jexler
groupmod -g $JEXLER_GID jexler

# adapt IDs of existing files/dirs
for DIR in $JEXLER_9090_DIRS; do
  find $DIR -group 9090 -exec chgrp -h jexler {} \;
  find $DIR -user 9090 -exec chown -h jexler {} \;
done

su-exec jexler $CATALINA_HOME/bin/catalina.sh run
