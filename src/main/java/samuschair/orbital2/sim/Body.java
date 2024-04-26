package samuschair.orbital2.sim;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;
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

	public Body(int mass, int radius, Color color, double x, double y) {
		this.mass = mass;
		this.radius = radius;
		this.position = new Vector2dFixed().set(x, y);
		this.velocity = new Vector2dFixed();
		this.acceleration = new Vector2dFixed();
		this.originalPosition = new Vector2dFixed().set(x, y);
		this.color = color;
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

	/**
	 * Moves this body based on its velocity and acceleration.
	 *
	 * @param dt the time step
	 */
	public void move(double dt) {
		velocity.add(acceleration.x * dt, acceleration.y * dt);
		position.add(velocity.x * dt, velocity.y * dt);
	}

	/**
	 * Sets the velocity of this body to orbit another body.
	 *
	 * @param other the body to orbit
	 * @param G     the gravitational constant
	 */
	public void orbit(Body other, double G) {
		double velocity = Math.sqrt(G * other.mass / distance(other));
		//get the angle 90 degrees to the right of the line between the two bodies
		double angle = Math.atan2(other.position.y - position.y, other.position.x - position.x) - Math.PI / 2;
		this.velocity.set(velocity * Math.cos(angle), velocity * Math.sin(angle));
	}

	public double distance(Body other) {
		return position.distance(other.position);
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

	@With
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	@AllArgsConstructor(access = AccessLevel.PRIVATE) // for @With
	public static final class BodyBuilder {
		private int mass;
		private int radius;
		private Color color;
		private double x, y;

		public BodyBuilder withColor(int r, int g, int b) {
			return this.withColor(new Color(r, g, b));
		}

		public BodyBuilder withPosition(double x, double y) {
			return this.withX(x).withY(y);
		}

		public BodyBuilder withPosition(Vector2dc pos) {
			return this.withX(pos.x()).withY(pos.y());
		}

		public Body build() {
			return new Body(mass, radius, color, x, y);
		}
	}

	/**
	 * {@link Vector2d} with a fixed {@link #toString()} method.
	 */
	private static final class Vector2dFixed extends Vector2d {
		public Vector2dFixed() {
			super();
		}

		@Override
		public String toString() {
			return "(" + MathUtil.round(x, 3) + ", " + MathUtil.round(y, 3) + ")";
		}
	}
}
