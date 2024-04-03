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

	private static final int CIRCLE_DIAMETER = 40;
	private static final int CIRCLE_RADIUS = CIRCLE_DIAMETER / 2;

	public DraggyCircleWindow() {
		super("Draggy Circle");
	}

	int ballX = 0, ballY = 0;
	NkVec2 velocity = NkVec2.create();

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

		//if mouse is down, move the circle to the mouse position
		if (nk_input_is_mouse_down(ctx.input(), NK_BUTTON_LEFT) && nk_input_has_mouse_click_in_rect(ctx.input(), NK_BUTTON_LEFT, space)) {
			NkVec2 mouse = ctx.input().mouse().pos();
			velocity.x(mouse.x() - CIRCLE_RADIUS - ballX);
			velocity.y(mouse.y() - CIRCLE_RADIUS - ballY);
		} else {
			//slow down
			velocity.x(velocity.x() * 0.995f);
			velocity.y(velocity.y() * 0.995f);
		}

		ballX += (int) velocity.x() / 10;
		ballY += (int) velocity.y() / 10;
		keepInside(space);

		nk_fill_circle(canvas, nk_rect(ballX, ballY, CIRCLE_DIAMETER, CIRCLE_DIAMETER, space), nk_rgb(50, 50, 220, NkColor.malloc(stack)));
	}

	private void keepInside(NkRect space) {
		int oldX = ballX, oldY = ballY;
		ballX = (int) Math.max(space.x(), ballX);
		ballX = (int) Math.min(space.x() + space.w() - CIRCLE_DIAMETER, ballX);
		if(ballX != oldX) {
			velocity.x(-velocity.x());
		}

		ballY = (int) Math.max(space.y(), ballY);
		ballY = (int) Math.min(space.y() + space.h() - CIRCLE_DIAMETER, ballY);
		if(ballY != oldY) {
			velocity.y(-velocity.y());
		}
	}
}
