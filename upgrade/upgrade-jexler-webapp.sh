#!/bin/bash

# Helper script for upgrading a jexler webapp to a newer version.

# The old webapp must be unpacked (i.e. a directory, not a WAR),
# the new webapp can be unpacked or a WAR file, and both must be
# placed in the same directory which must be the current working
# directory when running the script:
#   <current-working-dir>
#     <old-webapp-name>
#     <new-webapp-name>[.war]

# Moves everything of the old webapp except the WEB-INF/jexlers
# directory (and except hidden files in the webapp directory and
# its WEB-INF directory) to <webapp-name>-saved-<date+time>;
# unzips the new webapp if only a WAR is present; copies everything
# from the new webapp except the WEB-INF/jexlers directory to
# <webapp-name>. Note that if manual changes had been made in the
# old webapp  (e.g. in the web.xml or in a JSP), those would have
# to be merged manually afterwards.

set -e
trap "printf \"\e[31;1mFAILED\e[0m\n\"" EXIT

# correct number of parameters?
if [ "$#" != "2" ]; then
  echo "usage: $0 <old-webapp-name> <new-webapp-name>"
  echo "example: $0 jexler jexler-1.0.5"
  exit 1
fi

# print parameters
ROOT=$(pwd)
WEBAPP_OLD=$1
WEBAPP_NEW=$2
echo "ROOT: '$ROOT"
echo "WEBAPP_OLD: '$WEBAPP_OLD'"
echo "WEBAPP_NEW: '$WEBAPP_NEW'"

# validate parameters
if [ ! -d "$ROOT/$WEBAPP_OLD" ]; then
  echo "ERROR: old webapp does not exist"
  ERROR="1"
fi
if [ ! -d "$ROOT/$WEBAPP_NEW" -a ! -f "$ROOT/$WEBAPP_NEW.war" ]; then
  echo "ERROR: new webapp does not exist (neither directory nor WAR found)"
  ERROR="1"
fi
if [ "$WEBAPP_OLD" == "$WEBAPP_NEW" ]; then
  echo "ERROR: old and new webapp have the same name"
  ERROR="1"
fi
if [ "$ERROR" == "1" ]; then
  exit 1
fi

# unzip new webapp, if necessary
if [ ! -d "$ROOT/$WEBAPP_NEW" ]; then
  mkdir "$ROOT/$WEBAPP_NEW"
  cd "$ROOT/$WEBAPP_NEW"
  jar xf "../$WEBAPP_NEW.war"
  #unzip -q "$ROOT/$WEBAPP_NEW.war" -d "$ROOT/$WEBAPP_NEW"
fi

# move old files away from old webapp directory
WEBAPP_SAVED="$WEBAPP_OLD-saved-$(date +%Y%m%d-%H%M%S)"
echo "WEBAPP_SAVED: '$WEBAPP_SAVED'"
mkdir "$ROOT/$WEBAPP_SAVED"
cd "$ROOT/$WEBAPP_OLD"
for FILE in $(ls); do
  if [ "$FILE" != "WEB-INF" ]; then
    mv "$FILE" "$ROOT/$WEBAPP_SAVED"
  fi
done
mkdir "$ROOT/$WEBAPP_SAVED/WEB-INF"
cd "$ROOT/$WEBAPP_OLD/WEB-INF"
for FILE in $(ls); do
  if [ "$FILE" != "jexlers" ]; then
    mv "$FILE" "$ROOT/$WEBAPP_SAVED/WEB-INF"
  fi
done
cd ../..

# copy new files to old webapp directory
cd "$ROOT/$WEBAPP_NEW"
for FILE in $(ls); do
  if [ "$FILE" != "WEB-INF" ]; then
    cp -r "$FILE" "$ROOT/$WEBAPP_OLD"
  fi
done
cd "$ROOT/$WEBAPP_NEW/WEB-INF"
for FILE in $(ls); do
  if [ "$FILE" != "jexlers" ]; then
    cp -r "$FILE" "$ROOT/$WEBAPP_OLD/WEB-INF"
  fi
done

# show contents of updated "old" webapp
ls -l "$ROOT/$WEBAPP_OLD"
ls -l "$ROOT/$WEBAPP_OLD/WEB-INF"

printf "\e[32;1mSUCCESS\e[0m\n"
printf "\e[34;1mNOTE\e[0m: any manual changes to merge? (web.xml? JSPs? ...?)\n"

trap - EXIT
