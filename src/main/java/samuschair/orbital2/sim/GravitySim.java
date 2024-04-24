package samuschair.orbital2.sim;

import samuschair.orbital2.util.MathUtil;

import java.util.LinkedHashSet;
import java.util.Set;

public class GravitySim {
	public final Set<Body> bodies;
	public final double G;

	private static final int TARGET_ITERATIONS_PER_FRAME = 250_000;

	public GravitySim(Set<Body> bodies, double G) {
		this.bodies = bodies;
		this.G = G;
	}

	public GravitySim(double G) { this(new LinkedHashSet<>(), G); }
	public GravitySim() { this(6.67430e-11); }

	public void addBody(Body body) { bodies.add(body); }

	public void update(double dt) {
		double timesToRun = TARGET_ITERATIONS_PER_FRAME / dt; // as dt goes up, we run fewer times
		timesToRun /= MathUtil.log2(bodies.size()); // as bodies go up, we run fewer times - based on the number of digits in the number of bodies

		double amountToMove = dt / timesToRun;
		for(int i = 0; i < timesToRun; i++) {
			updateOnce(amountToMove);
		}
	}

	private void updateOnce(double amountToMove) {
		for(Body body : bodies) {
			body.acceleration.set(0, 0);
		}

		for(Body body : bodies) {
			for(Body other : bodies) {
				if(body == other) continue;
				double distance = other.position.distance(body.position);
				double force = G * body.mass * other.mass / (distance * distance);
				double angle = Math.atan2(other.position.y - body.position.y, other.position.x - body.position.x);
				body.acceleration.add(force * Math.cos(angle) / body.mass, force * Math.sin(angle) / body.mass);
			}
		}

		for(Body body : bodies) {
			body.move(amountToMove);
		}
	}

	// KE = 1/2mv^2
	public double getKineticEnergy() {
		double sum = 0;
		for(Body body : bodies) {
			sum += body.kineticEnergy();
		}
		return sum;
	}

	// PE = -G m1m2/r
	public double getPotentialEnergy() {
		double sum = 0;
		for(Body body : bodies) {
			for(Body other : bodies) {
				if(body == other) continue;
				sum += -G * body.mass * other.mass / other.position.distance(body.position);
			}
		}
		return sum / 2;
	}

	public void reset() {
		bodies.forEach(Body::reset);
	}
}
