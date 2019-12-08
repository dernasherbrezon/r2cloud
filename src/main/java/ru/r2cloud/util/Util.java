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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

public final class Util {

	private static final Logger LOG = LoggerFactory.getLogger(Util.class);
	private static final Pattern COMMA = Pattern.compile(",");

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
				LOG.info("unable to create parent dir: " + path.toAbsolutePath(), e);
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
				LOG.error("unable to delete: " + f.toAbsolutePath(), e);
				return false;
			}
		}
		try {
			Files.delete(f);
			return true;
		} catch (IOException e) {
			LOG.error("unable to delete: " + f.toAbsolutePath(), e);
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
	public static Long readTotalSamples(Path rawFile) {
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
			return ((b1 << 24) | (b2 << 16) + (b3 << 8) + b4) / 2;
		} catch (IOException e1) {
			LOG.error("unable to get total number of samples", e1);
			return null;
		}
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
				LOG.error("unable to get value: " + name, e);
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

	private Util() {
		// do nothing
	}

}
