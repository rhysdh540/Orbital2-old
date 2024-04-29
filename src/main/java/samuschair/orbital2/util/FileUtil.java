package samuschair.orbital2.util;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.Marshaller;
import org.joml.Vector2d;
import samuschair.orbital2.sim.Body;
import samuschair.orbital2.sim.GravitySim;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
	private static final Jankson JANKSON = Jankson.builder()
			.allowBareRootObject()
			.registerSerializer(Body.class, FileUtil::serialize)
			.registerDeserializer(JsonObject.class, Body.class, FileUtil::deserialize)
			.registerTypeFactory(Body.class, () -> Body.builder().build())
			.build();

	private static final JsonGrammar GRAMMAR = JsonGrammar.builder()
			.printTrailingCommas(false)
			.bareSpecialNumerics(true)
			.withComments(true)
			.bareRootObject(true)
			.printUnquotedKeys(true)
			.build();

	private static final Path SAVE_DIR;

	static {
		String property = System.getProperty("orbital.saveDir");
		SAVE_DIR = property == null ? Path.of(System.getProperty("user.home"), ".orbital2") : Path.of(property);
		try {
			Files.createDirectories(SAVE_DIR);
		} catch (Exception e) {
			System.err.println("Failed to create save directory " + SAVE_DIR);
			e.printStackTrace(System.err);
		}
	}

	public static void save(GravitySim sim, String name) {
		Path path = SAVE_DIR.resolve(name + ".grav.json5");
		try(Writer writer = Files.newBufferedWriter(path)) {
			if(!Files.exists(path)) Files.createFile(path);
			JANKSON.toJson(sim).toJson(writer, GRAMMAR, 0);
			writer.flush();
		} catch (Exception e) {
			System.err.println("Failed to save sim to " + path);
			e.printStackTrace(System.err);
		}
	}

	public static GravitySim loadGravitySim(String name) {
		Path path = SAVE_DIR.resolve(name + ".grav.json5");
		String json;
		try {
			json = Files.readString(path);
		} catch (FileNotFoundException e) {
			System.err.println("No save file found at " + path);
			return new GravitySim();
		} catch (Exception e) {
			System.err.println("Failed to load sim from " + path);
			e.printStackTrace(System.err);
			return new GravitySim();
		}

		try {
			return JANKSON.fromJson(json, GravitySim.class);
		} catch (Exception e) {
			System.err.println("Failed to parse sim from " + path);
			e.printStackTrace(System.err);
			return new GravitySim();
		}
	}

	private static JsonElement serialize(Body body, Marshaller marshaller) {
		JsonObject obj = new JsonObject();
		obj.put("mass", new JsonPrimitive(body.mass));
		obj.put("radius", new JsonPrimitive(body.radius));
		obj.put("color", new JsonPrimitive(body.color.getRGB()));
		obj.put("pos", vectorToJson(body.position));
		obj.put("vel", vectorToJson(body.velocity));
		obj.put("acc", vectorToJson(body.acceleration));
		return obj;
	}

	private static Body deserialize(JsonObject obj, Marshaller marshaller) {
		int mass = obj.getInt("mass", 1);
		int radius = obj.getInt("radius", 1);
		int color = obj.getInt("color", -1); // -1 is white
		Vector2d pos = vectorFromJson(obj.get("pos"));
		Vector2d vel = vectorFromJson(obj.get("vel"));
		Vector2d acc = vectorFromJson(obj.get("acc"));
		Body b = Body.builder()
				.withMass(mass)
				.withRadius(radius)
				.withColor(new Color(color))
				.withPosition(pos)
				.build();
		b.velocity.set(vel);
		b.acceleration.set(acc);
		return b;
	}

	private static JsonArray vectorToJson(Vector2d vec) {
		JsonArray arr = new JsonArray();
		arr.add(new JsonPrimitive(vec.x));
		arr.add(new JsonPrimitive(vec.y));
		return arr;
	}

	private static Vector2d vectorFromJson(JsonElement obj) {
		if(obj == null) return new Vector2d();
		if(!(obj instanceof JsonArray arr)) return new Vector2d();
		if(arr.size() != 2) return new Vector2d();
		return new Vector2d(arr.getDouble(0, 0), arr.getDouble(1, 0));
	}
}
