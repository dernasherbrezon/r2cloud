package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.ax25.Ax25Beacon;
import ru.r2cloud.jradio.ax25.Header;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.jradio.trace.DemodulatorTrace;
import ru.r2cloud.jradio.trace.HdlcFrameStats;
import ru.r2cloud.jradio.trace.HdlcReceiverTrace;
import ru.r2cloud.jradio.trace.TraceContext;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;

public class AstrocastDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

//	@Test
//	public void testSomeData() throws Exception {
//		File wav = TestUtil.setupClasspathResource(tempFolder, "data/astrocast.raw.gz");
//		PredictOreKit predict = new PredictOreKit(config);
//		AstrocastDecoder decoder = new AstrocastDecoder(predict, config);
//		DecoderResult result = decoder.decode(wav, TestUtil.loadObservation("data/astrocast.raw.gz.json").getReq());
//		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
//		assertNotNull(result.getDataPath());
//		assertNotNull(result.getRawPath());
//	}

	public static void main(String[] args) throws Exception {
//		try (BeaconInputStream<SnetBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/a-12.bin")), SnetBeacon.class)) {
//			while( bis.hasNext() ) {
//				SnetBeacon beacon = bis.next();
//				System.out.println("here");
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		NrziDecode nrzi = new NrziDecode(new ArrayByteInput(new byte[] {0,1,1,1,1,1,1,1,0,1,1,0,1,1,0,1,0,0,0,0,0,0,0,1}));
//		NrziDecode nrzi = new NrziDecode(new ArrayByteInput(new byte[] {0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,0}));

//		HdlcReceiver rr = new HdlcReceiver(new ArrayByteInput(new byte[] {0,1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,1,0,1,1,1,1,1,1,0}), 10000);
//		rr.readBytes();
//		NrziDecode nrzi = new NrziDecode(new ArrayByteInput(new byte[] {1,1,0,1,1,0,0,1,0,1,1,0,0,1,0,1,0,1,1,0,1,0,0,1,1,0,1,0,0,1,0}));
//		while( true ) {
//			System.out.println(nrzi.readByte());
//		}
//		byte[] buf = new byte[]16;
//		Header h = new Header(new DataInputStream(new ByteArrayInputStream(buf)));
//		for( int i =0;i<5000;i+=1000 ) {
//		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/git/GASPACS-Comms-Info/GASPACS_48K_AF.wav")));
		// new
//		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_5383342_2022-01-31T04-08-16.wav")));
		// old
		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_5646507_2022-03-21T12-04-55.wav")));
//		FloatToComplex fccc = new FloatToComplex(source);
//		OutputStreamSink sink = new OutputStreamSink(fccc);
//		try (OutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/dernasherbrezon/Downloads/satnogs_5309165_2022-01-18T08-59-14.cf32"))) {
//			sink.process(os);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_edited.wav")));
//		FloatToComplex f2c = new FloatToComplex(source);
//		float[] taps = Firdes.lowPass(1.0, source.getContext().getSampleRate(), source.getContext().getSampleRate() / 2, 500, Window.WIN_HAMMING, 6.76);
//		FrequencyXlatingFIRFilter xlating = new FrequencyXlatingFIRFilter(f2c, taps, 1, 1000);

//		try {
//			FileOutputStream fos = new FileOutputStream("/Users/dernasherbrezon/Downloads/satnogs_4703816_2021-09-08T22-52-24_edited.wav.f32");
////			WavFileSink sink = new WavFileSink(next);
////			sink.process(fos);
//			
//			OutputStreamSink sink = new OutputStreamSink(source);
//			sink.process(fos);
//			
//			sink.close();
//			fos.close();
//		} catch (Exception e) {
//			// TODO: handle exception
//		}

//		int[] DOWNLINK_SPEEDS = new int[] { 1250, 2500, 5000, 12500 };
//		int[] DOWNLINK_SPEEDS = new int[] { 5000, 12500 };
//		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
//		FskDemodulator demod = new FskDemodulator(source, 1250, 5000.0f, 8, 2000, true);
//		SoftToHard s2h = new SoftToHard(demod);
//		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 8, "0010110111010100100101111111110111010011011110110000111100011111", 64 * 8);
//		Smog1Signalling signa = new Smog1Signalling(s2h);
//		result.add(signa);
//		for (int i = 0; i < DOWNLINK_SPEEDS.length; i++) {
//			WavFileSource source1 = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_5309165_2022-01-18T08-59-14.wav")));
////			FloatToComplex f2c1 = new FloatToComplex(source1);
////			GmskDemodulator t128Demod = new GmskDemodulator(f2c1, DOWNLINK_SPEEDS[i], DOWNLINK_SPEEDS[i] * 2.0f, 0.175f * 3, 0.02f, 1, 2000);
////			CorrelateSyncword correlateTag128 = new CorrelateSyncword(t128Demod, 0, "0010110111010100", 260 * 8);
//			FskDemodulator t128Demod = new FskDemodulator(source1, DOWNLINK_SPEEDS[i]);
//			CorrelateSyncword correlateTag128 = new CorrelateSyncword(t128Demod, 6, "001011011101010001100011110001010011010110011001", 260 * 8);
//			Smog1RaCoded raCoded128 = new Smog1RaCoded(t128Demod, 128, 260);
//			result.add(raCoded128);
//
//			WavFileSource source2 = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_5309165_2022-01-18T08-59-14.wav")));
////			FloatToComplex f2c2 = new FloatToComplex(source2);
////			GmskDemodulator t256Demod = new GmskDemodulator(f2c2, DOWNLINK_SPEEDS[i], DOWNLINK_SPEEDS[i] * 2.0f, 0.175f * 3, 0.02f, 1, 2000);
////			CorrelateSyncword correlateTag256 = new CorrelateSyncword(t256Demod, 0, "0010110111010100", 514 * 8);
//			FskDemodulator t256Demod = new FskDemodulator(source2, DOWNLINK_SPEEDS[i]);
//			CorrelateSyncword correlateTag256 = new CorrelateSyncword(t256Demod, 6, "001011011101010001100011110001010011010110011001", 514 * 8);
//			Smog1RaCoded raCoded256 = new Smog1RaCoded(t256Demod, 256, 514);
//			result.add(raCoded256);
//
//			WavFileSource source3 = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_5309165_2022-01-18T08-59-14.wav")));
//			FloatToComplex f2c3 = new FloatToComplex(source3);
//			FskDemodulator atl1ShortDemod = new FskDemodulator(source3, DOWNLINK_SPEEDS[i]);
////			GmskDemodulator atl1ShortDemod = new GmskDemodulator(f2c3, DOWNLINK_SPEEDS[i], DOWNLINK_SPEEDS[i] * 2.0f, 0.175f * 3, 0.02f, 1, 2000);
//			result.add(new Smog1Short(atl1ShortDemod));
//
//			WavFileSource source4 = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_5309165_2022-01-18T08-59-14.wav")));
//			FloatToComplex f2c4 = new FloatToComplex(source4);
//			FskDemodulator atl1Demod = new FskDemodulator(source4, DOWNLINK_SPEEDS[i]);
////			GmskDemodulator atl1Demod = new GmskDemodulator(f2c4, DOWNLINK_SPEEDS[i], DOWNLINK_SPEEDS[i] * 2.0f, 0.175f * 3, 0.045f, 1, 2000);
//			result.add(new Smog1(atl1Demod));
//		}
//
//		for (BeaconSource<? extends Beacon> cur : result) {
//			int total = 0;
//			while (cur.hasNext()) {
//				Object next = cur.next();
//				System.out.println(next);
//				total++;
//			}
//			System.out.println("========");
//			System.out.println(total);
//		}

		TraceContext.instance.setHdlcReceiverTrace(new HdlcReceiverTrace());
		TraceContext.instance.setDemodTrace(new DemodulatorTrace(24));

//		RmsAgc agc = new RmsAgc(source, 2e-2f / 2, 1.0f);
//		MultiplyConst mm = new MultiplyConst(agc, 1.0f);
		FskDemodulator demod = new FskDemodulator(source, 9600, 5000.0f, 1, 2000, true);
//		FskDemodulator demod = new FskDemodulator(source, 2400, 5000.0f, 4, 2000, true);
//		FskDemodulator demod = new FskDemodulator(source, 4800, 5000.0f, Util.convertDecimation(4800), 2000, true);
//		FskDemodulator demod = new FskDemodulator(source, 500, 470.0f, 48, 2000, false);
//		FskDemodulator demod = new FskDemodulator(source, 500, 5000.0f, 24, 2000, true);
//		FloatToComplex f2c = new FloatToComplex(source);
//		RttyDemodulator demod = new RttyDemodulator(f2c, 24, 500, 0, 450);

//		float gainMu = 0.175f * 3;
//		FloatToComplex f2c = new FloatToComplex(source);
//		GmskDemodulator demod = new GmskDemodulator(f2c, 19200, 40000, gainMu, 0.02f, 1, 2000);

//		FloatToComplex f2c = new FloatToComplex(source);
//		BpskDemodulator demod = new BpskDemodulator(f2c, 9600, 1, -9600, false);
//		AfskDemodulator demod = new AfskDemodulator(source, 4800, -1200, 3600, 2);
//		FloatToComplex fc = new FloatToComplex(source);
//		BpskDemodulator demod = new BpskDemodulator(fc, 2400, 4, 1500.0, false);
//		GmskDemodulator demod = new GmskDemodulator(f2c, 4800, 20000, gainMu);
//		SoftToHard s2h = new SoftToHard(demod);
//		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(s2h, 4, "00011010110011111111110000011101", false);
//		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 255 * 8 * 5), 1, Endianness.GR_MSB_FIRST));
////		Astrocast9k6 input = new Astrocast9k6(pdu);
//		Astrocast input = new Astrocast(pdu);

//		SoftToHard bs = new SoftToHard(demod);
//		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(bs, 3, "0010110111010100", false);
//		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, 37 * 8));
//		Lucky7 input = new Lucky7(pdu);

//		Ax100BeaconSource input = new Ax100BeaconSource<>(demod, 255, "11000011101010100110011001010101", CspBeacon.class, false ,true, true); //, false, false, false
//		Ax100BeaconSource input = new Ax100BeaconSource<>(demod, 512, CspBeacon.class); //, false, false, false

//		Spooqy1 input = new Spooqy1(demod);		
//		AfskDemodulator demod = new AfskDemodulator(source, 1200, 500, 1700, 5);
//		Ax25BeaconSource input = new Ax25BeaconSource<>(demodulator, Ax25Beacon.class);
//		GmskDemodulator demodulator = new GmskDemodulator(f2c, 9600, 12000, 0.175f * 3, 0.02f, 1, 2000);
//		SoftToHard bs = new SoftToHard(demodulator);
//		FskDemodulator demod = new FskDemodulator(source, 9600);
//		OpsSat input = new OpsSat(new SoftToHard(demod));
//		Ax25BeaconSource<Ax25Beacon> input = new Ax25BeaconSource<>(demod, Ax25Beacon.class);
		Ax25G3ruhBeaconSource input = new Ax25G3ruhBeaconSource<>(demod, Ax25Beacon.class);
//		Gaspacs input = new Gaspacs(demod);
//		Dstar1Decoder input = new Dstar1Decoder(null, null)

//		SoftToHard s2h = new SoftToHard(demod);
//		InvertBits invert = new InvertBits(s2h);		
//		CorrelateSyncword correlateTag = new CorrelateSyncword(invert, 4, "111011110000111011110000", CMX909bBeacon.MAX_SIZE * 8);
//		Technosat input = new Technosat(correlateTag);
		// 0010 0100
		// 0010010011

		// 111011 0 1111 00011011000

		// 0110111

		// 0010010110

		// 0100010011

//		CorrelateSyncword correlate = new CorrelateSyncword(demod, 1, "0010110111010100", 100 * 8);
//		while (true) {
//			byte[] data = correlate.readBytes();
//			SoftToHard.convertToHard(data);
//			System.out.println(data.length);
//			byte[] mm = new byte[data.length];
//			for (int i = 0, k = 0; i < data.length - 10; k++) {
////				i++;
//				int curByte = 0;
//				for (int j = 0; j < 8; j++) {
//					// MSB
//					curByte <<= 1;
//					curByte |= data[i + j] > 0 ? 1 : 0;
//
//					// LSB
////					curByte |= ((data[i + j] > 0 ? 1 : 0) << j);
//
////				curByte = curByte << 1;
////				curByte += (data[i + j] << j);
////				curByte += (data[i + j] << j);
//				}
//				mm[k] = (byte) curByte;
//				i += 8;
//			}
//			System.out.println(new String(mm, StandardCharsets.US_ASCII));
//			System.out.println("========j");
//		}
//		CorrelateSyncword correlate = new CorrelateSyncword(invert, 5, "111011110000111011110000", CMX909bBeacon.MAX_SIZE * 8);
//		CorrelateSyncword correlate = new CorrelateSyncword(invert, 4, "0101011101100101", CMX909bBeacon.MAX_SIZE * 8);
//		Dstar1 input = new Dstar1(correlate);
//		Beesat4 input = new Beesat4(correlate);
//		Technosat input = new Technosat(correlate);
//		Ax100BeaconSource input = new Ax100BeaconSource<>(demod, 255, "11000011101010100110011001010101", CspBeacon.class, false, true, true);
//		UspBeaconSource input = new UspBeaconSource<>(demod, UspBeacon.class);
//		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(new SoftToHard(demod), 4, "00110101001011100011010100101110", false);
//		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 512 * 8), 1, Endianness.GR_MSB_FIRST));
//		Cc11xxReceiver cc11 = new Cc11xxReceiver(pdu, false, true);
//		while(true) {
//			cc11.readBytes();
//			System.out.println("message");
//		}
//		Strand1 input = new Strand1(demod);
//		Alsat1n input = new Alsat1n(demod);

//		try (OutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/dernasherbrezon/Downloads/gaspacs.bin"))) {
//			BeaconOutputStream bos = new BeaconOutputStream(os);
//			while (input.hasNext()) {
//				bos.write(input.next());
//			}
//			bos.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		int total = 0;
		while (input.hasNext()) {
//			System.out.println(i);
			Object next = input.next();
//			Beacon b = (Beacon) next;
//			UspBeacon b = (UspBeacon)next;
//			byte[] raw = b.getRawData();
//			for (int i = 0; i < 8 && i < raw.length; i++) {
//				System.out.printf("%s ", Integer.toHexString(raw[i] & 0xFF));
//			}
//			System.out.println(b.getHeader());
//			Ax25Beacon bb = (Ax25Beacon) next;

			System.out.println(next);

//			System.out.println(bb.getHeader() + " " + bb.getTypeA() + " " + bb.getTypeB());
//			Spooqy1Beacon bb = (Spooqy1Beacon)next;
//			System.out.println(bytesToHex(bb.getRawData()));
			total++;
//			System.out.println(next);
//		}
		}
		System.out.println("total: " + total);

		logHdlcTrace();
//		for (HdlcFrameStats cur : TraceContext.instance.getHdlcReceiverTrace().getBeaconStats()) {
//			System.out.println(cur.getBeforeFlagsCount() + "  " + cur.getFrame().length + "  " + cur.getAfterFlagsCount());
//		}
//		List<byte[]> trace = TraceContext.instance.getHdlcReceiverTrace().getDemodulatorOutput();
//		for (int i = 0; i < trace.size(); i++) {
//			System.out.println(Arrays.toString(trace.get(i)));
//		}
//		MultiplyConst mm = new MultiplyConst(source, 1.0f);
//		FskDemodulator demodulator = new FskDemodulator(mm, 9600);
////		FloatToComplex fc = new FloatToComplex(source);
////		GmskDemodulator demodulator = new GmskDemodulator(fc, 9600, 20000, 0.175f * 3);
//		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(demodulator, 6, "10010011000010110101000111011110", true);
//		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new FixedLengthTagger(correlateTag, (255 + 3) * 8));
//		AX100Decoder ax100 = new AX100Decoder(pdu, false, true, true);
//		Aistechsat2 ais = new Aistechsat2(ax100);
//		int i =0;
//		while( ais.hasNext() ) {
//			System.out.println(ais.next());
//			i++;
//		}
//		System.out.println(i);
	}

	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	@Test
