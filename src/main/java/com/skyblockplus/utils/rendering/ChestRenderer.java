/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils.rendering;

import static com.skyblockplus.utils.utils.HttpUtils.getJson;
import static com.skyblockplus.utils.utils.HttpUtils.getJsonObject;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.Utils.getEmoji;
import static com.skyblockplus.utils.utils.Utils.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.utils.Utils;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.Getter;

public class ChestRenderer {

	public static final int CHEST_ROWS = 6;
	public static final int CHEST_COLUMNS = 9;

	public static BufferedImage renderChest(List<Image> slots, int scale) throws IOException {
		int marginTop = 18 * scale;
		int marginLeft = 8 * scale;
		int itemSize = 16 * scale;
		int itemSpacing = itemSize + 2 * scale;

		BufferedImage chestRender = getBackgroundImage(scale);
		Graphics2D graphics = chestRender.createGraphics();

		for (int row = 0; row < CHEST_ROWS; row++) {
			for (int column = 0; column < CHEST_COLUMNS; column++) {
				int slotIndex = row * CHEST_COLUMNS + column;
				Image image = slots.get(slotIndex);
				if (image != null) {
					int x = marginLeft + column * itemSpacing;
					int y = marginTop + row * itemSpacing;
					graphics.drawImage(slots.get(slotIndex), x, y, null);
				}
			}
		}

		graphics.dispose();
		return chestRender;
	}

	public static void main1(String[] args) throws IOException {
		Utils.initialize();

		JsonElement musJson = getJson("https://hst.sh/raw/quwijeyadu.json");
		Set<String> donated = new HashSet<>(
			higherDepth(musJson, "members.fb3d96498a5b4d5b91b763db14b195ad.items").getAsJsonObject().keySet()
		);

		JsonObject obj = getJsonObject(
			"https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/prerelease/constants/parents.json"
		);
		obj.add("ASPECT_OF_THE_END", gson.toJsonTree(List.of("ASPECT_OF_THE_VOID")));
		obj.add("REVENANT_SWORD", gson.toJsonTree(List.of("REAPER_SWORD", "AXE_OF_THE_SHREDDED")));
		obj.add("ZOMBIE_SWORD", gson.toJsonTree(List.of("ORNATE_ZOMBIE_SWORD", "FLORID_ZOMBIE_SWORD")));
		obj.add("LEAPING_SWORD", gson.toJsonTree(List.of("SILK_EDGE_SWORD")));
		obj.add("FIREDUST_DAGGER", gson.toJsonTree(List.of("BURSTFIRE_DAGGER", "HEARTFIRE_DAGGER")));
		obj.add("MAWDUST_DAGGER", gson.toJsonTree(List.of("BURSTMAW_DAGGER", "HEARTMAW_DAGGER")));
		obj.add("ZOMBIE", gson.toJsonTree(List.of("REVENANT", "REAPER")));
		obj.add("CHEAP_TUXEDO", gson.toJsonTree(List.of("FANCY_TUXEDO", "ELEGANT_TUXEDO")));
		obj.add("SPONGE", gson.toJsonTree(List.of("SHARK_SCALE")));
		obj.add("BRONZE_HUNTER", gson.toJsonTree(List.of("SILVER_HUNTER", "GOLD_HUNTER", "DIAMOND_HUNTER")));
		obj.add("MAGMA", gson.toJsonTree(List.of("VANQUISHED")));
		obj.add("MELON", gson.toJsonTree(List.of("CROPIE", "SQUASH", "FERMENTO")));
		obj.add("VAMPIRE_MASK", gson.toJsonTree(List.of("WITCH_MASK", "VAMPIRE_WITCH_MASK")));
		obj.add("FARMER_BOOTS", gson.toJsonTree(List.of("RANCHERS_BOOTS")));
		obj.add("JUNGLE_AXE", gson.toJsonTree(List.of("TREECAPITATOR_AXE")));
		obj.add("RADIANT_POWER_ORB", gson.toJsonTree(List.of("MANA_FLUX_POWER_ORB", "OVERFLUX_POWER_ORB", "PLASMAFLUX_POWER_ORB")));
		obj.add("ZOMBIE_HEART", gson.toJsonTree(List.of("CRYSTALLIZED_HEART", "REVIVED_HEART")));
		obj.add("WAND_OF_HEALING", gson.toJsonTree(List.of("WAND_OF_MENDING", "WAND_OF_RESTORATION", "WAND_OF_ATONEMENT")));
		obj.add("MITHRIL_DRILL_1", gson.toJsonTree(List.of("MITHRIL_DRILL_2")));
		obj.add("GEMSTONE_DRILL_1", gson.toJsonTree(List.of("GEMSTONE_DRILL_2", "GEMSTONE_DRILL_3", "GEMSTONE_DRILL_4")));
		obj.add("TITANIUM_DRILL_1", gson.toJsonTree(List.of("TITANIUM_DRILL_2", "TITANIUM_DRILL_3", "TITANIUM_DRILL_4")));
		obj.add("MAGMA_ROD", gson.toJsonTree(List.of("INFERNO_ROD", "HELLFIRE_ROD")));
		obj.add("THEORETICAL_HOE_WHEAT_1", gson.toJsonTree(List.of("THEORETICAL_HOE_WHEAT_2", "THEORETICAL_HOE_WHEAT_3")));
		obj.add("THEORETICAL_HOE_CARROT_1", gson.toJsonTree(List.of("THEORETICAL_HOE_CARROT_2", "THEORETICAL_HOE_CARROT_3")));
		obj.add("THEORETICAL_HOE_POTATO_1", gson.toJsonTree(List.of("THEORETICAL_HOE_POTATO_2", "THEORETICAL_HOE_POTATO_3")));
		obj.add("THEORETICAL_HOE_WARTS_1", gson.toJsonTree(List.of("THEORETICAL_HOE_WARTS_2", "THEORETICAL_HOE_WARTS_3")));
		obj.add("THEORETICAL_HOE_CANE_1", gson.toJsonTree(List.of("THEORETICAL_HOE_CANE_2", "THEORETICAL_HOE_CANE_3")));
		obj.add("MELON_DICER", gson.toJsonTree(List.of("MELON_DICER_2", "MELON_DICER_3")));
		obj.add("PUMPKIN_DICER", gson.toJsonTree(List.of("PUMPKIN_DICER_2", "PUMPKIN_DICER_3")));

		for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
			List<String> valueList = new ArrayList<>(
				streamJsonArray(entry.getValue().getAsJsonArray()).map(JsonElement::getAsString).toList()
			);
			for (int i = valueList.size() - 1; i >= 0; i--) {
				if (donated.contains(valueList.get(i))) {
					donated.add(entry.getKey());
					donated.addAll(new ArrayList<>(valueList.subList(0, i)));
					break;
				}
			}
		}

