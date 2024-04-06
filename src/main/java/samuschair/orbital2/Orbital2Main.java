package samuschair.orbital2;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.nuklear.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.Callback;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Platform;
import samuschair.orbital2.window.GravitySim;
import samuschair.orbital2.window.NumberDisplay;
import samuschair.orbital2.window.NumberEdit;
import samuschair.orbital2.window.Window;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static samuschair.orbital2.util.IOUtil.ioResourceToByteBuffer;

@SuppressWarnings({"resource", "SameParameterValue"})
public class Orbital2Main {

	// region Main setup and constants
	public static final boolean debug;
	public static final int fpsLimit;

	private static final int BUFFER_INITIAL_SIZE = 4 * 1024;

	private static final int MAX_VERTEX_BUFFER = 512 * 1024;
	private static final int MAX_ELEMENT_BUFFER = 128 * 1024;

	public static final NkAllocator ALLOCATOR;

	private static final NkDrawVertexLayoutElement.Buffer VERTEX_LAYOUT;

	static {
		debug = Boolean.parseBoolean(System.getProperty("orbital.debug", "not true"));

		int fps;
		try {
			fps = Integer.parseInt(System.getProperty("orbital.fps", "60"));
		} catch(NumberFormatException e) {
			log("Invalid fps value, defaulting to 60");
			fps = 60;
		}
		fpsLimit = fps;

		if(debug) {
			log("Debug mode enabled");
			log("FPS limit: " + fpsLimit);
			System.setProperty("org.lwjgl.util.Debug", "true");
		}

		Configuration.DEBUG_STREAM.set(System.out);

		ALLOCATOR = NkAllocator.create()
				.alloc((handle, old, size) -> nmemAllocChecked(size))
				.mfree((handle, ptr) -> nmemFree(ptr));

		VERTEX_LAYOUT = NkDrawVertexLayoutElement.create(4)
				.position(0).attribute(NK_VERTEX_POSITION).format(NK_FORMAT_FLOAT).offset(0)
				.position(1).attribute(NK_VERTEX_TEXCOORD).format(NK_FORMAT_FLOAT).offset(8)
				.position(2).attribute(NK_VERTEX_COLOR).format(NK_FORMAT_R8G8B8A8).offset(16)
				.position(3).attribute(NK_VERTEX_ATTRIBUTE_COUNT).format(NK_FORMAT_COUNT).offset(0)
				.flip();
	}
	// endregion

	// region Font loading
	private static final ByteBuffer font;

