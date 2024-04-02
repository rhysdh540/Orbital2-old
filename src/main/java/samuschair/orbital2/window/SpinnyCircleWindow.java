package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;

import static org.lwjgl.nuklear.Nuklear.*;

public class SpinnyCircleWindow extends Window {
	public SpinnyCircleWindow() {
		super("Spinny Circle");
	}

	private int motion_X = 0;
	private int motion_Y = 0;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		NkCommandBuffer canvas = nk_window_get_canvas(ctx);
		Objects.requireNonNull(canvas);

		nk_layout_row_dynamic(ctx, 120, 1);

		NkRect space = NkRect.malloc(stack)
						.w(width)
						.h(height);
		nk_widget(space, ctx);

//		nk_fill_rect(canvas, space, 0, nk_rgb(50, 50, 50, NkColor.malloc(stack)));
		float x = (float) (space.x() + Math.cos(motion_X * 0.1) * 50) + width / 2 - 20;
		float y = (float) (space.y()  + Math.sin(motion_Y * 0.1) * 50) + height / 2 - 20;
		nk_fill_circle(canvas, nk_rect(x, y, 40, 40, space), nk_rgb(50, 50, 220, NkColor.malloc(stack)));
		motion_X++;
		motion_Y++;
	}
}
