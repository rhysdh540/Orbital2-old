package samuschair.orbital2.window;

import org.joml.Vector2d;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.Body;
import samuschair.orbital2.util.Pair;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryUtil.memASCII;

public class NumberEdit extends Window {
	public NumberEdit(GravitySim sim) {
		super("Edit", 300, 575);
		this.sim = sim;
	}

	private final GravitySim sim;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		body(ctx, stack, "Outer Body", sim.outer);
		body(ctx, stack, "Inner Body", sim.inner);
	}

	private void body(NkContext ctx, MemoryStack stack, String name, Body body) {
		nk_layout_row_dynamic(ctx, 30, 1);
		nk_label(ctx, name, NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_BOTTOM);
		nk_layout_row_dynamic(ctx, 30, 2);
		nk_label(ctx, "Mass", NK_TEXT_LEFT);
		body.mass = editInt(ctx, stack, body.mass);
		nk_label(ctx, "Radius", NK_TEXT_LEFT);
		body.radius = editInt(ctx, stack, body.radius);
		nk_label(ctx, "Color", NK_TEXT_LEFT);
		editColor(ctx, stack, body.color);

		editVector(ctx, stack, body.position, "Position");
		editVector(ctx, stack, body.velocity, "Velocity");
	}

	@SuppressWarnings("DuplicatedCode")
	private void editColor(NkContext ctx, MemoryStack stack, NkColorf color) {
		if (nk_combo_begin_color(ctx, nk_rgb_cf(color, NkColor.malloc(stack)), NkVec2.malloc(stack).set(nk_widget_width(ctx), 400))) {
			nk_layout_row_dynamic(ctx, 120, 1);
			nk_color_picker(ctx, color, NK_RGBA);
			nk_layout_row_dynamic(ctx, 25, 1);
			color
					.r(nk_propertyf(ctx, "#R:", 0, color.r(), 1.0f, 0.01f, 0.005f))
					.g(nk_propertyf(ctx, "#G:", 0, color.g(), 1.0f, 0.01f, 0.005f))
					.b(nk_propertyf(ctx, "#B:", 0, color.b(), 1.0f, 0.01f, 0.005f))
					.a(nk_propertyf(ctx, "#A:", 0, color.a(), 1.0f, 0.01f, 0.005f));
			nk_combo_end(ctx);
		}
	}

	private int editInt(NkContext ctx, MemoryStack stack, int value) {
		ByteBuffer buffer = stack.calloc(11);
		IntBuffer len = stack.ints(memASCII(String.valueOf(value), false, buffer));
		nk_edit_string(ctx, NK_EDIT_FIELD, buffer, len, 10, NkPluginFilter.create(Nuklear::nnk_filter_decimal));
		String str = memASCII(buffer, len.get(0));
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return value;
		}
	}


	private static final Map<Vector2d, Pair<String, String>> vectors = new HashMap<>();

	private void editVector(NkContext ctx, MemoryStack stack, Vector2d value, String label) {
		// Calculate the width of the window


		Pair<String, String> strings = vectors.computeIfAbsent(value, ignore -> Pair.of("", ""));

		// Set up the row with static widths
		nk_layout_row_begin(ctx, NK_STATIC, 30, 6);
		nk_layout_row_push(ctx, 70);
		nk_label(ctx, label + ":", NK_TEXT_LEFT);
		nk_layout_row_push(ctx, 8);
		nk_label(ctx, "(", NK_TEXT_LEFT);
		nk_layout_row_push(ctx, 80);

		ByteBuffer buffer = stack.calloc(32);
		IntBuffer len = stack.ints(memASCII(strings.first, false, buffer));
		nk_edit_string(ctx, NK_EDIT_FIELD, buffer, len, 32, NkPluginFilter.create(Nuklear::nnk_filter_float));
		strings.first = memASCII(buffer, len.get(0));
		if(nk_input_is_key_pressed(ctx.input(), NK_KEY_ENTER)) {
			try {
				value.x = Double.parseDouble(strings.first);
			} catch (NumberFormatException e) {
			}
		}

		nk_layout_row_push(ctx, 8);
		nk_label(ctx, ",", NK_TEXT_LEFT);
		nk_layout_row_push(ctx, 80);

		buffer = stack.calloc(32);
		len = stack.ints(memASCII(strings.second, false, buffer));
		nk_edit_string(ctx, NK_EDIT_FIELD, buffer, len, 32, NkPluginFilter.create(Nuklear::nnk_filter_float));
		strings.second = memASCII(buffer, len.get(0));
		if(nk_input_is_key_pressed(ctx.input(), NK_KEY_ENTER)) {
			try {
				value.y = Double.parseDouble(strings.second);
			} catch (NumberFormatException e) {
			}
		}

		nk_layout_row_push(ctx, 10);
		nk_label(ctx, ")", NK_TEXT_LEFT);
		nk_layout_row_end(ctx);
	}
}