//	@Ignore
	public void testSomeData2() throws Exception {
		// File wav = TestUtil.setupClasspathResource(tempFolder, "data/suomi.raw.gz");
		// File wav = new
		// File("/Users/dernasherbrezon/Downloads/1585841981897/output.raw.gz-236-239.raw.gz");
		// File meta = new
		// File("/Users/dernasherbrezon/Downloads/1585841981897/output.raw.gz-236-239.raw.gz.json");
//		 File wav = new File("/Users/dernasherbrezon/Downloads/1626652144170-48900/output.raw.gz-205-207.raw.gz");
//		 File meta = new File("/Users/dernasherbrezon/Downloads/1626652144170-48900/output.raw.gz-205-207.raw.gz.json");
//		 File wav = new File("/Users/dernasherbrezon/Downloads/1643478870097-R2CLOUD11/output.raw-335-339.raw.gz");
//		 File meta = new File("/Users/dernasherbrezon/Downloads/1643478870097-R2CLOUD11/output.raw-335-339.raw.gz.json");
		String id = "1648751781985-42761";
//		String id = "1644411370136-39444";

//		File wav = new File("/Users/dernasherbrezon/Downloads/" + id + "/output.raw.gz-387-399.raw.gz");
//		File meta = new File("/Users/dernasherbrezon/Downloads/" + id + "/output.raw.gz-387-399.raw.gz.json");

		TraceContext.instance.setHdlcReceiverTrace(new HdlcReceiverTrace());
		TraceContext.instance.setDemodTrace(new DemodulatorTrace(24));

		File wav = new File("/Users/dernasherbrezon/Downloads/" + id + "/output.raw");
		File meta = new File("/Users/dernasherbrezon/Downloads/" + id + "/meta.json");
		Observation observation = load(meta);
//		observation.setBaudRates(Collections.singletonList(1200));
		PredictOreKit predict = new PredictOreKit(config);
//		BpskAx25G3ruhDecoder decoder =new BpskAx25G3ruhDecoder(predict, config, 9600, Ax25Beacon.class);
//		BpskAx25Decoder decoder = new BpskAx25Decoder(predict, config, 600, Itasat1Beacon.class, new byte[] {0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1});
//		Itasat1Decoder decoder = new Itasat1Decoder(predict, config);
//		OpsSatDecoder decoder = new OpsSatDecoder(predict, config);
//		DelfiPqDecoder decoder = new DelfiPqDecoder(predict, config);	
//		TechnosatDecoder decoder = new TechnosatDecoder(predict, config);
		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, Ax25Beacon.class, new byte[] {0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1});
