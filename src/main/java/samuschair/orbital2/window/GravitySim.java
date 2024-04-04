package samuschair.orbital2.window;

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
	final Body outer = new Body(100, 10);
	final Body inner = new Body(10000, 50);

	private static final double G = 0.05;
	private static final double ENERGY_LOST_ON_COLLISION = 0.95;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		NkCommandBuffer canvas = nk_window_get_canvas(ctx);
		Objects.requireNonNull(canvas);

		nk_layout_row_dynamic(ctx, height - 56, 1);
		NkRect space = nk_window_get_bounds(ctx, NkRect.malloc(stack));

		nk_widget(space, ctx);
		nk_stroke_rect(canvas, space, 0, 1, nk_rgb(255, 255, 255, NkColor.malloc(stack)));

		if(timeControls.isRunning()) {
			moveBodies();
			keepInside(outer, space);
			keepInside(inner, space);
		}

		nk_fill_circle(canvas,
				nk_rect((float) outer.position.x - outer.getRadius(),
						(float) outer.position.y - outer.getRadius(),
						outer.getDiameter(),
						outer.getDiameter(), space),
				nk_rgb(255, 255, 255, NkColor.malloc(stack)));

		nk_fill_circle(canvas,
				nk_rect((float) inner.position.x - inner.getRadius(),
						(float) inner.position.y - inner.getRadius(),
						inner.getDiameter(),
						inner.getDiameter(), space),
				nk_rgb(0, 255, 255, NkColor.malloc(stack)));
	}

	private void resetPositions() {
		inner.position.x = (double) width / 2;
		inner.position.y = (double) height / 2;
		inner.setVelocity(0, -0.005);
		inner.setAcceleration(0, 0);

		outer.position.x = inner.position.x + 400;
		outer.position.y = inner.position.y;
		outer.setAcceleration(0, 0);
		outer.setVelocity(0, 1.125); // TODO: programmatically figure out the correct speed for a circular orbit
	}

	private void moveBodies() {
		double timescale = timeControls.getTimescale();
		double f = G * (outer.getMass() * inner.getMass()) / Math.pow(outer.position.distance(inner.position), 2);

		double a1 = f / inner.getMass();
		double a2 = f / outer.getMass();

		double angle = Math.atan2(inner.position.y - outer.position.y, inner.position.x - outer.position.x);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		inner.acceleration.set(-(a1 * cos), -(a1 * sin));
		outer.acceleration.set(a2 * cos, a2 * sin);

		inner.velocity.add(inner.acceleration.x * timescale, inner.acceleration.y * timescale);
		outer.velocity.add(outer.acceleration.x * timescale, outer.acceleration.y * timescale);

		inner.position.add(inner.velocity.x * timescale, inner.velocity.y * timescale);
		outer.position.add(outer.velocity.x * timescale, outer.velocity.y * timescale);
	}

	private static void keepInside(Body body, NkRect space) {
		double oldX = body.position.x, oldY = body.position.y;
		body.position.x = Math.max(space.x() + body.getRadius(), body.position.x);
		body.position.x =  Math.min(space.x() + space.w() - body.getRadius(), body.position.x);
		if(body.position.x != oldX) {
			body.velocity.mul(-ENERGY_LOST_ON_COLLISION, 1);
		}

		body.position.y = Math.max(space.y() + body.getRadius(), body.position.y);
		body.position.y = Math.min(space.y() + space.h() - body.getRadius(), body.position.y);
		if(body.position.y != oldY) {
			body.velocity.mul(1, -ENERGY_LOST_ON_COLLISION);
		}
	}
}