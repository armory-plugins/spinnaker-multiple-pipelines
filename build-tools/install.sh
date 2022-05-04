#!/bin/sh

set -ex

echo "Installing plugin $PLUGIN_FILE"

VERSION=$(echo "$PLUGIN_ID" | sed 's/smp-//')

cd /opt/smp

mkdir -p /opt/smp/target/smp/$VERSION
cp /opt/smp/plugins-docker.json /opt/smp/target/smp/plugins.json
cp /opt/smp/$PLUGIN_FILE /opt/smp/target/smp/$VERSION/$PLUGIN_FILE