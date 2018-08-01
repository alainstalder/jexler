#!/bin/bash

set -e -u
trap "echo \"$(tput bold)`basename \"$0\"` failed\$(tput sgr 0)\" >&2" EXIT

cd "${0%/*}"

# build doc...
../../gradlew clean asciidoctor

# rename
cd build/asciidoc
mv html5 guide
cd guide
mv guide.html index.html

#tput bel
date

trap - EXIT



