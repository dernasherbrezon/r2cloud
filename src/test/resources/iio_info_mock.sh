#!/bin/bash

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
	-a)
	    curl -s http://localhost:8003/t
		shift 
		;;
	*)
		shift 1
		;;
esac
done