package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nuklear.Nuklear.*;

/**
 * A Nuklear window
 * <p>
 * Note that this is not a system window (that can be done with GLFW or similar),
 * but instead a window within the Nuklear context, which will be rendered inside
 * a system window.
 */
public abstract class Window {

	protected final String title;
	protected final int width, height;
	protected final int flags;

	public static final int DEFAULT_FLAGS = NK_WINDOW_BORDER | NK_WINDOW_MOVABLE | NK_WINDOW_SCALABLE | NK_WINDOW_MINIMIZABLE | NK_WINDOW_TITLE;

	public Window(String title) {
		this(title, 230, 250);
	}

	protected static final NkPluginFilter filter = NkPluginFilter.create(Nuklear::nnk_filter_default);

	public Window(String title, int width, int height) {
		this(title, width, height, DEFAULT_FLAGS);
	}

	public Window(String title, int width, int height, int flags) {
		this.title = title;
		this.width = width;
		this.height = height;
		this.flags = flags;
	}

	/**
	 * Layout the window at the specified position
	 * @param ctx the Nuklear context
	 * @param x the x position of the window
	 * @param y the y position of the window
	 */
	public void layout(NkContext ctx, int x, int y) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			if (nk_begin(ctx, title, nk_rect(x, y, width, height, NkRect.malloc(stack)), flags)) {
				render(ctx, stack);
			}
			nk_end(ctx);
		}
	}

	/**
	 * Draw the window contents
	 * @param ctx the Nuklear context
	 * @param stack the memory stack
	 */
	protected abstract void render(NkContext ctx, MemoryStack stack);
}
