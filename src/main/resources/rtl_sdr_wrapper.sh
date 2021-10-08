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
	-p)
		PPM="$2"
		shift 2
		;;
	-g)
		GAIN="$2"
		shift 2
		;;
	-d)
		DEVICE_INDEX="$2"
		shift 2
		;;
	-o)
		OUTPUT="$2"
		shift 2
		;;
	-rtl)
		RTL_SDR="$2"
		shift 2
		;;
esac
done

_term() {
  kill -TERM "$rtl" 2>/dev/null
  tail --pid=$rtl -f /dev/null
}

trap _term SIGTERM

set -o pipefail

CMD="${RTL_SDR} -f ${FREQUENCY} -d ${DEVICE_INDEX} -s ${SAMPLE_RATE} -p ${PPM} -g ${GAIN} -"
${CMD} | gzip > ${OUTPUT} &

rtl=$(jobs -p)
child=$! 
wait "$child"
