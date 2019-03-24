#!/bin/bash

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
	-t)
		echo "  0:  Realtek, RTL2838UHIDIR, SN: 00000001"
		shift 
		;;
	-p2)
		echo "real sample rate: 2048118 current PPM: 58 cumulative PPM: 53"
		shift
		;;
	*)
		shift 1
		;;
esac
done