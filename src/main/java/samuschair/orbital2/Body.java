package samuschair.orbital2;

import org.joml.Vector2d;
import org.lwjgl.nuklear.NkColorf;

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
		this.position = new Vector2d();
		this.velocity = new Vector2d();
		this.acceleration = new Vector2d();
		this.color = NkColorf.create()
				.r((float) r / 255)
				.g((float) g / 255)
				.b((float) b / 255)
				.a(1);
	}

	public void setVelocity(double x, double y) {
		velocity.set(x, y);
	}

	public void setAcceleration(double x, double y) {
		acceleration.set(x, y);
	}

	public int getDiameter() {
		return radius * 2;
	}
}
