package samuschair.orbital2.window;

import org.joml.Vector2d;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkMouse;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.Body;
import samuschair.orbital2.Orbital2Main;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;

public class GravitySim extends Window {
	public GravitySim() {
		super("Simulator", 1000, 1000);
		resetPositions();
		timeControls.onReset(this::resetPositions);
	}

	public final TimeControls timeControls = new TimeControls();
	public final CameraControls cameraControls = new CameraControls(this);
	public final NumberEdit numberEdit = new NumberEdit(this);
	public final NumberDisplay numberDisplay = new NumberDisplay(this);

	final Body[] bodies = {
			Body.builder() // Sun
					.withMass(10000)
					.withRadius(50)
					.withColor(255, 255, 240)
					.withPosition(width / 2, height / 2)
					.build(),
			Body.builder() // Earth
					.withMass(100)
					.withRadius(10)
					.withColor(35, 87, 219)
					.withPosition(width / 2 + 400, height / 2)
			.build(),
			Body.builder()
					.withMass(10)
					.withRadius(5)
					.withColor(255, 0, 0)
					.withPosition(500, 500)
					.build(),
	};

	final Body inner = bodies[1];
	final Body outer = bodies[0];

	final Vector2d offset = new Vector2d();
	double zoomScale = 1;

	boolean walls = false;

	static final double G = 0.05;//6.67430e-11;
	static final double ENERGY_KEPT_ON_COLLISION = 1; // 0 is inelastic, 1 is elastic
	static final int TIMESCALE_PRECISION = 1;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		NkCommandBuffer canvas = nk_window_get_canvas(ctx);
		Objects.requireNonNull(canvas);

		nk_layout_row_dynamic(ctx, height - 56, 1);
		NkRect space = nk_window_get_bounds(ctx, NkRect.malloc(stack));

		nk_widget(space, ctx);
		if(walls) {
			nk_stroke_rect(canvas, space, 0, 1, nk_rgb(255, 255, 255, NkColor.malloc(stack)));
		}

		if(timeControls.isRunning()) {
			moveBodies();
		}

		panView(ctx, space);

		for(Body body : bodies) {
			drawCircle(ctx, stack, canvas, space, body);
		}
	}

	private void drawCircle(NkContext ctx, MemoryStack stack, NkCommandBuffer canvas, NkRect space, Body body) {
		float d = body.getDiameter() * (float) zoomScale;

		nk_fill_circle(canvas,
				nk_rect((float) (body.position.x - body.radius + offset.x) * (float) zoomScale,
						(float) (body.position.y - body.radius + offset.y) * (float) zoomScale,
						d, d, space),
				nk_rgb_cf(body.color, NkColor.malloc(stack)));
	}

	/*
	private void drawCircle(NkContext ctx, MemoryStack stack, NkCommandBuffer canvas, NkRect space, Body body) {
		NkMouse mouse = ctx.input().mouse();
		float mouseX = mouse.pos().x();
		float mouseY = mouse.pos().y();

		float d = body.getDiameter() * (float) zoomScale;

		// Calculate the offset from the mouse to the object's center
		float offsetX = (float) ((body.position.x - mouseX) * (float) zoomScale);
		float offsetY = (float) ((body.position.y - mouseY) * (float) zoomScale);

		// Calculate the new position by applying the zoom scale
		float x = mouseX + offsetX;
		float y = mouseY + offsetY;

		// Draw the circle at the new position
		nk_fill_circle(canvas,
				nk_rect(x, y, d, d, space),
				nk_rgb_cf(body.color, NkColor.malloc(stack)));
	}
	 */

	private void resetPositions() {
		for(Body body : bodies) {
			body.reset();
		}

		double distance = outer.position.distance(inner.position);
		outer.velocity.set(0, Math.sqrt(G * inner.mass / distance));
	}

	private void moveBodies() {
		int timescale = (int) (timeControls.getTimescale() * TIMESCALE_PRECISION);
		int sign = timescale < 0 ? -1 : 1;
		for(int i = 0; i < Math.abs(timescale); i++) {
			for (Body body1 : bodies) {
				Vector2d totalForce = new Vector2d();
				for (Body body2 : bodies) {
					if (body1 != body2) {
						double f = G * (body1.mass * body2.mass) / Math.pow(body1.position.distance(body2.position), 2);
						double angle = Math.atan2(body1.position.y - body2.position.y, body1.position.x - body2.position.x);
						double fx = f * Math.cos(angle);
						double fy = f * Math.sin(angle);
						totalForce.add(fx, fy);
					}
				}
				double a1 = totalForce.length() / body1.mass;
				double angle = Math.atan2(totalForce.y, totalForce.x);
				double cos = Math.cos(angle);
				double sin = Math.sin(angle);

				body1.acceleration.set(-(a1 * cos), -(a1 * sin)).div(TIMESCALE_PRECISION * sign);
				body1.velocity.add(body1.acceleration);
				body1.position.add(body1.velocity.div(TIMESCALE_PRECISION * sign, new Vector2d()));
			}
		}
	}

	private void panView(NkContext ctx, NkRect space) {
		long window = Orbital2Main.getWindowId();
		NkMouse mouse = ctx.input().mouse();
		if(nk_input_is_mouse_hovering_rect(ctx.input(), space)) {
			if(nk_input_is_mouse_down(ctx.input(), NK_BUTTON_LEFT)) {
				glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_CAPTURED);
				NkVec2 delta = mouse.delta();
				if(delta.x() <= 450 && delta.y() <= 450) {
					offset.add(delta.x(), delta.y());
				}
			} else {
				glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
			}
		} else {
			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		}

		zoomScale += mouse.scroll_delta().y() * 0.1;
	}

	private void keepInside(Body body, NkRect space) {
		int r = body.radius;
		double x = body.position.x;
		if(x < space.x() + r + offset.x || x > space.x() + space.w() + offset.x - r) {
			double interceptX = x < space.x() + r ? space.x() + r : space.x() + space.w() - r;
			double oldX = body.position.x - body.velocity.x;
			double scalar = (interceptX - oldX) / (x - oldX); // from 0-1, how far along the line from old to new the intercept is

			double interceptY = body.position.y - body.velocity.y * (1 - scalar);
			body.position.set(interceptX, interceptY);
			body.velocity.x *= -ENERGY_KEPT_ON_COLLISION;
		}

		double y = body.position.y;
		if(y < space.y() + r || y > space.y() + space.h() - r) {
			double interceptY = y < space.y() + r ? space.y() + r : space.y() + space.h() - r;
			double oldY = body.position.y - body.velocity.y;
			double scalar = (interceptY - oldY) / (y - oldY);

			double interceptX = body.position.x - body.velocity.x * (1 - scalar);
			body.position.set(interceptX, interceptY);
			body.velocity.y *= -ENERGY_KEPT_ON_COLLISION;
		}
	}
}
