package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.Body;

import static org.lwjgl.nuklear.Nuklear.*;

public class NumberDisplay extends Window {
	public NumberDisplay(GravitySim sim) {
		super("Numbers", 300, 325);
		this.sim = sim;
	}

	private final GravitySim sim;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		nk_layout_row_dynamic(ctx, 30, 1);
		body(sim.outer, ctx);
		nk_label(ctx, "Inner:", NK_TEXT_LEFT);
		body(sim.inner, ctx);

		nk_label(ctx, "Distance: " + sim.inner.position.distance(sim.outer.position), NK_TEXT_LEFT);
	}

	private static void body(Body body, NkContext ctx) {
		nk_label(ctx, "Acceleration: " + body.acceleration, NK_TEXT_LEFT);
		nk_label(ctx, "Velocity: " + body.velocity, NK_TEXT_LEFT);
		nk_label(ctx, "Position: " + body.position, NK_TEXT_LEFT);
	}
}
