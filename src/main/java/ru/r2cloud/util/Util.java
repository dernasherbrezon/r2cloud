package ru.r2cloud.util;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertPathBuilderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.model.SampleRateKey;
import ru.r2cloud.model.SampleRateMapping;

public final class Util {

	private static final Logger LOG = LoggerFactory.getLogger(Util.class);
	private static final Pattern COMMA = Pattern.compile(",");
	private static final Map<SampleRateKey, SampleRateMapping> MAPPING = new HashMap<>();

	// Samples rates for different devices and most commonly used baud rates
	// They take into consideration ~10khz margin for doppler shift
	// Some sample rates cannot be integer decimated to the required baud
	// In that case fractional part is moved closer to symbol synchronization
	// @formatter:off
	static {
		// rtl-sdr
		index(240_000,  48_000,     400,    200);
		index(240_000,  48_000,   2_000,    500);
		index(240_000,  48_000,   6_000,  1_200);
		index(240_000,  48_000,  12_000,  2_400);
		index(240_000,  48_000,  24_000,  4_800);
		index(240_000,  48_000,  48_000,  9_600);
		index(288_000,  57_600,  57_600, 19_200);
		index(230_400, 115_200, 115_200, 38_400);
		index(288_000, 144_000, 144_000, 72_000);
		index(240_000,  10_000,   5_000,  1_250);
		index(240_000,  15_000,   7_500,  2_500);
		index(240_000,  40_000,  20_000,  5_000);
		index(300_000,  37_500,  37_500, 12_500);

		// sdr-server
		// base rate 1200000 / 2400000
		index( 48_000,  48_000,     400,    200);
		index( 48_000,  48_000,   2_000,    500);
		index( 48_000,  48_000,   6_000,  1_200);
		index( 48_000,  48_000,  12_000,  2_400);
		index( 48_000,  48_000,  24_000,  4_800);
		index( 48_000,  48_000,  48_000,  9_600);
		index( 60_000,  60_000,  60_000, 19_200); // fractional
		index(120_000, 120_000, 120_000, 38_400); // fractional, also for 1440000
		index(150_000, 150_000, 150_000, 72_000); // fractional
		index( 10_000,  10_000,   5_000,  1_250);
		index( 15_000,  15_000,   7_500,  2_500);
		index( 40_000,  40_000,  20_000,  5_000);
		index( 37_500,  37_500,  37_500, 12_500);
		// base rate 1440000
		index( 40_000,  40_000,  40_000, 12_500); // fractional
		// base rate 960000
		index( 38_400,  38_400,  38_400, 19_200);
		index(192_000, 192_000, 192_000, 38_400);
		index(160_000, 160_000, 160_000, 72_000); // fractional
		index( 38_400,  38_400,  38_400, 12_500); // fractional
		
		// spy-server AirSpy
		index( 46_875,  46_875,   1_875,    200); // fractional
		index( 46_875,  46_875,   1_875,    500); // fractional
		index( 46_875,  46_875,   9_375,  1_200); // fractional
		index( 46_875,  46_875,   9_375,  2_400); // fractional
		index( 46_875,  46_875,  46_875,  4_800); // fractional
		index( 46_875,  46_875,  46_875,  9_600); // fractional
		index( 93_750,  46_875,  46_875, 19_200); // fractional
		index(187_500, 187_500, 187_500, 38_400); // fractional
		index(187_500, 187_500, 187_500, 72_000); // fractional
		index( 46_875,  46_875,   9_375,  1_250); // fractional
		index( 46_875,  46_875,   9_375,  2_500); // fractional
		index( 46_875,  46_875,  46_875,  5_000); // fractional
		index( 46_875,  46_875,  46_875, 12_500); // fractional
	}
	// @formatter:on

	public static void rotateImage(File result) {
		try {
			BufferedImage image;
			try (FileInputStream fis = new FileInputStream(result)) {
				image = ImageIO.read(fis);
			}
			AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
			tx.translate(-image.getWidth(null), -image.getHeight(null));
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			image = op.filter(image, null);
			try (FileOutputStream fos = new FileOutputStream(result)) {
				ImageIO.write(image, "jpg", fos);
			}
		} catch (Exception e) {
			LOG.error("unable to rotate image", e);
		}
	}

	public static File initDirectory(String path) {
		File result = new File(path);
		if (result.exists() && !result.isDirectory()) {
			throw new IllegalArgumentException("base path exists and not directory: " + result.getAbsolutePath());
		}
		if (!result.exists() && !result.mkdirs()) {
			throw new IllegalArgumentException("unable to create basepath: " + result.getAbsolutePath());
		}
		return result;
	}

