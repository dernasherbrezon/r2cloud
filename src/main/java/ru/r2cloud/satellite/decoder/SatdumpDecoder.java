package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.fec.ccsds.UncorrectableException;
import ru.r2cloud.jradio.util.IOUtils;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.InstrumentChannel;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;
import ru.r2cloud.util.SatdumpLogProcessor;

public class SatdumpDecoder implements Decoder {

	private static final Logger LOG = LoggerFactory.getLogger(SatdumpDecoder.class);
	private static final int SYNCWORD_LENGTH = 4;

	private final ProcessFactory factory;
	private final Configuration config;

	public SatdumpDecoder(Configuration config, ProcessFactory factory) {
		this.factory = factory;
		this.config = config;
	}

	@Override
	public DecoderResult decode(File rawFile, Observation request, final Transmitter transmitter, final Satellite satellite) {
		DecoderResult result = new DecoderResult();
		if (!rawFile.exists() || rawFile.length() == 0) {
			return result;
		}
		result.setIq(rawFile);
		File data = find(rawFile.getParentFile().listFiles(), ".cadu");
		if (data != null) {
			if (!processCadu(data, request, transmitter, satellite)) {
				return result;
			}
		} else {
			if (!processBaseband(rawFile, request, transmitter, satellite)) {
				return result;
			}
			data = find(rawFile.getParentFile().listFiles(), ".cadu");
		}
		boolean imagesAvailable = true;
		if (transmitter.getBeaconSizeBytes() > 0) {
			int numberOfDecodedPackets = 0;
			if (data != null && transmitter.getSatdumpCaduSizeBytes() != 0) {
				numberOfDecodedPackets = (int) (data.length() / transmitter.getSatdumpCaduSizeBytes());
				if (data.length() % transmitter.getSatdumpCaduSizeBytes() != 0) {
					LOG.warn("[{}] unexpected number of bytes in the data file. number of packets is incorrect", request.getId());
				}
				if (transmitter.getBeaconClass() != null) {
					// make beaconstream-compatible data file
					File dataFile = new File(config.getTempDirectory(), "data.bin");
					try (InputStream input = new BufferedInputStream(new FileInputStream(data)); BeaconOutputStream bos = new BeaconOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)))) {
						byte[] cadu = new byte[transmitter.getSatdumpCaduSizeBytes()];
						// beacon doesn't include ASM and ReedSolomon
						byte[] beaconBytes = new byte[transmitter.getBeaconSizeBytes()];
						while (true) {
							try {
								IOUtils.readFully(input, cadu);
								System.arraycopy(cadu, SYNCWORD_LENGTH, beaconBytes, 0, beaconBytes.length);
								Beacon cur = transmitter.getBeaconClass().getDeclaredConstructor().newInstance();
								cur.readExternal(beaconBytes);
								bos.write(cur);
							} catch (UncorrectableException e) {
								continue;
							} catch (IOException e) {
								break;
							}
						}
						data = dataFile;
					} catch (Exception e) {
						LOG.error("[{}] can't convert to beacon", request.getId(), e);
					}
				}
			}
			result.setNumberOfDecodedPackets(numberOfDecodedPackets);
			if (data != null) {
				result.setTotalSize(data.length());
			}
			if (numberOfDecodedPackets > 0) {
				result.setData(data);
			}
			imagesAvailable = (numberOfDecodedPackets > 0);
		}
		if (satellite.getInstruments() != null && imagesAvailable) {
			File base = rawFile.getParentFile();
			List<Instrument> instruments = new ArrayList<>(satellite.getInstruments().size());
			for (Instrument cur : satellite.getInstruments()) {
				if (!cur.isEnabled()) {
					continue;
				}
				File instrumentDir = new File(base, cur.getSatdumpName());
				if (!instrumentDir.exists()) {
					continue;
				}
				Instrument enrichedInstrument = new Instrument(cur);
				if (cur.getChannels() != null) {
					List<InstrumentChannel> availableChannels = new ArrayList<>(cur.getChannels().size());
					for (InstrumentChannel curChannel : cur.getChannels()) {
						File channelFile = new File(instrumentDir, curChannel.getSatdumpName());
						if (!channelFile.exists()) {
							continue;
						}
						InstrumentChannel enriched = new InstrumentChannel(curChannel);
						enriched.setImage(channelFile);
						availableChannels.add(enriched);
					}
					if (availableChannels.isEmpty()) {
						continue;
					}
					enrichedInstrument.setChannels(availableChannels);
				} else if (cur.isSeries()) {
					Pattern pattern = Pattern.compile(cur.getSatdumpImageSeries());
					List<File> imageSeries = new ArrayList<>();
					List<File> allFiles = new ArrayList<>();
					for (File curFile : instrumentDir.listFiles()) {
						allFiles.add(curFile);
					}
					Collections.sort(allFiles, new Comparator<File>() {
						@Override
						public int compare(File o1, File o2) {
							return o1.getName().compareTo(o2.getName());
						}
					});
					for (File curSeries : allFiles) {
						if (pattern.matcher(curSeries.getName()).matches()) {
							imageSeries.add(curSeries);
						}
					}
					enrichedInstrument.setImageSeries(imageSeries);
				}
				if (cur.getSatdumpCombined() != null) {
					File combined = new File(instrumentDir, cur.getSatdumpCombined());
					if (combined.exists()) {
						enrichedInstrument.setCombinedImage(combined);
					}
				}
				instruments.add(enrichedInstrument);
			}
			if (!instruments.isEmpty()) {
				result.setInstruments(instruments);
			}
		}

		return result;
	}

	private boolean processCadu(File cadu, Observation request, final Transmitter transmitter, final Satellite satellite) {
		ProcessWrapper process = null;
		String taskset = config.getProperty("satellites.taskset.path");
		if (taskset == null) {
			taskset = "";
		}
		String commandLine = taskset + " " + config.getProperty("satellites.satdump.path") + " " + transmitter.getSatdumpPipeline() + " cadu " + cadu.getAbsolutePath() + " " + cadu.getParentFile().getAbsolutePath();
		if (satellite.getSatdumpSatelliteNumber() != null) {
			commandLine += " --satellite_number " + satellite.getSatdumpSatelliteNumber();
		}
		try {
			process = factory.create(commandLine.trim(), true, false);
			new SatdumpLogProcessor(request.getId(), process.getInputStream(), "satdump-decode-stdio").start();
			new SatdumpLogProcessor(request.getId(), process.getErrorStream(), "satdump-decode-stderr").start();
			int responseCode = process.waitFor();
			if (responseCode != 0 && responseCode != 1) {
				LOG.info("[{}] invalid response code from satdump. assume no data: {}", request.getId(), responseCode);
				return false;
			}
			return true;
		} catch (IOException e) {
			LOG.error("[{}] unable to decode", request.getId(), e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private boolean processBaseband(File rawFile, Observation request, final Transmitter transmitter, final Satellite satellite) {
		ProcessWrapper process = null;
		String taskset = config.getProperty("satellites.taskset.path");
		if (taskset == null) {
			taskset = "";
		}
		String commandLine = taskset + " " + config.getProperty("satellites.satdump.path") + " " + transmitter.getSatdumpPipeline() + " baseband " + rawFile.getAbsolutePath() + " " + rawFile.getParentFile().getAbsolutePath() + " --dc_block true --samplerate " + request.getSampleRate()
				+ " --baseband_format " + request.getDataFormat().getSatdump();
		if (satellite.getSatdumpSatelliteNumber() != null) {
			commandLine += " --satellite_number " + satellite.getSatdumpSatelliteNumber();
		}
		commandLine += " --start_timestamp " + request.getStartTimeMillis();
		try {
			process = factory.create(commandLine.trim(), true, false);
			new SatdumpLogProcessor(request.getId(), process.getInputStream(), "satdump-decode-stdio").start();
			new SatdumpLogProcessor(request.getId(), process.getErrorStream(), "satdump-decode-stderr").start();
			int responseCode = process.waitFor();
			if (responseCode != 0 && responseCode != 1) {
				LOG.info("[{}] invalid response code from satdump. assume no data: {}", request.getId(), responseCode);
				return false;
			}
			return true;
		} catch (IOException e) {
			LOG.error("[{}] unable to decode", request.getId(), e);
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static File find(File[] files, String extension) {
		for (File cur : files) {
			if (cur.getName().endsWith(extension)) {
				return cur;
			}
		}
		return null;
	}

}
