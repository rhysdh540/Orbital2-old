package samuschair.orbital2.window;

import lombok.Getter;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;
import samuschair.orbital2.util.MathUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nuklear.Nuklear.*;

public class TimeControls extends Window {
	public TimeControls() {
		super("Time", 300, 160, FLAGS_NO_RESIZE);
	}

	@Getter
	private boolean running = false;

	@Getter
	private float timescale = 1;

	private boolean started = false;

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

		FloatBuffer buf = stack.floats(timescale);
		nk_layout_row_dynamic(ctx, 30, 1);
		nk_label(ctx, "Timescale: " + timescale, NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_BOTTOM);
		nk_slider_float(ctx, 0.1f, buf, 10.0f, 0.1f);
		timescale = MathUtil.round(buf.get(0), 1);
	}

	public void onReset(Runnable r) {
		onReset.add(r);
	}
}