//		SalsatDecoder decoder = new SalsatDecoder(predict, config);
//		Strand1Decoder decoder = new Strand1Decoder(predict, config);
//		FskAx100Decoder decoder = new FskAx100Decoder(predict, config, 260, Lume1Beacon.class, 4800, 9600);
//		new BpskAx25G3ruhDecoder(predict, props, 9600, UvsqsatBeacon.class)
//		SnetDecoder decoder = new SnetDecoder(predict, config);
//		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, Ax25Beacon.class);
//		AfskAx25Decoder decoder = new AfskAx25Decoder(predict, config, 1200, Ax25Beacon.class, null);
//		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, Ax25Beacon.class, null);
//		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, Ax25Beacon.class, new byte[] {0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1});
//		FoxSlowDecoder decoder = new FoxSlowDecoder<>(predict, config, Fox1CBeacon.class);
//		OpsSatDecoder decoder = new OpsSatDecoder(predict, config);
//		SalsatDecoder decoder = new SalsatDecoder(predict, config);
//		FskAx100Decoder decoder = new FskAx100Decoder(predict, config, 255, CspBeacon.class, 9600);
//		ChompttDecoder decoder = new ChompttDecoder(predict, config);
//		ReaktorHelloWorldDecoder decoder = new ReaktorHelloWorldDecoder(predict, config);
//		Gomx1Decoder decoder = new Gomx1Decoder(predict, config);
//		Aausat4Decoder decoder = new Aausat4Decoder(predict, config);
//		FoxSlowDecoder<Fox1BBeacon> decoder = new FoxSlowDecoder<>(predict, config, Fox1BBeacon.class);
//		PwSat2Decoder decoder = new PwSat2Decoder(predict, config);
//		Nayif1Decoder decoder = new Nayif1Decoder(predict, config);
//		Aistechsat2Decoder decoder =new Aistechsat2Decoder(predict, config);
//		TechnosatDecoder decoder = new TechnosatDecoder(predict, config);
//		Smog1Decoder decoder = new Smog1Decoder(predict, config);
//		Ao73Decoder decoder = new Ao73Decoder(predict, config);
//		FoxSlowDecoder decoder= new FoxSlowDecoder<>(predict, config, Fox1CBeacon.class);
//		UspDecoder decoder = new UspDecoder(predict, config, UspBeacon.class);
//		DelfiPqDecoder decoder = new DelfiPqDecoder(predict, config);
		// ObservationResult result = decoder.decode(wav,
		// TestUtil.loadObservation("data/suomi.raw.gz.json").getReq());
		DecoderResult result = decoder.decode(wav, observation.getReq(), new Transmitter());

		logHdlcTrace();

		assertEquals(2, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getRawPath());