		int scale = 3;
		int itemSize = 16 * scale;

		BufferedImage glass = getItemImage("STAINED_GLASS_PANE:15", itemSize);
		BufferedImage dye = getItemImage("INK_SACK:8", itemSize);
		BufferedImage dSword = getItemImage("DIAMOND_SWORD", itemSize);
		BufferedImage arrow = getItemImage("ARROW", itemSize);
		BufferedImage barrier = getItemImage("BARRIER", itemSize);

		JsonArray weaponsCata = getMuseumCategoriesJson().getAsJsonArray("weapons");

		int maxPage = (int) Math.ceil(weaponsCata.size() / 28.0);
		for (int page = 0; page < maxPage; page++) {
			List<Image> slots = new ArrayList<>();
			int idx = page * 28;

			for (int j = 0; j < CHEST_ROWS; j++) {
				for (int i = 0; i < CHEST_COLUMNS; i++) {
					BufferedImage image;
					if (i == 4 && j == 0) {
						image = dSword; // Weapons page icon
					} else if (j == 5 && ((page > 0 && i == 0) || (i == 3) || (page < maxPage - 1 && i == 8))) {
						image = arrow; // Left, go back, right arrows
					} else if (i == 4 && j == 5) {
						image = barrier; // Close
					} else if (i == 0 || i == CHEST_COLUMNS - 1 || j == 0 || j == CHEST_ROWS - 1) {
						image = glass; // Boundary
					} else {
						if (idx < weaponsCata.size()) {
							String itemId = weaponsCata.get(idx).getAsString();
							if (donated.contains(itemId)) {
								image = getItemImage(itemId, itemSize);
							} else {
								image = dye;
							}
						} else {
							image = null;
						}
						idx++;
					}

					slots.add(image);
				}
			}

			BufferedImage chestRender = renderChest(slots, scale);
			File outputfile = new File("src/main/java/com/skyblockplus/json/renders/page" + page + ".png");
			ImageIO.write(chestRender, "png", outputfile);
		}
	}

	public static BufferedImage getItemImage(String id, int size) throws IOException {
		try {
			return ImageIO.read(new File("src/main/java/com/skyblockplus/json/imagecache/" + id + ".png"));
		} catch (Exception ignored) {}

		String[] lol = getEmoji(id).split(":");
		String disId = lol[lol.length - 1].replace(">", "");
		BufferedImage img = ImageIO.read(new URL("https://cdn.discordapp.com/emojis/" + disId + ".png?size=" + size + "&quality=lossless"));

		File outputfile = new File("src/main/java/com/skyblockplus/json/imagecache/" + id + ".png");
		ImageIO.write(img, "png", outputfile);
		return img;
	}

	private static BufferedImage resizeImage(BufferedImage image, int width, int height) {
		BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scaledImage.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(image, 0, 0, width, height, null);
		graphics.dispose();
		return scaledImage;
	}
}
