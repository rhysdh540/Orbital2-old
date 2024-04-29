package samuschair.orbital2.window;

import lombok.Getter;
import org.joml.Vector2i;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkWindow;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import samuschair.orbital2.Orbital2Main;
import samuschair.orbital2.util.CursedUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.UUID;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;

/**
 * A Nuklear window
 * <p>
 * Note that this is not a system window (that can be done with GLFW or similar),
 * but instead a window within the Nuklear context, which will be rendered inside
 * a system window.
 */
public abstract class Window {

	@Getter
	protected String title;

	protected NkWindow window;

	public final UUID id = UUID.randomUUID();

	@Getter
	protected int width, height;
	protected int flags;

	protected int x, y;
	protected int oldX, oldY;
	protected int oldWidth, oldHeight;

	public static final int DEFAULT_FLAGS = NK_WINDOW_BORDER | NK_WINDOW_MOVABLE | NK_WINDOW_SCALABLE | NK_WINDOW_MINIMIZABLE | NK_WINDOW_TITLE;
	public static final int FLAGS_NO_RESIZE = NK_WINDOW_BORDER | NK_WINDOW_MOVABLE | NK_WINDOW_MINIMIZABLE | NK_WINDOW_TITLE;

	public Window(String title) {
		this(title, 230, 250);
	}

	public Window(String title, int width, int height) {
		this(title, width, height, DEFAULT_FLAGS);
	}

	public Window(String title, int width, int height, int flags) {
		this.title = title;
		this.width = width;
		this.height = height;
		this.flags = flags;
	}

	public void layout(NkContext ctx, Vector2i position) {
		layout(ctx, position.x, position.y);
	}

	/**
	 * Layout the window at the specified position
	 * @param ctx the Nuklear context
	 * @param x the x position of the window
	 * @param y the y position of the window
	 */
	public void layout(NkContext ctx, int x, int y) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			preRender(ctx, stack);
			NkRect bounds = NkRect.malloc(stack);
			if (nk_begin_titled(ctx, id.toString(), title, nk_rect(x, y, width, height, bounds), flags)) {
				window = nk_window_find(ctx, id.toString());
				updatePosition(ctx, stack);
				render(ctx, stack);
			}
			nk_end(ctx);
			postRender(ctx, stack);
		}
	}

	protected void updatePosition(NkContext ctx, MemoryStack stack) {
		oldX = x;
		oldY = y;
		oldWidth = width;
		oldHeight = height;
		NkRect bounds = nk_window_get_bounds(ctx, NkRect.malloc(stack));
		this.x = (int) bounds.x();
		this.y = (int) bounds.y();
		this.width = (int) nk_window_get_width(ctx);
		this.height = (int) nk_window_get_height(ctx);
	}

	public void takeScreenshot(Path path) {
		long win = Orbital2Main.getWindowId();
		glfwMakeContextCurrent(win);
		IntBuffer widthBuffer = MemoryUtil.memAllocInt(1);
		IntBuffer heightBuffer = MemoryUtil.memAllocInt(1);
		glfwGetFramebufferSize(win, widthBuffer, heightBuffer);
		int width = widthBuffer.get(0), height = heightBuffer.get(0);
		ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4); // 4 bytes per pixel
		glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
		//crop the image to the window size
		BufferedImage image = new BufferedImage(widthBuffer.get(0), heightBuffer.get(0), BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int index = (i + j * width) * 4;
				int r = pixels.get(index) & 0xFF;
				int g = pixels.get(index + 1) & 0xFF;
				int b = pixels.get(index + 2) & 0xFF;
				int a = pixels.get(index + 3) & 0xFF;
				image.setRGB(i, height - j - 1, (a << 24) | (r << 16) | (g << 8) | b);
			}
		}
		image = image.getSubimage(x, y, this.width * 2, this.height * 2); // todo: why must it be x2?
		try {
			ImageIO.write(image, "png", path.toFile());
		} catch (Exception e) {
			throw CursedUtil.asUnchecked(e);
		}
	}

	/**
	 * Draw the window contents
	 * @param ctx the Nuklear context
	 * @param stack the memory stack
	 */
	protected abstract void render(NkContext ctx, MemoryStack stack);

	protected void preRender(NkContext ctx, MemoryStack stack) {
		// Do nothing by default
	}

	protected void postRender(NkContext ctx, MemoryStack stack) {
		// Do nothing by default
	}
}
