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
			velocity.x(mouse.x() - 20 - ballX);
			velocity.y(mouse.y() - 20 - ballY);
		} else {
			//slow down
			velocity.x(velocity.x() * 0.995f);
			velocity.y(velocity.y() * 0.995f);
		}

		int oldX = ballX, oldY = ballY;
		ballX += (int) velocity.x() / 10;
		ballY += (int) velocity.y() / 10;
		keepInsideWindow();
		if(oldX == ballX)
			velocity.x(0);
		if(oldY == ballY)
			velocity.y(0);

		nk_fill_circle(canvas, nk_rect(ballX, ballY, 40, 40, space), nk_rgb(50, 50, 220, NkColor.malloc(stack)));
	}

	private void keepInsideWindow() {
		int oldX = ballX, oldY = ballY;
		ballX = Math.max(this.x + 6, ballX);
		ballX = Math.min(this.x + width - 58, ballX);
		if(ballX != oldX) {
			boolean windowChanged = this.oldWidth != this.width || this.oldX != this.x;
			if(windowChanged) {
				float diff = (float) (this.width - this.oldWidth);
				if(diff == 0) {
					diff = this.x - this.oldX;
				}
				velocity.x(velocity.x() + diff);
			} else {
				velocity.x(-velocity.x());
			}
		}

		ballY = Math.max(this.y + 40, ballY);
		ballY = Math.min(this.y + height - 60, ballY);
		if(ballY != oldY) {
			boolean windowChanged = this.oldHeight != this.height || this.oldY != this.y;
			if(windowChanged) {
				float diff = (float) (this.height - this.oldHeight);
				if(diff == 0) {
					diff = this.y - this.oldY;
				}
				velocity.y(velocity.y() + diff);
			} else {
				velocity.y(-velocity.y());
			}
		}
	}
}
