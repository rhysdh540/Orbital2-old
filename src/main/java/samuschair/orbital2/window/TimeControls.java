package samuschair.orbital2.window;

import lombok.Getter;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryUtil.memASCII;

public class TimeControls extends Window {
	public TimeControls() {
		super("Time", 300, 160, FLAGS_NO_RESIZE);
	}

	@Getter
	private boolean running = false;

	@Getter
	private float timescale = 1;

	private boolean started = false;

	private String timescaleString = "1";

	private final List<Runnable> onReset = new ArrayList<>();

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		nk_layout_row_dynamic(ctx, 30, 2);
		if(nk_button_label(ctx, !started ? "Start" : running ? "Pause" : "Resume")) {
			running = !running;
			started = true;
		}

		if(nk_button_label(ctx, "Reset")) {
			running = false;
			started = false;
			timescale = 1;
			onReset.forEach(Runnable::run);
		}

		nk_layout_row_dynamic(ctx, 30, 1);
		nk_label(ctx, "Timescale: " + timescale, NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_BOTTOM);
		ByteBuffer buffer = stack.calloc(11);
		IntBuffer len = stack.ints(memASCII(timescaleString, false, buffer));
		nk_edit_string(ctx, NK_EDIT_SIMPLE, buffer, len, 10, NkPluginFilter.create(Nuklear::nnk_filter_decimal));
		timescaleString = memASCII(buffer, len.get(0));
		if(nk_input_is_key_pressed(ctx.input(), NK_KEY_ENTER)) {
			try {
				timescale = Float.parseFloat(timescaleString);
			} catch(NumberFormatException e) {
			}
		}
	}

	public void onReset(Runnable r) {
		onReset.add(r);
	}
}
