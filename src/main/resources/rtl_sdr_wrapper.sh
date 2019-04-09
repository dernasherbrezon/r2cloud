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

CMD="${RTL_SDR} -f ${FREQUENCY} -s ${SAMPLE_RATE} -p ${PPM} -g ${GAIN} -"
echo "running: ${CMD}" >> /dev/stderr
${CMD} | gzip > ${OUTPUT}