//		List<byte[]> trace = TraceContext.instance.getHdlcReceiverTrace().getDemodulatorOutput();
//		for (int i = 0; i < trace.size(); i++) {
//			System.out.println(Arrays.toString(trace.get(i)));
//		}
	}

	private static void logHdlcTrace() {
		HdlcReceiverTrace trace = TraceContext.instance.getHdlcReceiverTrace();
		if (trace == null) {
			return;
		}
		int totalFixed = 0;
		Map<String, Integer> countByHeader = new HashMap<>();
		Map<String, Integer> countByPresync = new HashMap<>();
		for (HdlcFrameStats cur : trace.getBeaconStats()) {
			if (cur.isAssistedHeaderWorked()) {
				totalFixed++;
			}
			byte[] header = new byte[Header.LENGTH_BYTES * 8];
			if (cur.getUnpackedBits().length >= header.length) {
				System.arraycopy(cur.getUnpackedBits(), 0, header, 0, header.length);
				String headerStr = Arrays.toString(header);
				Integer previous = countByHeader.get(headerStr);
				if (previous == null) {
					previous = 0;
				}
				previous++;
				countByHeader.put(headerStr, previous);
			}
			System.out.println(cur.getBeforeFlagsCount() + "  " + cur.getFrame().length + "  " + cur.getAfterFlagsCount());

			StringBuilder str = new StringBuilder();
			for (int i = 0; i < cur.getPresyncStats().size(); i++) {
				if (i != 0) {
					str.append("\n");
				}
				str.append("\t" + cur.getPresyncStats().get(i).getShiftRegister() + "\t" + cur.getPresyncStats().get(i).getRawModulatorInput());
			}
			String presyncStr = str.toString();
			Integer previous = countByPresync.get(presyncStr);
			if (previous == null) {
				previous = 0;
			}
			previous++;
			countByPresync.put(presyncStr, previous);
		}
		System.out.println("assisted header worked: " + totalFixed);
		System.out.println("unique headers:");
		for (Entry<String, Integer> cur : countByHeader.entrySet()) {
			System.out.println(cur.getValue() + " " + cur.getKey());
		}
		System.out.println("unique presync:");
		for (Entry<String, Integer> cur : countByPresync.entrySet()) {
			System.out.println(cur.getValue() + "\n" + cur.getKey());
		}
	}
