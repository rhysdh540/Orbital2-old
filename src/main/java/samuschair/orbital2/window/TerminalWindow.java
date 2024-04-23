package samuschair.orbital2.window;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkStyleItem;
import org.lwjgl.system.MemoryStack;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.lwjgl.nuklear.Nuklear.*;

public class TerminalWindow extends Window {
	public static final Queue<String> messages = new ArrayDeque<>() {
		@Override
		public boolean add(String s) {
			if (size() > 100) {
				remove();
			}
			return super.add(s);
		}
	};

	public TerminalWindow() {
		super("Console", 300, 400);
		System.setOut(new PrintStream(System.out) {
			@Override
			public void println(String x) {
				messages.add(x);
				super.println(x);
			}
		});

		System.out.println("Console initialized");
		System.out.println("Test message");
		System.out.println("Test message 2");
	}

	@Override
	protected void preRender(NkContext ctx, MemoryStack stack) {
		nk_style_push_style_item(ctx,
				ctx.style().window().fixed_background(),
				nk_style_item_color(
						nk_rgba(
								30, 30, 30, 255, NkColor.malloc()
						),
						NkStyleItem.malloc()
				)
		);
	}

	@Override
	protected void render(NkContext ctx, MemoryStack stack) {
		nk_layout_row_dynamic(ctx, height - 60, 1);

		nk_group_begin(ctx, "Console", 0);
		nk_layout_row_dynamic(ctx, 0, 1);
		for (String message : messages) {
			nk_text(ctx, message, NK_TEXT_LEFT);
		}
		nk_group_end(ctx);
	}

	@Override
	protected void postRender(NkContext ctx, MemoryStack stack) {
		nk_style_pop_color(ctx);
	}
}
