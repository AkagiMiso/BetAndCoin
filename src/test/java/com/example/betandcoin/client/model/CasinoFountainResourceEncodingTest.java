package com.example.betandcoin.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import software.bernie.geckolib3.geo.raw.pojo.Converter;
import software.bernie.geckolib3.geo.raw.tree.RawBoneGroup;
import software.bernie.geckolib3.geo.raw.tree.RawGeometryTree;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** GeckoLib 3 reads geometry and animations with Charset.defaultCharset(). */
public class CasinoFountainResourceEncodingTest {
    private static final String ROOT = "/assets/bet_and_coin/";
    private static final String[] GEOMETRIES = {
            "casino_fountain", "casino_fountain_base_middle",
            "casino_fountain_base_upper", "casino_fountain_water_lower",
            "casino_fountain_water_upper", "casino_fountain_water_fall"
    };

    @Test
    public void fountainResourcesParseIdenticallyAcrossClientEncodings() throws IOException {
        for (String geometry : GEOMETRIES) {
            assertPortable("geo/" + geometry + ".geo.json");
        }
        assertPortable("animations/casino_fountain.animation.json");
    }

    @Test
    public void gbkClientRetainsBothWaterfallBonesAndAllFourCurtains() throws IOException {
        // Exercise GeckoLib's actual name-keyed hierarchy, where GBK decoding
        // previously collapsed the two UTF-8 names and replaced the upper bone.
        String decoded = new String(read("geo/casino_fountain_water_fall.geo.json"),
                Charset.forName("GBK"));
        RawGeometryTree hierarchy = RawGeometryTree.parseHierarchy(Converter.fromJsonString(decoded));
        Set<String> names = hierarchy.topLevelBones.keySet();
        for (RawBoneGroup bone : hierarchy.topLevelBones.values()) {
            assertEquals(4, bone.selfBone.getCubes().length);
        }
        assertEquals(2, names.size());
        assertTrue(names.contains("\u6c34\u5e18\u4e0a"));
        assertTrue(names.contains("\u6c34\u5e18\u4e0b"));
    }

    @Test
    public void animationAndParentReferencesStillResolveToMainModelBones() throws IOException {
        Set<String> names = new HashSet<>();
        JsonObject geometry = parse(read("geo/casino_fountain.geo.json"),
                StandardCharsets.UTF_8).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            assertTrue(names.add(element.getAsJsonObject().get("name").getAsString()));
        }
        for (JsonElement element : geometry.getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            if (bone.has("parent")) {
                assertTrue(names.contains(bone.get("parent").getAsString()));
            }
        }
        JsonObject animations = parse(read("animations/casino_fountain.animation.json"),
                StandardCharsets.UTF_8).getAsJsonObject().getAsJsonObject("animations");
        for (java.util.Map.Entry<String, JsonElement> animation : animations.entrySet()) {
            for (java.util.Map.Entry<String, JsonElement> bone : animation.getValue()
                    .getAsJsonObject().getAsJsonObject("bones").entrySet()) {
                assertTrue("Unknown animated bone: " + bone.getKey(), names.contains(bone.getKey()));
            }
        }
    }

    private static void assertPortable(String path) throws IOException {
        byte[] bytes = read(path);
        for (byte value : bytes) {
            assertTrue("Use JSON Unicode escapes in " + path, value >= 0);
        }
        JsonElement expected = parse(bytes, StandardCharsets.UTF_8);
        for (String encoding : new String[] {"GBK", "windows-1252"}) {
            assertEquals(path + " decoded with " + encoding,
                    expected, parse(bytes, Charset.forName(encoding)));
        }
    }

    private static JsonElement parse(byte[] bytes, Charset charset) {
        return new JsonParser().parse(new String(bytes, charset));
    }

    private static byte[] read(String path) throws IOException {
        try (InputStream stream = CasinoFountainResourceEncodingTest.class
                .getResourceAsStream(ROOT + path)) {
            assertNotNull("Missing resource: " + path, stream);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = stream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toByteArray();
        }
    }
}
