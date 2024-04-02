package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;

import static org.lwjgl.nuklear.Nuklear.*;

public class DraggyCircleWindow extends Window {
	public DraggyCircleWindow() {
		super("Draggy Circle");
	}

	int ballX = 0, ballY = 0;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		NkCommandBuffer canvas = nk_window_get_canvas(ctx);
		Objects.requireNonNull(canvas);

		nk_layout_row_static(ctx, height - 60, width - 25, 1);

		NkRect space = NkRect.malloc(stack)
				.x(this.x)
				.y(this.y)
				.w(width)
				.h(height);
		nk_widget(space, ctx);

		nk_fill_rect(canvas, space, 0, nk_rgb(255, 255, 255, NkColor.malloc(stack)));
		//if mouse is down, move the circle to the mouse position
		if (nk_input_is_mouse_down(ctx.input(), NK_BUTTON_LEFT) && nk_input_has_mouse_click_in_rect(ctx.input(), NK_BUTTON_LEFT, space)) {
			NkVec2 mouse = ctx.input().mouse().pos();
			ballX = (int) (mouse.x() - 20);
			ballY = (int) (mouse.y() - 20);
		}
//		System.out.printf("(%d, %d)\n", ballX, ballY);
		ballX = Math.max(this.x + 5, ballX);
		ballY = Math.max(this.y + 40, ballY);
		ballX = Math.min(this.x + width - 58, ballX);
		ballY = Math.min(this.y + height - 58, ballY);

		nk_fill_circle(canvas, nk_rect(ballX, ballY, 40, 40, space), nk_rgb(50, 50, 220, NkColor.malloc(stack)));
	}
}
