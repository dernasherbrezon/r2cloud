#!/bin/bash

while [[ $# -gt 0 ]]
do
key="$1"
case $key in
	-f)
		FREQUENCY="$2"
		shift 2
		;;
	-s)
		SAMPLE_RATE="$2"
		shift 2
		;;
	-g)
		GAIN="$2"
		shift 2
		;;
	-o)
		OUTPUT="$2"
		shift 2
		;;
	-cli)
		PLUTO_CLI="$2"
		shift 2
		;;
esac
done

_term() {
  kill -TERM "$rtl" 2>/dev/null
  #tail --pid=$rtl -f /dev/null
}

trap _term SIGTERM

set -o pipefail

CMD="${PLUTO_CLI} -f ${FREQUENCY} -s ${SAMPLE_RATE} -b 16384 -g ${GAIN} -"
${CMD} | gzip > ${OUTPUT} &

rtl=$(jobs -p)
child=$! 
wait "$child"