//	
//	public static void main(String[] args) throws Exception {
//		float gainMu = 0.175f * 3;
//		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_2417310_2020-06-21T23-56-15.wav")));
//		FskDemodulator demod = new FskDemodulator(source, 9600, 5000.0f, 0.175f * 3, 1, (double)9600 / 8);
////		FloatInput next = new LowPassFilter(source, 1, 1.0, (double) 9600 * 0.625, 9600 / 8, Window.WIN_HAMMING, 6.76);
////		float samplesPerSymbol = next.getContext().getSampleRate() / 9600;
////		next = new DcBlocker(next, (int) (Math.ceil(samplesPerSymbol * 32)), true);
////		next = new ClockRecoveryMM(next, next.getContext().getSampleRate() / 9600, (float) (0.25 * gainMu * gainMu), 0.5f, gainMu, 0.005f);
////		next = new Rail(next, -1.0f, 1.0f);
////		ByteInput bie = new FloatToChar(next, 127.0f);
////		Ax25G3ruhBeaconSource input = new Ax25G3ruhBeaconSource<>(bie, Ax25Beacon.class);
//		Ax25G3ruhBeaconSource input = new Ax25G3ruhBeaconSource<>(demod, Ax25Beacon.class);
//		int total = 0;
//		while( input.hasNext() ) {
//			System.out.println(input.next());
//			total++;
//		}
//		System.out.println(total);
//	}	
//
//	@Test
//	// @Ignore
//	public void testSomeData3() throws Exception {
//		// File wav = TestUtil.setupClasspathResource(tempFolder, "data/suomi.raw.gz");
//		// File wav = new File("/Users/dernasherbrezon/Downloads/1585841981897/output.raw.gz-236-239.raw.gz");
//		// File meta = new File("/Users/dernasherbrezon/Downloads/1585841981897/output.raw.gz-236-239.raw.gz.json");
//		int[] timings = new int[] { 239, 269, 284, 314, 329, 344, 359, 374, 419, 434, 449, 464, 479, 494, 509, 524, 539 };
////		int[] timings = new int[] { 269 };
////		int[] timings = new int[] { 284 };
//		for (int cur : timings) {
//			File wav = new File("/Users/dernasherbrezon/Downloads/1592750646120/output.raw.gz-" + cur + "-" + (cur + 1) + ".raw.gz");
//			File meta = new File("/Users/dernasherbrezon/Downloads/1592750646120/output.raw.gz-" + cur + "-" + (cur + 1) + ".raw.gz.json");
//			Observation observation = load(meta);
//			PredictOreKit predict = new PredictOreKit(config);
//			FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, 9600, Ax25Beacon.class);
//			// ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("data/suomi.raw.gz.json").getReq());
//			ObservationRequest req = observation.getReq();
//			req.setBandwidth(20000);
//			DecoderResult result = decoder.decode(wav, req);
//			System.out.println(cur + " -> " + result.getNumberOfDecodedPackets());
//		}
//		// assertEquals(2, result.getNumberOfDecodedPackets().longValue());
//		// assertNotNull(result.getDataPath());
//		// assertNotNull(result.getRawPath());
//	}

	public static Observation load(File file) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			JsonObject meta = Json.parse(r).asObject();
			return Observation.fromJson(meta);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();
	}

}
