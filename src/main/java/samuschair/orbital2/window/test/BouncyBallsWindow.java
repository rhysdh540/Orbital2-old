package samuschair.orbital2.window.test;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.window.Window;

import java.util.Objects;

import static org.lwjgl.nuklear.Nuklear.*;

public class BouncyBallsWindow extends Window {

	private static final float FRICTION = 0.995f;

	public BouncyBallsWindow() {
		super("Bouncy Balls", 600, 600);
	}

	Circle[] circles = {
			new Circle(0, 0, 40),
			new Circle(10000, 0, 80),
			new Circle(0, 10000, 60),
			new Circle(10000, 10000, 100)
	};

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
			for (Circle circle : circles) {
				circle.vx(mouse.x() - circle.radius - circle.x);
				circle.vy(mouse.y() - circle.radius - circle.y);
			}
		} else {
			for(Circle circle : circles) {
				//slow down
				circle.vx(circle.vx() * FRICTION);
				circle.vy(circle.vy() * FRICTION);
			}
		}

		// move the circle
		for(Circle circle : circles) {
			circle.x += (int) circle.vx() / 10;
			circle.y += (int) circle.vy() / 10;
		}

		stopCirclesFromHittingEachOther();

		for(Circle circle : circles) {
			keepInside(circle, space);
		}

		for(Circle circle : circles) {
			nk_fill_circle(canvas, nk_rect(circle.x, circle.y, circle.diameter, circle.diameter, space), nk_rgb(50, 50, 220, NkColor.malloc(stack)));
		}
	}

	private static void keepInside(Circle circle, NkRect space) {
		int oldX = circle.x, oldY = circle.y;
		circle.x = (int) Math.max(space.x(), circle.x);
		circle.x = (int) Math.min(space.x() + space.w() - circle.diameter, circle.x);
		if(circle.x != oldX) {
			circle.vx(-circle.vx());
		}

		circle.y = (int) Math.max(space.y(), circle.y);
		circle.y = (int) Math.min(space.y() + space.h() - circle.diameter, circle.y);
		if(circle.y != oldY) {
			circle.vy(-circle.vy());
		}
	}

	private void stopCirclesFromHittingEachOther() {
		for(int i = 0; i < circles.length; i++) {
			Circle circle = circles[i];
			for(int j = i + 1; j < circles.length; j++) {
				Circle other = circles[j];
				if(Math.abs(circle.cx() - other.cx()) < circle.radius + other.radius && Math.abs(circle.cy() - other.cy()) < circle.radius + other.radius) {
					{
						float temp = circle.vx();
						circle.vx(other.vx());
						other.vx(temp);
					}
					{
						float temp = circle.vy();
						circle.vy(other.vy());
						other.vy(temp);
					}

					// make sure they arent touching anymore
					for(int k = 0; Math.abs(circle.cx() - other.cx()) < circle.radius + other.radius && Math.abs(circle.cy() - other.cy()) < circle.radius + other.radius && k < 10000; k++) {
						circle.x += (int) circle.vx() / 10;
						circle.y += (int) circle.vy() / 10;
						other.x += (int) other.vx() / 10;
						other.y += (int) other.vy() / 10;
					}
				}
			}
		}
	}

	public static class Circle {

		// from top left corner of the square
		public int x, y;
		public final NkVec2 velocity;

		public final int diameter;
		public final int radius;

		public Circle(int x, int y, int diameter) {
			this.x = x;
			this.y = y;
			this.velocity = NkVec2.create();
			this.diameter = diameter;
			this.radius = diameter / 2;
		}

		public int cx() {
			return x + radius;
		}

		public int cy() {
			return y + radius;
		}

		public float vx() {
			return velocity.x();
		}

		public float vy() {
			return velocity.y();
		}

		public void vx(float vx) {
			velocity.x(vx);
		}

		public void vy(float vy) {
			velocity.y(vy);
		}
	}
}
