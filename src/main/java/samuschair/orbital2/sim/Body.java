package samuschair.orbital2.sim;

import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.lwjgl.nuklear.NkColorf;
import samuschair.orbital2.util.MathUtil;

import java.awt.*;

public class Body {

	public int mass;
	public int radius;

	public final Vector2d position;
	public final Vector2d velocity;
	public final Vector2d acceleration;
	private final Vector2dc originalPosition;

	public final Color color;

	public Body(int mass, int radius, int r, int g, int b, int x, int y) {
		this.mass = mass;
		this.radius = radius;
		this.position = new Vector2dFixed().set(x, y);
		this.velocity = new Vector2dFixed();
		this.acceleration = new Vector2dFixed();
		this.originalPosition = new Vector2dFixed().set(x, y);
		this.color = new Color(r, g, b);
	}

	public void reset() {
		position.set(originalPosition);
		velocity.set(0, 0);
		acceleration.set(0, 0);
	}

	public int getDiameter() {
		return radius * 2;
	}

	public double kineticEnergy() {
		return 0.5 * mass * velocity.lengthSquared();
	}

	public void move(double dt) {
		velocity.add(acceleration.x * dt, acceleration.y * dt);
		position.add(velocity.x * dt, velocity.y * dt);
	}

	private static final class Vector2dFixed extends Vector2d {
		public Vector2dFixed() {
			super();
		}

		@Override
		public String toString() {
			return "(" + MathUtil.round(x, 3) + ", " + MathUtil.round(y, 3) + ")";
		}
	}

	public NkColorf colorAsNkColorf() {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		return NkColorf.malloc().set((float) r / 255, (float) g / 255, (float) b / 255, 1);
	}

	public static BodyBuilder builder() {
		return new BodyBuilder();
	}

	public static final class BodyBuilder {
		private int mass;
		private int radius;
		private int r, g, b;
		private int x, y;

		private BodyBuilder() {
		}

		public BodyBuilder withMass(int mass) {
			this.mass = mass;
			return this;
		}

		public BodyBuilder withRadius(int radius) {
			this.radius = radius;
			return this;
		}

		public BodyBuilder withR(int r) {
			this.r = r;
			return this;
		}

		public BodyBuilder withG(int g) {
			this.g = g;
			return this;
		}

		public BodyBuilder withB(int b) {
			this.b = b;
			return this;
		}

		public BodyBuilder withColor(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
			return this;
		}

		public BodyBuilder withPosition(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public BodyBuilder withColor(Color color) {
			this.r = color.getRed();
			this.g = color.getGreen();
			this.b = color.getBlue();
			return this;
		}

		public Body build() {
			return new Body(mass, radius, r, g, b, x, y);
		}
	}
}
