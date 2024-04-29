package samuschair.orbital2.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public final class IOUtil {

	private IOUtil() {}

	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}

	/**
	 * Reads the specified resource, decompresses it and returns it as a ByteBuffer.
	 *
	 * @param resource   the resource to read
	 * @param bufferSize the initial buffer size
	 *
	 * @return the resource data
	 *
	 * @throws IOException if an IO error occurs
	 */
	public static ByteBuffer gZippedIOResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;
		if(!resource.endsWith(".gz")) {
			resource += ".gz";
		}

		Path path = resource.startsWith("http") ? null : Paths.get(resource);
		if (path != null && Files.isReadable(path)) {
			try (SeekableByteChannel fc = Files.newByteChannel(path)) {
				buffer = BufferUtils.createByteBuffer((int)fc.size() + 1);
				while (fc.read(buffer) != -1);
			}
		} else {
			try (
					InputStream source = new GZIPInputStream(resource.startsWith("http")
							? new URL(resource).openStream()
							: IOUtil.class.getClassLoader().getResourceAsStream(resource));
					ReadableByteChannel rbc = Channels.newChannel(source)
			) {
				buffer = BufferUtils.createByteBuffer(bufferSize);

				while (true) {
					int bytes = rbc.read(buffer);
					if (bytes == -1) {
						break;
					}
					if (buffer.remaining() == 0) {
						buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2); // 50%
					}
				}
			}
		}

		buffer.flip();
		return MemoryUtil.memSlice(buffer);
	}

	public static String readResource(String resource) {
		try {
			return new String(IOUtil.class.getClassLoader().getResourceAsStream(resource).readAllBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			return null;
		}
	}
}
