package samuschair.orbital2.window;

import org.joml.Vector2d;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkMouse;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.Orbital2Main;
import samuschair.orbital2.sim.Body;
import samuschair.orbital2.sim.GravitySim;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;

public class SimWindow extends Window {
	public SimWindow() {
		super("Gravity Simulation", 1000, 1000);

		Body earth = Body.builder() // Earth
				.withMass(100)
				.withRadius(10)
				.withColor(35, 87, 219)
				.withPosition(width / 2 + 400, height / 2)
				.build();

		Body sun = Body.builder() // Sun
				.withMass(10000)
				.withRadius(50)
				.withColor(255, 255, 240)
				.withPosition(width / 2, height / 2)
				.build();

		earth.orbit(sun, sim.G);
		sim.addBody(earth);
		sim.addBody(sun);
	}

	private final Vector2d offset = new Vector2d(0, 0);
	private double zoomScale = 1;

	private final GravitySim sim = new GravitySim(0.05);

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		sim.update(1);

		NkCommandBuffer canvas = Objects.requireNonNull(nk_window_get_canvas(ctx));

		nk_layout_row_dynamic(ctx, height - 56, 1);
		NkRect space = nk_window_get_bounds(ctx, NkRect.malloc(stack));

		panView(ctx, space);

		for (Body body : sim.bodies) {
			drawCircle(canvas, space, body);
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

	private void drawCircle(NkCommandBuffer canvas, NkRect space, Body body) {
		float d = body.getDiameter() * (float) zoomScale;

		nk_fill_circle(canvas,
				nk_rect((float) (body.position.x - body.radius + offset.x) * (float) zoomScale,
						(float) (body.position.y - body.radius + offset.y) * (float) zoomScale,
						d, d, space),
				nk_rgb_cf(body.colorAsNkColorf(), NkColor.malloc()));
	}
}
