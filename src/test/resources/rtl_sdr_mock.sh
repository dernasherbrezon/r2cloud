#!/bin/bash

echo "command line: $@"

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
	-f|-s|-g|-p)
		shift 2
		;;
	*)
		outputFile=$1
		shift 1
		;;
esac
done

if [ -z "$outputFile" ]; then
	exit 1
fi

echo "processing: ${outputFile}"
# this is rtlsdrdata server
curl http://localhost:8002/ >> $outputFile

