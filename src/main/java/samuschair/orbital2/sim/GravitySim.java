package samuschair.orbital2.sim;

import java.util.LinkedHashSet;
import java.util.Set;

public class GravitySim {
	public final Set<Body> bodies;
	public final double G;

	// the amount of times to run the simulation per update
	private static final int TIMESCALE_PRECISION = 100;

	public GravitySim(Set<Body> bodies, double G) {
		this.bodies = bodies;
		this.G = G;
	}

	public GravitySim(double G) { this(new LinkedHashSet<>(), G); }
	public GravitySim() { this(6.67430e-11); }

	public void addBody(Body body) { bodies.add(body); }
	public void removeBody(Body body) { bodies.remove(body); }

	public void update(double dt) {
		for(int i = 0; i < TIMESCALE_PRECISION; i++) {
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
				body.move(dt / TIMESCALE_PRECISION);
			}
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