	public static boolean initDirectory(Path path) {
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
				return true;
			} catch (IOException e) {
				LOG.info("unable to create parent dir: {}", path.toAbsolutePath(), e);
				return false;
			}
		}
		return true;
	}

	public static List<Path> toList(DirectoryStream<Path> stream) {
		List<Path> result = new ArrayList<>();
		for (Path cur : stream) {
			result.add(cur);
		}
		return result;
	}

	public static void toLog(Logger log, InputStream is) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
			String curLine = null;
			while ((curLine = in.readLine()) != null) {
				log.info(curLine);
			}
		}
	}

	public static void shutdown(ScheduledExecutorService executor, long timeoutMillis) {
		if (executor == null) {
			return;
		}
		executor.shutdownNow();
		boolean cleanlyTerminated;
		try {
			cleanlyTerminated = executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			cleanlyTerminated = executor.isTerminated();
		}
		if (!cleanlyTerminated) {
			String threadpoolName;
			if (executor instanceof ScheduledThreadPoolExecutor) {
				ThreadFactory factory = ((ScheduledThreadPoolExecutor) executor).getThreadFactory();
				if (factory instanceof NamingThreadFactory) {
					NamingThreadFactory namingFactory = (NamingThreadFactory) factory;
					threadpoolName = namingFactory.getPrefix();
				} else {
					threadpoolName = "unknown[" + factory.getClass().getSimpleName() + "]";
				}
			} else {
				threadpoolName = "unknown[" + executor.getClass().getSimpleName() + "]";
			}
			LOG.error("executor did not terminate in the specified time: {}", threadpoolName);
		}
	}

	public static void deleteQuietly(File file) {
		deleteQuietly(file.toPath());
	}

	public static void deleteQuietly(Path file) {
		if (!Files.exists(file)) {
			return;
		}
		try {
			Files.delete(file);
		} catch (IOException e) {
			LOG.error("unable to delete temp file: {}", file.toAbsolutePath(), e);
		}
	}

	public static void shutdown(String name, ProcessWrapper process, long timeoutMillis) {
		if (process == null || !process.isAlive()) {
			return;
		}
		try {
			LOG.info("stopping: {}", name);
			process.destroy();
			if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
				LOG.info("unable to cleanly shutdown. kill process: {}", name);
				int statusCode = process.destroyForcibly().waitFor();
				if (statusCode != 0 && statusCode != 137) {
					LOG.info("invalid status code while stopping: {}", statusCode);
				}
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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

	public static boolean deleteDirectory(Path f) {
		if (Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(f)) {
				for (Path entry : entries) {
					boolean curResult = deleteDirectory(entry);
					if (!curResult) {
						return curResult;
					}
				}
			} catch (IOException e) {
				LOG.error("unable to delete: {}", f.toAbsolutePath(), e);
				return false;
			}
		}
		try {
			Files.delete(f);
			return true;
		} catch (IOException e) {
			LOG.error("unable to delete: {}", f.toAbsolutePath(), e);
			return false;
		}
	}

	public static List<String> splitComma(String str) {
		String[] values = COMMA.split(str);
		List<String> result = new ArrayList<>();
		for (String cur : values) {
			cur = cur.trim();
			if (cur.length() == 0) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	public static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[1024 * 4];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

	public static void closeQuietly(Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (IOException e) {
			LOG.info("unable to close", e);
		}
	}

	// works well for files less than 4Gb
	public static Long readTotalBytes(Path rawFile) {
		if (rawFile.getFileName().toString().endsWith(".gz")) {
			try (SeekableByteChannel bch = Files.newByteChannel(rawFile, StandardOpenOption.READ)) {
				if (bch.size() < 4) {
					return null;
				}
				bch.position(bch.size() - 4);
				ByteBuffer dst = ByteBuffer.allocate(4);
				readFully(bch, dst);
				long b4 = dst.get(0) & 0xFF;
				long b3 = dst.get(1) & 0xFF;
				long b2 = dst.get(2) & 0xFF;
				long b1 = dst.get(3) & 0xFF;
				return ((b1 << 24) | (b2 << 16) + (b3 << 8) + b4);
			} catch (IOException e1) {
				LOG.error("unable to get total number of samples", e1);
				return null;
			}
		} else {
			try {
				return Files.size(rawFile);
			} catch (IOException e) {
				LOG.error("unable to get total number of samples", e);
				return null;
			}
		}
	}

	// do not log whole stacktrace for network-based exceptions
	// they are expected because base station can work without internet
	public static void logIOException(Logger log, String message, Throwable e) {
		logIOException(log, true, message, e);
	}

	public static void logIOException(Logger log, boolean error, String message, Throwable e) {
		String cause = getShortMessageToLog(e);
		if (cause != null) {
			if (error) {
				log.error("{}: {}", message, cause);
			} else {
				log.info("{}: {}", message, cause);
			}
		} else {
			if (error) {
				log.error(message, e);
			} else {
				log.info(message, e);
			}
		}
	}

	private static String getShortMessageToLog(Throwable e) {
		if (e.getCause() != null) {
			return getShortMessageToLog(e.getCause());
		}
		if (e instanceof IOException) {
			if (e.getMessage() != null) {
				return e.getMessage();
			}
			return e.toString();
		}
		if (e instanceof UnresolvedAddressException) {
			return e.toString();
		}
		if (e instanceof CertPathBuilderException) {
			return e.getMessage();
		}
		return null;
	}

	public static void readFully(ReadableByteChannel channel, ByteBuffer b) throws IOException {
		final int expectedLength = b.remaining();
		int read = 0;
		while (read < expectedLength) {
			int readNow = channel.read(b);
			if (readNow <= 0) {
				break;
			}
			read += readNow;
		}
		if (read < expectedLength) {
			throw new EOFException();
		}
	}

	@SuppressWarnings("unchecked")
	public static JsonValue convertObject(Object obj) {
		JsonValue primitiveValue = convertPrimitive(obj);
		if (primitiveValue != null) {
			return primitiveValue;
		}
		if (obj.getClass().isArray()) {
			if (obj.getClass().getComponentType() == byte.class) {
				return Json.value((String) bytesToHex((byte[]) obj));
			} else {
				JsonArray result = new JsonArray();
				for (int i = 0; i < Array.getLength(obj); i++) {
					JsonValue convertObject = convertObject(Array.get(obj, i));
					if (convertObject == null) {
						continue;
					}
					result.add(convertObject);
				}
				if (result.isEmpty()) {
					return null;
				}
				return result;
			}
		} else if (obj instanceof Collection<?>) {
			JsonArray result = new JsonArray();
			for (Object curCollectionItem : (Collection<?>) obj) {
				JsonValue convertObject = convertObject(curCollectionItem);
				if (convertObject == null) {
					continue;
				}
				result.add(convertObject);
			}
			if (result.isEmpty()) {
				return null;
			}
			return result;
		} else if (obj instanceof Map<?, ?>) {
			JsonObject result = new JsonObject();
			for (Entry<Object, Object> curEntry : ((Map<Object, Object>) obj).entrySet()) {
				JsonValue convertObject = convertObject(curEntry.getValue());
				if (convertObject == null) {
					continue;
				}
				result.add(curEntry.getKey().toString(), convertObject);
			}
			if (result.isEmpty()) {
				return null;
			}
			return result;
		} else if (obj.getClass().isEnum()) {
			return Json.value(((Enum<?>) obj).name());
		}
		JsonObject result = new JsonObject();
		Method[] m = obj.getClass().getMethods();
		Arrays.sort(m, MethodComparator.INSTANCE);
		for (Method cur : m) {
			if (cur.getParameterCount() > 0) {
				continue;
			}
			String name = extractName(cur.getName());
			if (name == null) {
				continue;
			}
			try {
				Object value = cur.invoke(obj, (Object[]) null);
				if (value == null || value instanceof Class<?>) {
					continue;
				}

				JsonValue jsonValue = convertObject(value);
				if (jsonValue == null) {
					continue;
				}
				result.add(name, jsonValue);
			} catch (Exception e) {
				LOG.error("unable to get value: {}", name, e);
			}
		}
		if (result.isEmpty()) {
			return null;
		}
		return result;
	}

	private static JsonValue convertPrimitive(Object value) {
		JsonValue jsonValue;
		if (value instanceof Integer) {
			jsonValue = Json.value((Integer) value);
		} else if (value instanceof Long) {
			jsonValue = Json.value((Long) value);
		} else if (value instanceof Float) {
			jsonValue = Json.value((Float) value);
		} else if (value instanceof Double) {
			jsonValue = Json.value((Double) value);
		} else if (value instanceof Boolean) {
			jsonValue = Json.value((Boolean) value);
		} else if (value instanceof Byte) {
			jsonValue = Json.value((Byte) value);
		} else if (value instanceof Short) {
			jsonValue = Json.value((Short) value);
		} else if (value instanceof String) {
			jsonValue = Json.value((String) value);
		} else {
			jsonValue = null;
		}
		return jsonValue;
	}

	private static String extractName(String methodName) {
		if (methodName.startsWith("get")) {
			return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
		}
		if (methodName.startsWith("is")) {
			return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
		}
		return null;
	}

	public static byte[] hexStringToByteArray(String s) {
		if (s == null) {
			return null;
		}
		int len = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == ' ') {
				continue;
			}
			len++;
		}
		byte[] data = new byte[len / 2];
		int index = 0;
		for (int i = 0; i < s.length();) {
			if (s.charAt(i) == ' ') {
				i++;
				continue;
			}
			data[index] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
			i += 2;
			index++;
		}
		return data;
	}

	private static void index(long deviceOutput, long demodulatorInput, long symbolSyncInput, int baudRate) {
		SampleRateMapping old = MAPPING.put(new SampleRateKey(deviceOutput, baudRate), new SampleRateMapping(deviceOutput, demodulatorInput, symbolSyncInput, baudRate));
		if (old != null) {
			LOG.error("duplicate sample rate mapping for: {}-{} ", deviceOutput, baudRate);
		}
	}

	public static long getDemodulatorInput(int baudRate, long deviceOutput) {
		SampleRateMapping mapping = MAPPING.get(new SampleRateKey(deviceOutput, baudRate));
		if (mapping != null) {
			return mapping.getDemodulatorInput();
		}
		long resultSampleRate = findClosest(deviceOutput, baudRate * 3);
		if (resultSampleRate % baudRate != 0) {
			LOG.warn("using non-integer decimation factor for unsupported baud rate: {} and bandwidth: {}", baudRate, deviceOutput);
		}
		return resultSampleRate;
	}

	public static long getSymbolSyncInput(int baudRate, long demodulatorInput) {
		for (SampleRateMapping cur : MAPPING.values()) {
			if (cur.getBaudRate() != baudRate) {
				continue;
			}
			if (cur.getDemodulatorInput() != demodulatorInput) {
				continue;
			}
			return cur.getSymbolSyncInput();
		}
		long resultSampleRate = findClosest(demodulatorInput, baudRate * 3);
		if (resultSampleRate % baudRate != 0) {
			LOG.warn("using non-integer decimation factor for unsupported baud rate: {} and bandwidth: {}", baudRate, demodulatorInput);
		}
		return resultSampleRate;
	}

	public static Long getSmallestGoodDeviceSampleRate(int baudRate, List<Long> supportedSamplesRates) {
		// choose the lowest possible
		SampleRateMapping result = null;
		// assume supported sample rates is not big
		for (Long deviceOutput : supportedSamplesRates) {
			SampleRateMapping cur = MAPPING.get(new SampleRateKey(deviceOutput, baudRate));
			if (cur == null) {
				continue;
			}
			if (result == null) {
				result = cur;
				continue;
			}
			if (cur.getDeviceOutput() < result.getDeviceOutput()) {
				result = cur;
			}
		}
		if (result != null) {
			return result.getDeviceOutput();
		}
		Collections.sort(supportedSamplesRates);
		int expectedSampleRate = baudRate * 3;
		if (expectedSampleRate < 48_000) {
			expectedSampleRate = 48_000;
		}
		for (long current : supportedSamplesRates) {
			if (current > expectedSampleRate) {
				return current;
			}
		}
		return null;
	}

	public static long getSmallestDividableSampleRate(int baudRate, long sampleRate) {
		SampleRateMapping result = null;
		for (Entry<SampleRateKey, SampleRateMapping> cur : MAPPING.entrySet()) {
			if (cur.getKey().getBaudRate() != baudRate) {
				continue;
			}
			if (sampleRate % cur.getKey().getDeviceOutput() != 0) {
				continue;
			}
			if (result == null) {
				result = cur.getValue();
				continue;
			}
			if (cur.getValue().getDeviceOutput() < result.getDeviceOutput()) {
				result = cur.getValue();
			}
		}
		if (result != null) {
			return result.getDeviceOutput();
		}
		// sample rate guaranteed to be integer dividable from the sdr server bandwidth
		long resultSampleRate = findClosest(sampleRate, baudRate * 3);
		if (resultSampleRate % baudRate != 0) {
			LOG.warn("using non-integer decimation factor for unsupported baud rate: {} and bandwidth: {}", baudRate, sampleRate);
		}
		return resultSampleRate;
	}

	private static long findClosest(long fromSampleRate, long toSampleRate) {
		int[] primeNumbers = new int[] { 11, 7, 5, 3, 2 };
		long result = fromSampleRate;
		long current = fromSampleRate;
		while (current >= toSampleRate) {
			result = current;
			for (int i = 0; i < primeNumbers.length; i++) {
				long next = current / primeNumbers[i];
				long remainder = current % primeNumbers[i];
				if (remainder == 0 && next > toSampleRate) {
					result = current;
					current = next;
				}
			}
			// no dividers found
			if (result == current) {
				break;
			}
		}
		return result;
	}

	private Util() {
		// do nothing
	}

}
