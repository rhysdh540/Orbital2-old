package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.nuklear.Nuklear.*;

public class CameraControls extends Window {
	public CameraControls(GravitySim sim) {
		super("Camera", 300, 160, FLAGS_NO_RESIZE);
		this.sim = sim;
	}

	private final GravitySim sim;

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		nk_layout_row_dynamic(ctx, 30, 1);
		if(nk_button_label(ctx, "Reset Camera")) {
			sim.offset.set(0, 0);
			sim.zoomScale = 1;
		}
	}
}