	static {
		try {
			font = ioResourceToByteBuffer("FiraSans-Regular.ttf", 512 * 1024);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	// endregion

	// region GLFW variables
	private static long windowId; // The identifier of the GLFW window
	private static int width, height; // The pixel dimensions of the GLFW window
	private static int displayWidth, displayHeight; // The pixel dimensions of the content inside the window, this will usually be the same size as the window.
	// endregion

	// region Nuklear variables
	private static final NkContext ctx = NkContext.create();
	private static final NkUserFont defaultFont = NkUserFont.create();
	private static final NkBuffer glDrawCommands = NkBuffer.create();
	private static final NkDrawNullTexture emptyTexture = NkDrawNullTexture.create();
	// endregion

	// region GL variables
	private static int vbo, vao, ebo; // Vertex buffer object, vertex array object, element buffer object
	private static int shaderProgram;
	private static int vertexShader; // vertex shader moves the points of triangles
	private static int fragmentShader; // fragment shader changes the colors of pixels
	private static int uniformTexture;
	private static int uniformProjectionMatrix;
	// endregion

	private static final Map<Window, Vector2i> windows = new LinkedHashMap<>();
	static {
		GravitySim sim = new GravitySim();
		windows.put(sim, new Vector2i(0, 0));
		windows.put(new NumberDisplay(sim), new Vector2i(1000, 0));
		windows.put(new NumberEdit(sim), new Vector2i(1300, 0));
		windows.put(sim.timeControls, new Vector2i(1000, 575));
	}

	public static void main(String[] args) {
		Callback debugProc = glSetup();
		NkContext ctx = setupGlfwToNuklearTranslations(windowId);
		setupFont();

		glfwShowWindow(windowId);
		glfwMaximizeWindow(windowId);
		while(!glfwWindowShouldClose(windowId)) {
			/* Input */
			newFrame();

			for(Entry<Window, Vector2i> entry : windows.entrySet()) {
				entry.getKey().layout(ctx, entry.getValue());
			}

			try(MemoryStack stack = stackPush()) {
				IntBuffer width = stack.mallocInt(1);
				IntBuffer height = stack.mallocInt(1);

				glfwGetWindowSize(windowId, width, height);
				glViewport(0, 0, width.get(0), height.get(0));
			}
			glClear(GL_COLOR_BUFFER_BIT);
			/*
			 * IMPORTANT: `nk_glfw_render` modifies some global OpenGL state
			 * with blending, scissor, face culling, depth test and viewport and
			 * defaults everything back into a default state.
			 * Make sure to either a.) save and restore or b.) reset your own state after
			 * rendering the UI.
			 */
			render(NK_ANTI_ALIASING_ON, MAX_VERTEX_BUFFER, MAX_ELEMENT_BUFFER);
			glfwSwapBuffers(windowId);
		}

		shutdown();

		glfwFreeCallbacks(windowId);
		if(debugProc != null) {
			debugProc.free();
		}
		glfwTerminate();
		Objects.requireNonNull(glfwSetErrorCallback(null)).free();
	}

	private static void setupContext() {
		String NK_SHADER_VERSION = Platform.get() == Platform.MACOSX ? "#version 150\n" : "#version 300 es\n";
		String vert =
				NK_SHADER_VERSION + """
						uniform mat4 ProjMtx;
						in vec2 Position;
						in vec2 TexCoord;
						in vec4 Color;
						out vec2 Frag_UV;
						out vec4 Frag_Color;
						void main() {
							Frag_UV = TexCoord;
							Frag_Color = Color;
							gl_Position = ProjMtx * vec4(Position.xy, 0, 1);
						}
						""";
		String frag =
				NK_SHADER_VERSION + """
						precision mediump float;
						uniform sampler2D Texture;
						in vec2 Frag_UV;
						in vec4 Frag_Color;
						out vec4 Out_Color;
						void main() {
							Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
						}
						""";

		nk_buffer_init(glDrawCommands, ALLOCATOR, BUFFER_INITIAL_SIZE);
		shaderProgram = glCreateProgram();
		vertexShader = glCreateShader(GL_VERTEX_SHADER);
		fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(vertexShader, vert);
		glShaderSource(fragmentShader, frag);
		glCompileShader(vertexShader);
		glCompileShader(fragmentShader);
		if(glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new IllegalStateException("Failed to compile vertex shader");
		}
		if(glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new IllegalStateException("Failed to compile fragment shader");
		}
		glAttachShader(shaderProgram, vertexShader);
		glAttachShader(shaderProgram, fragmentShader);
		glLinkProgram(shaderProgram);
		if(glGetProgrami(shaderProgram, GL_LINK_STATUS) != GL_TRUE) {
			throw new IllegalStateException("Failed to link shader program");
		}

		uniformTexture = glGetUniformLocation(shaderProgram, "Texture");
		uniformProjectionMatrix = glGetUniformLocation(shaderProgram, "ProjMtx");
		int position = glGetAttribLocation(shaderProgram, "Position");
		int uv = glGetAttribLocation(shaderProgram, "TexCoord");
		int color = glGetAttribLocation(shaderProgram, "Color");

		{
			// buffer setup
			vbo = glGenBuffers();
			ebo = glGenBuffers();
			vao = glGenVertexArrays();

			glBindVertexArray(vao);
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

			glEnableVertexAttribArray(position);
			glEnableVertexAttribArray(uv);
			glEnableVertexAttribArray(color);

			glVertexAttribPointer(position, 2, GL_FLOAT, false, 20, 0);
			glVertexAttribPointer(uv, 2, GL_FLOAT, false, 20, 8);
			glVertexAttribPointer(color, 4, GL_UNSIGNED_BYTE, true, 20, 16);
		}

		{
			// null texture setup
			int nullTexID = glGenTextures();

			emptyTexture.texture().id(nullTexID);
			emptyTexture.uv().set(0.5f, 0.5f);

			glBindTexture(GL_TEXTURE_2D, nullTexID);
			try(MemoryStack stack = stackPush()) {
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(0xFFFFFFFF));
			}
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		}

		glBindTexture(GL_TEXTURE_2D, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	private static NkContext setupGlfwToNuklearTranslations(long win) {
		glfwSetScrollCallback(win, (window, xOffset, yOffset) -> {
			try(MemoryStack stack = stackPush()) {
				NkVec2 scroll = NkVec2.malloc(stack)
						.x((float) xOffset)
						.y((float) yOffset);
				nk_input_scroll(ctx, scroll);
			}
		});
		glfwSetCharCallback(win, (window, codepoint) -> nk_input_unicode(ctx, codepoint));
		glfwSetKeyCallback(win, Orbital2Main::translateSpecialKeyPress);
		glfwSetCursorPosCallback(win, (window, xPos, yPos) -> nk_input_motion(ctx, (int) xPos, (int) yPos));
		glfwSetMouseButtonCallback(win, Orbital2Main::translateMouseButtonPress);

		nk_init(ctx, ALLOCATOR, null);
		ctx.clip()
				.copy((handle, text, len) -> {
					if(len == 0) {
						return;
					}

					try(MemoryStack stack = stackPush()) {
						ByteBuffer str = stack.malloc(len + 1);
						memCopy(text, memAddress(str), len);
						str.put(len, (byte) 0);

						glfwSetClipboardString(win, str);
					}
				})
				.paste((handle, edit) -> {
					long text = nglfwGetClipboardString(win);
					if(text != NULL) {
						nnk_textedit_paste(edit, text, nnk_strlen(text));
					}
				});

		setupContext();
		return ctx;
	}

	private static void newFrame() {
		try(MemoryStack stack = stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);

			glfwGetWindowSize(windowId, w, h);
			width = w.get(0);
			height = h.get(0);

			glfwGetFramebufferSize(windowId, w, h);
			displayWidth = w.get(0);
			displayHeight = h.get(0);
		}

		nk_input_begin(ctx);
		glfwPollEvents();

		NkMouse mouse = ctx.input().mouse();
		if(mouse.grab()) {
			glfwSetInputMode(windowId, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
		} else if(mouse.grabbed()) {
			float prevX = mouse.prev().x();
			float prevY = mouse.prev().y();
			glfwSetCursorPos(windowId, prevX, prevY);
			mouse.pos().x(prevX);
			mouse.pos().y(prevY);
		} else if(mouse.ungrab()) {
			glfwSetInputMode(windowId, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
		}

		nk_input_end(ctx);
	}

	private static void render(int antiAliasingEnabled, int maxVertexBuffer, int maxElementBuffer) {
		try(MemoryStack stack = stackPush()) {
			// setup global state
			glEnable(GL_BLEND);
			glBlendEquation(GL_FUNC_ADD);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glDisable(GL_CULL_FACE);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_SCISSOR_TEST);
			glActiveTexture(GL_TEXTURE0);

			// setup program
			glUseProgram(shaderProgram);
			glUniform1i(uniformTexture, 0);
			glUniformMatrix4fv(uniformProjectionMatrix, false, stack.floats(
					2.0f / width, 0.0f, 0.0f, 0.0f,
					0.0f, -2.0f / height, 0.0f, 0.0f,
					0.0f, 0.0f, -1.0f, 0.0f,
					-1.0f, 1.0f, 0.0f, 1.0f
			));
			glViewport(0, 0, displayWidth, displayHeight);
		}

		{
			// convert from command queue into draw list and draw to screen

			// allocate vertex and element buffer
			glBindVertexArray(vao);
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

			glBufferData(GL_ARRAY_BUFFER, maxVertexBuffer, GL_STREAM_DRAW);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, maxElementBuffer, GL_STREAM_DRAW);

			// load draw vertices & elements directly into vertex + element buffer
			ByteBuffer vertices = Objects.requireNonNull(glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, maxVertexBuffer, null));
			ByteBuffer elements = Objects.requireNonNull(glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, maxElementBuffer, null));
			try(MemoryStack stack = stackPush()) {
				// fill convert configuration
				NkConvertConfig config = NkConvertConfig.calloc(stack)
						.vertex_layout(VERTEX_LAYOUT)
						.vertex_size(20)
						.vertex_alignment(4)
						.tex_null(emptyTexture)
						.circle_segment_count(22)
						.curve_segment_count(22)
						.arc_segment_count(22)
						.global_alpha(1.0f)
						.shape_AA(antiAliasingEnabled)
						.line_AA(antiAliasingEnabled);

				// setup buffers to load vertices and elements
				NkBuffer vbuf = NkBuffer.malloc(stack);
				NkBuffer ebuf = NkBuffer.malloc(stack);

				nk_buffer_init_fixed(vbuf, vertices/*, maxVertexBuffer*/);
				nk_buffer_init_fixed(ebuf, elements/*, maxElementBuffer*/);
				nk_convert(ctx, glDrawCommands, vbuf, ebuf, config);
			}
			glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
			glUnmapBuffer(GL_ARRAY_BUFFER);

			// iterate over and execute each draw command
			float fbScaleX = (float) displayWidth / (float) width;
			float fbScaleY = (float) displayHeight / (float) height;

			long offset = NULL;
			for(NkDrawCommand cmd = nk__draw_begin(ctx, glDrawCommands); cmd != null; cmd = nk__draw_next(cmd, glDrawCommands, ctx)) {
				if(cmd.elem_count() == 0) {
					continue;
				}
				glBindTexture(GL_TEXTURE_2D, cmd.texture().id());
				glScissor(
						(int) (cmd.clip_rect().x() * fbScaleX),
						(int) ((height - (int) (cmd.clip_rect().y() + cmd.clip_rect().h())) * fbScaleY),
						(int) (cmd.clip_rect().w() * fbScaleX),
						(int) (cmd.clip_rect().h() * fbScaleY)
				);
				glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);
				offset += cmd.elem_count() * 2L;
			}
			nk_clear(ctx);
			nk_buffer_clear(glDrawCommands);
		}

