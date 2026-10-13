package com.example.betandcoin.game.roulette;

import com.google.gson.*;
import net.minecraft.client.renderer.model.BlockModel;
import org.junit.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class RouletteResourceTest {
    private static final String ROOT = "/assets/bet_and_coin/";
    private JsonObject json(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(ROOT + name)) {
            assertNotNull(name, input);
            return new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test public void segmentTransformsAndColorsMatchActualWheelUv() throws Exception {
        BufferedImage texture = ImageIO.read(getClass().getResourceAsStream(ROOT + "textures/block/roulette_table_main.png"));
        JsonObject wheel = json("models/block/roulette_table_main.json").getAsJsonArray("elements").get(18).getAsJsonObject();
        JsonArray uv = wheel.getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv");
        int[] rgb = {0x690a0a, 0x180d0d, 0x286311};
        for (int i = 0; i < 68; i++) {
            RouletteLayout.Segment s = RouletteLayout.segment(i);
            double u = uv.get(0).getAsDouble() + (s.x + 0.25 - 2) / 12 * (uv.get(2).getAsDouble() - uv.get(0).getAsDouble());
            double v = uv.get(1).getAsDouble() + (s.z + 0.25 - 8) / 12 * (uv.get(3).getAsDouble() - uv.get(1).getAsDouble());
            assertEquals("segment " + i, rgb[s.color.ordinal()],
                    texture.getRGB((int) (u / 16 * texture.getWidth()), (int) (v / 16 * texture.getHeight())) & 0xffffff);
        }
    }

    @Test public void modelsParseInVanillaAndOnlyCenterChangesBetweenFrames() throws Exception {
        JsonObject main = json("models/block/roulette_table_main.json");
        JsonObject frame = json("models/block/roulette_table_main_45.json");
        for (int i = 0; i < 21; i++) assertEquals(main.getAsJsonArray("elements").get(i), frame.getAsJsonArray("elements").get(i));
        for (int i = 21; i < 24; i++) assertEquals(45,
                frame.getAsJsonArray("elements").get(i).getAsJsonObject().getAsJsonObject("rotation").get("angle").getAsInt());
        for (String name : new String[] {"roulette_table_main", "roulette_table_main_45", "roulette_table_glow",
                "roulette_table_glow_red", "roulette_table_glow_black", "roulette_table_glow_green"}) {
            JsonObject model = json("models/block/" + name + ".json");
            assertNotNull(BlockModel.fromString(model.toString()));
            for (java.util.Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                String path = texture.getValue().getAsString().replace("bet_and_coin:", "textures/") + ".png";
                try (InputStream in = getClass().getResourceAsStream(ROOT + path)) { assertNotNull(path, in); }
            }
        }
        assertEquals(1, json("models/block/roulette_table_glow.json").getAsJsonArray("elements").size());
    }

    @Test public void everyBettingSurfacePixelMatchesAuthoredColorsAndExcludesWhiteBorders() throws Exception {
        BufferedImage texture = ImageIO.read(getClass().getResourceAsStream(ROOT + "textures/block/roulette_table_main.png"));
        JsonObject tabletop = json("models/block/roulette_table_main.json").getAsJsonArray("elements").get(1).getAsJsonObject();
        JsonArray uv = tabletop.getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv");
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            int rgb = texture.getRGB(x, y);
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            RouletteRules.Color expected = null;
            if (r > g * 2 && r > b * 2) expected = RouletteRules.Color.RED;
            else if (g > r * 2 && g > b * 2) expected = RouletteRules.Color.GREEN;
            else if (r < 32 && g < 32 && b < 32) expected = RouletteRules.Color.BLACK;
            double u = (x + 0.5) / texture.getWidth() * 16;
            double v = (y + 0.5) / texture.getHeight() * 16;
            double localX = (u - uv.get(0).getAsDouble()) / (uv.get(2).getAsDouble() - uv.get(0).getAsDouble()) * 16;
            double localZ = -8 + (v - uv.get(1).getAsDouble()) / (uv.get(3).getAsDouble() - uv.get(1).getAsDouble()) * 16;
            assertEquals("table pixel " + x + "," + y, expected, RouletteGeometry.bettingZone(localX, localZ));
        }
    }

    @Test public void coloredGlowTexturesContainTheAuthoredVisiblePixel() throws Exception {
        JsonArray uv = json("models/block/roulette_table_glow.json").getAsJsonArray("elements").get(0)
                .getAsJsonObject().getAsJsonObject("faces").getAsJsonObject("up").getAsJsonArray("uv");
        int[] colors = new int[3];
        int i = 0;
        for (String name : new String[] {"red", "black", "green"}) {
            BufferedImage texture = ImageIO.read(getClass().getResourceAsStream(ROOT + "textures/block/roulette_table_glow_" + name + ".png"));
            double u = (uv.get(0).getAsDouble() + uv.get(2).getAsDouble()) / 2;
            double v = (uv.get(1).getAsDouble() + uv.get(3).getAsDouble()) / 2;
            int rgb = texture.getRGB((int) (u / 16 * texture.getWidth()), (int) (v / 16 * texture.getHeight()));
            assertTrue("visible " + name, (rgb >>> 24) != 0);
            colors[i++] = rgb;
        }
        assertNotEquals(colors[0], colors[1]); assertNotEquals(colors[1], colors[2]);
    }
}
