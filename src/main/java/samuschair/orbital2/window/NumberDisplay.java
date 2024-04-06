package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.Body;
import samuschair.orbital2.util.MathUtil;

import java.text.DecimalFormat;

import static org.lwjgl.nuklear.Nuklear.*;

public class NumberDisplay extends Window {
	public NumberDisplay(GravitySim sim) {
		super("Numbers", 300, 575);
		this.sim = sim;
	}

	private final GravitySim sim;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		nk_layout_row_dynamic(ctx, 30, 1);
		nk_label(ctx, "Inner Body", NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_BOTTOM);
		body(sim.inner, ctx);
		nk_label(ctx, "Outer Body", NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_BOTTOM);
		body(sim.outer, ctx);

		nk_label(ctx, "Simulation", NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_BOTTOM);
		nk_label(ctx, "G: " + GravitySim.G, NK_TEXT_LEFT);
		nk_label(ctx, "Distance between bodies: " + MathUtil.round(sim.inner.position.distance(sim.outer.position), 5), NK_TEXT_LEFT);
		nk_label(ctx, "Energy kept on collision: " + GravitySim.ENERGY_KEPT_ON_COLLISION, NK_TEXT_LEFT);

		double ke = MathUtil.round(sim.inner.kineticEnergy() + sim.outer.kineticEnergy(), 3);
		nk_label(ctx, "Kinetic energy: " + ke, NK_TEXT_LEFT);
		double pe = MathUtil.round(GravitySim.G * (sim.inner.mass * sim.outer.mass / sim.inner.position.distanceSquared(sim.outer.position)), 3);
		nk_label(ctx, "Potential energy: " + pe, NK_TEXT_LEFT);
		nk_label(ctx, "Total energy: " + MathUtil.round(ke + pe, 3), NK_TEXT_LEFT);
	}

	// needed for small numbers like acceleration and velocity to not have scientific notation
	private static final DecimalFormat df = new DecimalFormat("#.##########");

	private static void body(Body body, NkContext ctx) {
		nk_label(ctx, "Acceleration: " + df.format(body.acceleration.length()), NK_TEXT_LEFT);
		nk_label(ctx, "Velocity: " + df.format(body.velocity.length()), NK_TEXT_LEFT);
		nk_label(ctx, "Position: " + body.position, NK_TEXT_LEFT);
	}
}
