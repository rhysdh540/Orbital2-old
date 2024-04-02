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

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		NkCommandBuffer canvas = nk_window_get_canvas(ctx);
		Objects.requireNonNull(canvas);

		nk_layout_row_dynamic(ctx, 120, 1);

		NkRect space = NkRect.malloc(stack);
		nk_widget(space, ctx);

		nk_fill_rect(canvas, space, 2, nk_rgb(160, 160, 160, NkColor.malloc(stack)));

		nk_fill_circle(canvas, nk_rect(space.x() + 20 + motion_X % 100, space.y() + 60, 40, 40, space), nk_rgb(50, 50, 220, NkColor.malloc(stack)));
		motion_X++;

		nk_stroke_line(canvas, space.x() +10, space.y() + 100, space.x() + 150, space.y() + 100, 3.0f, nk_rgb(200, 20, 20, NkColor.malloc(stack)));

		nk_layout_row_dynamic(ctx, 50, 1);
		nk_label(ctx, "Hello world!", NK_TEXT_LEFT);

		nk_layout_row_static(ctx, 30, 80, 1);
		if (nk_button_label(ctx, "AnyButton"))
			System.out.println("AnyButton pressed");
	}
}
