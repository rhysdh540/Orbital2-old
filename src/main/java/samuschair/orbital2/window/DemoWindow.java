package samuschair.orbital2.window;

import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryUtil.memASCII;

public class DemoWindow extends Window {

	private static final int EASY = 0;
	private static final int HARD = 1;

	public NkColorf background = NkColorf.create()
			.r(0.10f)
			.g(0.18f)
			.b(0.24f)
			.a(1.0f);

	private int op = EASY;

	private final IntBuffer options = BufferUtils.createIntBuffer(1).put(0, 20);

	private String text = "";

	public DemoWindow() {
		super("Demo");
	}

	@Override
	public void render(NkContext ctx, MemoryStack stack) {
		nk_layout_row_static(ctx, 30, 80, 1);
		if (nk_button_label(ctx, "button")) {
			System.out.println("button pressed");
		}

		nk_layout_row_dynamic(ctx, 30, 2);
		if (nk_option_label(ctx, "easy", op == EASY)) {
			op = EASY;
		}
		if (nk_option_label(ctx, "hard", op == HARD)) {
			op = HARD;
		}

		nk_layout_row_dynamic(ctx, 25, 1);
		nk_property_int(ctx, "number:", 0, options, 100, 10, 1);

		nk_layout_row_dynamic(ctx, 20, 1);
		nk_label(ctx, "background:", NK_TEXT_LEFT);
		nk_layout_row_dynamic(ctx, 25, 1);
		if (nk_combo_begin_color(ctx, nk_rgb_cf(background, NkColor.malloc(stack)), NkVec2.malloc(stack).set(nk_widget_width(ctx), 400))) {
			nk_layout_row_dynamic(ctx, 120, 1);
			nk_color_picker(ctx, background, NK_RGBA);
			nk_layout_row_dynamic(ctx, 25, 1);
			background
					.r(nk_propertyf(ctx, "#R:", 0, background.r(), 1.0f, 0.01f, 0.005f))
					.g(nk_propertyf(ctx, "#G:", 0, background.g(), 1.0f, 0.01f, 0.005f))
					.b(nk_propertyf(ctx, "#B:", 0, background.b(), 1.0f, 0.01f, 0.005f))
					.a(nk_propertyf(ctx, "#A:", 0, background.a(), 1.0f, 0.01f, 0.005f));
			nk_combo_end(ctx);
		}

		nk_layout_row_dynamic(ctx, 30, 1);
		ByteBuffer buffer = stack.calloc(256);
		int length = memASCII(text, false, buffer);

		IntBuffer len = stack.ints(length);
		nk_edit_string(ctx, NK_EDIT_FIELD, buffer, len, 255, filter);
		try {
			text = memASCII(buffer, len.get(0));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
