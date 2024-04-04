package samuschair.orbital2.window;

import lombok.AccessLevel;
import lombok.Setter;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryUtil.memASCII;

public class SingleDimensionBoxCollisionsWindow extends Window {
	private static final NkPluginFilter filter = NkPluginFilter.create(Nuklear::nnk_filter_decimal);
	private static final int BOX_WIDTH = 50;
	private static final int INVALID = Integer.MIN_VALUE;

	public SingleDimensionBoxCollisionsWindow() {
		super("1D Box Collisions", 1500, 1000);
	}

	@Setter(AccessLevel.PRIVATE)
	private int mass1 = 1, mass2 = 1;

	@Setter(AccessLevel.PRIVATE)
	private float velocity1 = INVALID, velocity2 = INVALID;

	private int position1 = 0, position2 = 100;

	private boolean running = false;

	private boolean wasInvalid = false;

	@Override
	public void render(NkContext ctx, MemoryStack stack) {
		//row 1: controls
		nk_layout_row_dynamic(ctx, 30, 2);
		numField(ctx, stack, "Mass 1:", mass1, mass11 -> setMass1((int) mass11.floatValue()));
		numField(ctx, stack, "Mass 2:", mass2, mass22 -> setMass2((int) mass22.floatValue()));
		numField(ctx, stack, "Velocity 1:", (int) velocity1, this::setVelocity1);
		numField(ctx, stack, "Velocity 2:", (int) velocity2, this::setVelocity2);
		if(nk_button_label(ctx, running ? "Stop" : "Start")) {
			running = !running;
		}
		if(nk_button_label(ctx, "Reset")) {
			position1 = 0;
			position2 = 100;
			velocity1 = 0;
			velocity2 = 0;
			running = false;
		}
		if((running || wasInvalid) && (mass1 == INVALID || mass2 == INVALID || velocity1 == INVALID || velocity2 == INVALID)) {
			running = false;
			wasInvalid = true;
			nk_label(ctx, "Please fill in all fields", NK_TEXT_ALIGN_LEFT);
		}
		nk_layout_row_end(ctx);

		//row 2: the simulation
		nk_layout_row_dynamic(ctx, height - 30 - 200, 1);
		NkCommandBuffer canvas = nk_window_get_canvas(ctx);
		Objects.requireNonNull(canvas);

		NkRect space = NkRect.malloc(stack)
				.x(this.x)
				.y(this.y + 30)
				.w(width)
				.h(height - 30 - 200);
		nk_widget(space, ctx);
		nk_fill_rect(canvas, space, 0, nk_rgb(255, 255, 255, NkColor.malloc(stack)));

		//draw the floor
		nk_stroke_line(canvas, this.x, this.y + 30 + height - 30 - 200, this.x + width, this.y + 30 + height - 30 - 200, 1, nk_rgb(0, 0, 0, NkColor.malloc(stack)));

		if(running) {
			runSimulation();
		}

		//draw the boxes
		nk_fill_rect(canvas, nk_rect(this.x + position1, this.y + 30 + height - 30 - 200 - 50, BOX_WIDTH, 50, space), 0, nk_rgb(255, 0, 0, NkColor.malloc(stack)));
		nk_fill_rect(canvas, nk_rect(this.x + position2, this.y + 30 + height - 30 - 200 - 50, BOX_WIDTH, 50, space), 0, nk_rgb(0, 0, 255, NkColor.malloc(stack)));
	}

	private void runSimulation() {
		//move the boxes
		position1 += velocity1;
		position2 += velocity2;

		//move them back if they collide with each other or the wall
		if(position1 < 0) {
			position1 = 0;
			velocity1 = -velocity1;
		}

		if(position1 + BOX_WIDTH >= position2) {
			position1 = position2 - BOX_WIDTH;

			float vf1 = ((mass1 - mass2) * velocity1 + 2 * mass2 * velocity2) / (mass1 + mass2);
			float vf2 = (2 * mass1 * velocity1 + (mass2 - mass1) * velocity2) / (mass1 + mass2);

			velocity1 = vf1;
			velocity2 = vf2;
		}

		if(position2 + BOX_WIDTH > width) {
			position2 = width - BOX_WIDTH;
			velocity2 = -velocity2;
		}
	}

	private static float parse(ByteBuffer buffer, IntBuffer length) {
		String text = memASCII(buffer, length.get(0));
		if(text.isEmpty() || text.equals("-")) {
			return INVALID;
		}
		return Float.parseFloat(text);
	}

	private static String valueOf(int value) {
		return value == INVALID ? "" : String.valueOf(value);
	}

	private static void numField(NkContext ctx, MemoryStack stack, String label, int value, Consumer<Float> setter) {
		ByteBuffer buffer = stack.calloc(5);
		IntBuffer length = stack.ints(memASCII(valueOf(value), false, buffer));
		nk_label(ctx, label, NK_TEXT_ALIGN_LEFT);
		nk_edit_string(ctx, NK_EDIT_SIMPLE, buffer, length, 5, filter);
		setter.accept(parse(buffer, length));
	}
}
