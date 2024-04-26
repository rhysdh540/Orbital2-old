package samuschair.orbital2;

import samuschair.orbital2.sim.Body;
import samuschair.orbital2.sim.GravitySim;
import samuschair.orbital2.util.FileUtil;
import java.util.Iterator;

public class Test {
	public static void main(String[] args) {

	}

	private static void testSerialization() {
		GravitySim sim = new GravitySim();
		Body earth = Body.builder() // Earth
				.withMass(100)
				.withRadius(10)
				.withColor(35, 87, 219)
				.withPosition(900, 500)
				.build();

		Body sun = Body.builder() // Sun
				.withMass(10000)
				.withRadius(50)
				.withColor(255, 255, 240)
				.withPosition(500, 500)
				.build();

		earth.orbit(sun, sim.G);
		sim.addBody(earth);
		sim.addBody(sun);

		for(int i = 0; i < 10000; i++) {
			sim.update(1000);
		}
		FileUtil.save(sim, "test");
		GravitySim loaded = FileUtil.loadGravitySim("test");
		Iterator<Body> it1 = loaded.bodies.iterator();
		Iterator<Body> it2 = sim.bodies.iterator();
		while(it1.hasNext() && it2.hasNext()) {
			Body b1 = it1.next();
			Body b2 = it2.next();
			if(b1.mass != b2.mass) throw new IllegalStateException("Mass mismatch");
			if(b1.radius != b2.radius) throw new IllegalStateException("Radius mismatch");
			if(!b1.color.equals(b2.color)) throw new IllegalStateException("Color mismatch");
			if(b1.position.distance(b2.position) > 0.00) throw new IllegalStateException("Position mismatch");
		}
		if(it1.hasNext() || it2.hasNext()) throw new IllegalStateException("Body count mismatch");
	}
}