		// default OpenGL state
		glUseProgram(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
		glDisable(GL_BLEND);
		glDisable(GL_SCISSOR_TEST);
	}

	private static Callback glSetup() {
		GLFWErrorCallback.createPrint().set();
		if(!glfwInit()) {
			throw new IllegalStateException("Unable to initialize glfw");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		if(Platform.get() == Platform.MACOSX) {
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		}

		if(debug) {
			glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
		}

		windowId = glfwCreateWindow(640, 640, "Orbital", NULL, NULL);
		if(windowId == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}

		glfwMakeContextCurrent(windowId);
		glfwSwapInterval(fpsLimit / 60); // Enable v-sync
		GLCapabilities caps = GL.createCapabilities();
		Callback debugProc = GLUtil.setupDebugMessageCallback();

		if(caps.OpenGL43) {
			GL43.glDebugMessageControl(GL43.GL_DEBUG_SOURCE_API, GL43.GL_DEBUG_TYPE_OTHER, GL43.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
		} else if(caps.GL_KHR_debug) {
			KHRDebug.glDebugMessageControl(
					KHRDebug.GL_DEBUG_SOURCE_API,
					KHRDebug.GL_DEBUG_TYPE_OTHER,
					KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION,
					(IntBuffer) null,
					false
			);
		} else if(caps.GL_ARB_debug_output) {
			glDebugMessageControlARB(GL_DEBUG_SOURCE_API_ARB, GL_DEBUG_TYPE_OTHER_ARB, GL_DEBUG_SEVERITY_LOW_ARB, (IntBuffer) null, false);
		}

		return debugProc;
	}

	private static void destroy() {
		glDetachShader(shaderProgram, vertexShader);
		glDetachShader(shaderProgram, fragmentShader);
		glDeleteShader(vertexShader);
		glDeleteShader(fragmentShader);
		glDeleteProgram(shaderProgram);
		glDeleteTextures(defaultFont.texture().id());
		glDeleteTextures(emptyTexture.texture().id());
		glDeleteBuffers(vbo);
		glDeleteBuffers(ebo);
		nk_buffer_free(glDrawCommands);

		GL.setCapabilities(null);
	}

	private static void shutdown() {
		free(ctx.clip()::copy);
		free(ctx.clip()::paste);
		nk_free(ctx);
		destroy();

		free(defaultFont::query);
		free(defaultFont::width);

		free(ALLOCATOR::alloc);
		free(ALLOCATOR::mfree);
	}

	private static void free(Supplier<NativeResource> resource) {
		Objects.requireNonNull(resource.get()).free();
	}

	private static void translateSpecialKeyPress(long window, int key, int scancode, int action, int mods) {
		boolean press = action == GLFW_PRESS || action == GLFW_REPEAT;
		switch(key) {
//			case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
			case GLFW_KEY_DELETE -> nk_input_key(ctx, NK_KEY_DEL, press);
			case GLFW_KEY_ENTER -> nk_input_key(ctx, NK_KEY_ENTER, press);
			case GLFW_KEY_TAB -> nk_input_key(ctx, NK_KEY_TAB, press);
			case GLFW_KEY_BACKSPACE -> nk_input_key(ctx, NK_KEY_BACKSPACE, press);
			case GLFW_KEY_UP -> nk_input_key(ctx, NK_KEY_UP, press);
			case GLFW_KEY_DOWN -> nk_input_key(ctx, NK_KEY_DOWN, press);
			case GLFW_KEY_HOME -> {
				nk_input_key(ctx, NK_KEY_TEXT_START, press);
				nk_input_key(ctx, NK_KEY_SCROLL_START, press);
			}
			case GLFW_KEY_END -> {
				nk_input_key(ctx, NK_KEY_TEXT_END, press);
				nk_input_key(ctx, NK_KEY_SCROLL_END, press);
			}
			case GLFW_KEY_PAGE_DOWN -> nk_input_key(ctx, NK_KEY_SCROLL_DOWN, press);
			case GLFW_KEY_PAGE_UP -> nk_input_key(ctx, NK_KEY_SCROLL_UP, press);
			case GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> nk_input_key(ctx, NK_KEY_SHIFT, press);
			case GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL, GLFW_KEY_LEFT_SUPER, GLFW_KEY_RIGHT_SUPER -> {
				if(press) {
					nk_input_key(ctx, NK_KEY_COPY, glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_PASTE, glfwGetKey(window, GLFW_KEY_V) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_CUT, glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_TEXT_UNDO, glfwGetKey(window, GLFW_KEY_Z) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_TEXT_REDO,
							(glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
									|| glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
									&& glfwGetKey(window, GLFW_KEY_Z) == GLFW_PRESS
					);
					nk_input_key(ctx, NK_KEY_TEXT_WORD_LEFT, glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_TEXT_WORD_RIGHT, glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_TEXT_LINE_START, glfwGetKey(window, GLFW_KEY_B) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_TEXT_LINE_END, glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS);
				} else {
					nk_input_key(ctx, NK_KEY_LEFT, glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_RIGHT, glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS);
					nk_input_key(ctx, NK_KEY_COPY, false);
					nk_input_key(ctx, NK_KEY_PASTE, false);
					nk_input_key(ctx, NK_KEY_CUT, false);
					nk_input_key(ctx, NK_KEY_SHIFT, false);
				}
			}
		}
	}

	private static void translateMouseButtonPress(long window, int button, int action, int mods) {
		try(MemoryStack stack = stackPush()) {
			DoubleBuffer cx = stack.mallocDouble(1);
			DoubleBuffer cy = stack.mallocDouble(1);

			glfwGetCursorPos(window, cx, cy);

			int x = (int) cx.get(0);
			int y = (int) cy.get(0);

			int nkButton = switch(button) {
				case GLFW_MOUSE_BUTTON_RIGHT -> NK_BUTTON_RIGHT;
				case GLFW_MOUSE_BUTTON_MIDDLE -> NK_BUTTON_MIDDLE;
				default -> NK_BUTTON_LEFT;
			};
			nk_input_button(ctx, nkButton, x, y, action == GLFW_PRESS);
		}
	}

	private static void setupFont() {
		final int BITMAP_W = 1024;
		final int BITMAP_H = 1024;

		final int FONT_HEIGHT = 18;
		int fontTexID = glGenTextures();

		STBTTFontinfo fontInfo = STBTTFontinfo.create();
		STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(95);

		float scale;
		float descent;

		try(MemoryStack stack = stackPush()) {
			stbtt_InitFont(fontInfo, font);
			scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);

			IntBuffer d = stack.mallocInt(1);
			stbtt_GetFontVMetrics(fontInfo, null, d, null);
			descent = d.get(0) * scale;

			ByteBuffer bitmap = memAlloc(BITMAP_W * BITMAP_H);

			STBTTPackContext pc = STBTTPackContext.malloc(stack);
			stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
			stbtt_PackSetOversampling(pc, 4, 4);
			stbtt_PackFontRange(pc, font, 0, FONT_HEIGHT, 32, cdata);
			stbtt_PackEnd(pc);

			// Convert R8 to RGBA8
			ByteBuffer texture = memAlloc(BITMAP_W * BITMAP_H * 4);
			for(int i = 0; i < bitmap.capacity(); i++) {
				texture.putInt((bitmap.get(i) << 24) | 0x00FFFFFF);
			}
			texture.flip();

			glBindTexture(GL_TEXTURE_2D, fontTexID);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, texture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			memFree(texture);
			memFree(bitmap);
		}

		defaultFont
				.width((handle, h, text, len) -> {
					float textWidth = 0;
					try(MemoryStack stack = stackPush()) {
						IntBuffer unicode = stack.mallocInt(1);

						int glyphLen = nnk_utf_decode(text, memAddress(unicode), len);
						int textLen = glyphLen;

						if(glyphLen == 0) {
							return 0;
						}

						IntBuffer advance = stack.mallocInt(1);
						while(textLen <= len && glyphLen != 0) {
							if(unicode.get(0) == NK_UTF_INVALID) {
								break;
							}

							/* query currently drawn glyph information */
							stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null);
							textWidth += advance.get(0) * scale;

							/* offset next glyph */
							glyphLen = nnk_utf_decode(text + textLen, memAddress(unicode), len - textLen);
							textLen += glyphLen;
						}
					}
					return textWidth;
				})
				.height(FONT_HEIGHT)
				.query((handle, fontHeight, glyph, codepoint, nextCodepoint) -> {
					try(MemoryStack stack = stackPush()) {
						FloatBuffer x = stack.floats(0.0f);
						FloatBuffer y = stack.floats(0.0f);

						STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
						IntBuffer advance = stack.mallocInt(1);

						stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false);
						stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);

						NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);

						ufg.width(q.x1() - q.x0());
						ufg.height(q.y1() - q.y0());
						ufg.offset().set(q.x0(), q.y0() + (FONT_HEIGHT + descent));
						ufg.xadvance(advance.get(0) * scale);
						ufg.uv(0).set(q.s0(), q.t0());
						ufg.uv(1).set(q.s1(), q.t1());
					}
				})
				.texture(it -> it.id(fontTexID));

		nk_style_set_font(ctx, defaultFont);
	}

	public static void log(Object message) {
		System.out.printf("[Orbital2] %s%n", message);
	}
}
