package samuschair.orbital2;

import org.lwjgl.nuklear.NkVec2;

public class Circle {

	// from top left corner of the square
	public int x, y;
	public final NkVec2 velocity;

	public final int diameter;
	public final int radius;

	public Circle(int x, int y, int diameter) {
		this.x = x;
		this.y = y;
		this.velocity = NkVec2.create();
		this.diameter = diameter;
		this.radius = diameter / 2;
	}

	public Circle() {
		this(0, 0, 40);
	}

	public void setPos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setVelocity(float x, float y) {
		velocity.x(x);
		velocity.y(y);
	}

	public int cx() {
		return x + radius;
	}

	public int cy() {
		return y + radius;
	}

	public float vx() {
		return velocity.x();
	}

	public float vy() {
		return velocity.y();
	}

	public void vx(float vx) {
		velocity.x(vx);
	}

	public void vy(float vy) {
		velocity.y(vy);
	}
}
