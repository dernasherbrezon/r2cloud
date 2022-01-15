#!/bin/bash

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
	-t)
	    curl -s http://localhost:8003/t 1>&2
		shift 
		;;
	*)
		shift 1
		;;
esac
done