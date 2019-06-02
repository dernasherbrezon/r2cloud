#!/bin/bash

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
	-i|-o|-e)
		shift 2
		;;
	*)
		$@
		break
		;;
esac
done