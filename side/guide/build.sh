#!/bin/bash

set -e -u
trap "echo \"$(tput bold)`basename \"$0\"` failed$(tput sgr 0)\" >&2" EXIT

cd "${0%/*}"

# build doc...
../../gradlew clean asciidoctor

# rename
cd build/asciidoc
mv html5 guide
cd guide
mv guide.html index.html

# swallow the black footer with date+time
mv index.html work.html
cat work.html | awk '
  /<div id=.footer.>/ { swallow=1 }
  /<.body>/ { swallow=0 }
  /.*/ {
    if (!swallow) { print }
  }' > index.html
rm work.html

#tput bel
date

trap - EXIT
