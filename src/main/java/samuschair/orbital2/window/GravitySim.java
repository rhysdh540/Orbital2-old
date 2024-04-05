package samuschair.orbital2.window;

import org.joml.Vector2d;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.Body;

import java.util.Objects;

import static org.lwjgl.nuklear.Nuklear.*;

public class GravitySim extends Window {
	public GravitySim() {
		super("Simulator", 1000, 1000);
		resetPositions();
		timeControls.onReset(this::resetPositions);
	}

	public final TimeControls timeControls = new TimeControls();
	final Body outer = new Body(100, 10, 35, 87, 219);
	final Body inner = new Body(10000, 50, 255, 255, 240);

	boolean walls = true;

	private static final double G = 5e-2;
	private static final double ENERGY_KEPT_ON_COLLISION = 0.95;
	private static final int TIMESCALE_PRECISION = 10;

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
			if(walls) {
				keepInside(outer, space);
				keepInside(inner, space);
			}
		}

		nk_fill_circle(canvas,
				nk_rect((float) outer.position.x - outer.radius,
						(float) outer.position.y - outer.radius,
						outer.getDiameter(),
						outer.getDiameter(), space),
				nk_rgb_cf(outer.color, NkColor.malloc(stack)));

		nk_fill_circle(canvas,
				nk_rect((float) inner.position.x - inner.radius,
						(float) inner.position.y - inner.radius,
						inner.getDiameter(),
						inner.getDiameter(), space),
				nk_rgb_cf(inner.color, NkColor.malloc(stack)));
	}

	private void resetPositions() {
		inner.position.x = (double) width / 2;
		inner.position.y = (double) height / 2;
		inner.velocity.set(0, -0.005);

		outer.position.x = inner.position.x + 400;
		outer.position.y = inner.position.y;

		outer.velocity.set(0, Math.sqrt(G * inner.mass / outer.position.distance(inner.position)));
	}

	private void moveBodies() {
		int timescale = (int) (timeControls.getTimescale() * TIMESCALE_PRECISION);
		for(int i = 0; i < timescale; i++) {
			double f = G * (outer.mass * inner.mass) / Math.pow(outer.position.distance(inner.position), 2);

			double a1 = f / inner.mass;
			double a2 = f / outer.mass;

			double angle = Math.atan2(inner.position.y - outer.position.y, inner.position.x - outer.position.x);
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);

			inner.acceleration.set(-(a1 * cos), -(a1 * sin)).div(TIMESCALE_PRECISION);
			outer.acceleration.set(a2 * cos, a2 * sin).div(TIMESCALE_PRECISION);

			inner.velocity.add(inner.acceleration);
			outer.velocity.add(outer.acceleration);

			inner.position.add(inner.velocity.div(TIMESCALE_PRECISION, new Vector2d()));
			outer.position.add(outer.velocity.div(TIMESCALE_PRECISION, new Vector2d()));
		}
	}

	private static void keepInside(Body body, NkRect space) {
		double oldX = body.position.x, oldY = body.position.y;
		body.position.x = Math.max(space.x() + body.radius, body.position.x);
		body.position.x =  Math.min(space.x() + space.w() - body.radius, body.position.x);
		if(body.position.x != oldX) {
			body.velocity.mul(-ENERGY_KEPT_ON_COLLISION, 1);
		}

		body.position.y = Math.max(space.y() + body.radius, body.position.y);
		body.position.y = Math.min(space.y() + space.h() - body.radius, body.position.y);
		if(body.position.y != oldY) {
			body.velocity.mul(1, -ENERGY_KEPT_ON_COLLISION);
		}
	}
}
