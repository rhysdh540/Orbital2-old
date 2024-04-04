package samuschair.orbital2;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

public class Body {

	@Getter
	@Setter
	private int mass;

	@Getter
	@Setter
	private int radius;

	public final Vector2d position;
	public final Vector2d velocity;
	public final Vector2d acceleration;

	public Body(int mass, int radius) {
		this.mass = mass;
		this.radius = radius;
		this.position = new Vector2d();
		this.velocity = new Vector2d();
		this.acceleration = new Vector2d();
	}

	public void setVelocity(double x, double y) {
		velocity.set(x, y);
	}

	public void setAcceleration(double x, double y) {
		acceleration.set(x, y);
	}

	public void setPos(int x, int y) {
		position.set(x, y);
	}

	public void setPos(double x, double y) {
		position.set(x, y);
	}

	public int getDiameter() {
		return radius * 2;
	}
}
