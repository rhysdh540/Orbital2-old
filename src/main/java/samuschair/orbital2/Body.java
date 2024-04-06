package samuschair.orbital2;

import org.joml.Vector2d;
import org.lwjgl.nuklear.NkColorf;
import samuschair.orbital2.util.MathUtil;

public class Body {

	public int mass;

	public int radius;

	public final Vector2d position;
	public final Vector2d velocity;
	public final Vector2d acceleration;

	public final NkColorf color;

	public Body(int mass, int radius, int r, int g, int b) {
		this.mass = mass;
		this.radius = radius;
		this.position = new Vector2dFixed();
		this.velocity = new Vector2dFixed();
		this.acceleration = new Vector2dFixed();
		this.color = NkColorf.create()
				.r((float) r / 255)
				.g((float) g / 255)
				.b((float) b / 255)
				.a(1);
	}

	public int getDiameter() {
		return radius * 2;
	}

	public double kineticEnergy() {
		return 0.5 * mass * velocity.lengthSquared();
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
}
