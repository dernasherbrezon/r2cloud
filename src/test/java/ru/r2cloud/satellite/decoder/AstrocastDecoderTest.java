package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.TestUtil;
import ru.r2cloud.jradio.Ax25BeaconSource;
import ru.r2cloud.jradio.ax25.Ax25Beacon;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;

public class AstrocastDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;

	@Test
	public void testSomeData() throws Exception {
		File wav = TestUtil.setupClasspathResource(tempFolder, "data/astrocast.raw.gz");
		PredictOreKit predict = new PredictOreKit(config);
		AstrocastDecoder decoder = new AstrocastDecoder(predict, config);
		DecoderResult result = decoder.decode(wav, TestUtil.loadObservation("data/astrocast.raw.gz.json").getReq());
		assertEquals(1, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getRawPath());
	}

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
		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_3169528_2020-11-21T14-22-53.wav")));
//		WavFileSource source = new WavFileSource(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/satnogs_edited.wav")));

//		RmsAgc agc = new RmsAgc(source, 2e-2f / 2, 1.0f);
//		MultiplyConst mm = new MultiplyConst(agc, 1.0f);
//		FskDemodulator demod = new FskDemodulator(source, 9600);//, 5000.0f, 1, 1000, false);

//		float gainMu = 0.175f * 3;
//		FloatToComplex f2c = new FloatToComplex(source);
//		GmskDemodulator demod = new GmskDemodulator(f2c, 4800, 20000, gainMu, 0.02f, 1, 2000);

//		FloatToComplex f2c = new FloatToComplex(source);
//		BpskDemodulator demod = new BpskDemodulator(f2c, 1200, 5, 1500, false);
		AfskDemodulator demod = new AfskDemodulator(source, 1200, 500, 1700, 5);
//		FloatToComplex fc = new FloatToComplex(source);
//		BpskDemodulator demod = new BpskDemodulator(fc, 2400, 4, 1500.0, false);
//		GmskDemodulator gmsk = new GmskDemodulator(source, baud, 20000, gainMu);
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
//		Ax100BeaconSource input = new Ax100BeaconSource<>(demod, 255, CspBeacon.class); //, false, false, false
		
//		Spooqy1 input = new Spooqy1(demod);		
//		AfskDemodulator demodulator = new AfskDemodulator(source, 1200, 500, 1700, 5);
//		Ax25BeaconSource input = new Ax25BeaconSource<>(demodulator, Ax25Beacon.class);
//		GmskDemodulator demodulator = new GmskDemodulator(f2c, 9600, 12000, 0.175f * 3, 0.02f, 1, 2000);
//		SoftToHard bs = new SoftToHard(demodulator);
//		FskDemodulator demod = new FskDemodulator(source, 9600);
//		OpsSat input = new OpsSat(new SoftToHard(demod));
		Ax25BeaconSource<Ax25Beacon> input = new Ax25BeaconSource<Ax25Beacon>(demod, Ax25Beacon.class);
//		Ax25G3ruhBeaconSource input = new Ax25G3ruhBeaconSource<>(demod, Ax25Beacon.class);
//		CorrelateAccessCodeTag correlateTag = new CorrelateAccessCodeTag(new SoftToHard(demod), 4, "00110101001011100011010100101110", false);
//		TaggedStreamToPdu pdu = new TaggedStreamToPdu(new UnpackedToPacked(new FixedLengthTagger(correlateTag, 512 * 8), 1, Endianness.GR_MSB_FIRST));
//		Cc11xxReceiver cc11 = new Cc11xxReceiver(pdu, false, true);
//		while(true) {
//			cc11.readBytes();
//			System.out.println("message");
//		}
//		Strand1 input = new Strand1(demod);
//		Alsat1n input = new Alsat1n(demod);
		int total = 0;
		while( input.hasNext() ) {
//			System.out.println(i);
			Object next = input.next();
			System.out.println(next);
//			Spooqy1Beacon bb = (Spooqy1Beacon)next;
//			System.out.println(bytesToHex(bb.getRawData()));
			total++;
//			System.out.println(next);
//		}
		}
		System.out.println("total: " + total);
		
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
//	 @Ignore
	public void testSomeData2() throws Exception {
		// File wav = TestUtil.setupClasspathResource(tempFolder, "data/suomi.raw.gz");
		// File wav = new File("/Users/dernasherbrezon/Downloads/1585841981897/output.raw.gz-236-239.raw.gz");
		// File meta = new File("/Users/dernasherbrezon/Downloads/1585841981897/output.raw.gz-236-239.raw.gz.json");
//		 File wav = new File("/Users/dernasherbrezon/Downloads/1606042724776/output.raw.gz-205-207.raw.gz");
//		 File meta = new File("/Users/dernasherbrezon/Downloads/1606042724776/output.raw.gz-205-207.raw.gz.json");
		File wav = new File("/Users/dernasherbrezon/Downloads/1606036981302/output.raw.gz");
		File meta = new File("/Users/dernasherbrezon/Downloads/1606036981302/meta.json");
		Observation observation = load(meta);
		PredictOreKit predict = new PredictOreKit(config);
		BpskAx25G3ruhDecoder decoder = new BpskAx25G3ruhDecoder(predict, config, 2400, Ax25Beacon.class);
//		OpsSatDecoder decoder = new OpsSatDecoder(predict, config);
//		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, 9600, Ax25Beacon.class);
//		SnetDecoder decoder = new SnetDecoder(predict, config);
//		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, 9600, Unisat6Beacon.class);
//		AfskAx25Decoder decoder = new AfskAx25Decoder(predict, config, 1200, Amical1Beacon.class);
//		FskAx25G3ruhDecoder decoder = new FskAx25G3ruhDecoder(predict, config, 9600, Uwe4Beacon.class);
//		OpsSatDecoder decoder = new OpsSatDecoder(predict, config);
//		SalsatDecoder decoder = new SalsatDecoder(predict, config);
//		FskAx100Decoder decoder = new FskAx100Decoder(predict, config, 9600, 255, Aistechsat2Beacon.class);
//		ChompttDecoder decoder = new ChompttDecoder(predict, config);
//		ReaktorHelloWorldDecoder decoder = new ReaktorHelloWorldDecoder(predict, config);
//		Gomx1Decoder decoder = new Gomx1Decoder(predict, config);
//		Aausat4Decoder decoder = new Aausat4Decoder(predict, config);
//		FoxSlowDecoder<Fox1BBeacon> decoder = new FoxSlowDecoder<>(predict, config, Fox1BBeacon.class);
//		PwSat2Decoder decoder = new PwSat2Decoder(predict, config);
//		Aistechsat2Decoder decoder =new Aistechsat2Decoder(predict, config);
		// ObservationResult result = decoder.decode(wav, TestUtil.loadObservation("data/suomi.raw.gz.json").getReq());
		DecoderResult result = decoder.decode(wav, observation.getReq());
		assertEquals(2, result.getNumberOfDecodedPackets().longValue());
		assertNotNull(result.getDataPath());
		assertNotNull(result.getRawPath());
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

//	public static void main(String[] args) throws Exception {
//		try (BeaconInputStream<Fox1CBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream("/Users/dernasherbrezon/Downloads/a-10.bin")), Fox1CBeacon.class)) {
//			while (bis.hasNext()) {
//				Fox1CBeacon beacon = bis.next();
//				System.out.println("here");
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

}
