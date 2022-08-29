package ru.r2cloud.model;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import ru.r2cloud.satellite.SatelliteDao;

public class SatelliteTest {

	@Test
	public void testIncompatibleJradio() {
		List<Satellite> result = loadFromConfig("satellites-incompatible.json");
		assertEquals(1, result.size());
		assertEquals(1, result.get(0).getTransmitters().size());
	}

	private static List<Satellite> loadFromConfig(String metaLocation) {
		List<Satellite> result = new ArrayList<>();
		JsonArray rawSatellites;
		try (Reader r = new InputStreamReader(SatelliteDao.class.getClassLoader().getResourceAsStream(metaLocation))) {
			rawSatellites = Json.parse(r).asArray();
		} catch (Exception e) {
			return Collections.emptyList();
		}
		for (int i = 0; i < rawSatellites.size(); i++) {
			Satellite cur = Satellite.fromJson(rawSatellites.get(i).asObject());
			cur.setSource(SatelliteSource.CONFIG);
			result.add(cur);
		}
		return result;
	}
}
