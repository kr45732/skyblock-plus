/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.Constants.ENCHANT_NAMES;
import static com.skyblockplus.utils.Constants.PET_NAMES;
import static com.skyblockplus.utils.Utils.*;

/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
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

public class EmojiUpdater {

	public static JsonObject idToEmoji = new JsonObject();

	public static JsonElement getMissing(String url) {
		Set<String> processedItemsSet = getJson(url).getAsJsonObject().keySet();
		Set<String> allItems = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json")
			.getAsJsonObject()
			.keySet();
		allItems.removeIf(processedItemsSet::contains);
		return gson.toJsonTree(allItems);
	}

	public static JsonObject processAll() {
		try {
			JsonArray sbItems = getSkyblockItemsJson();
			Set<String> allSbItems = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json")
				.getAsJsonObject()
				.keySet();
			JsonObject out = new JsonObject();
			File skyCryptFiles = new File("src/main/java/com/skyblockplus/json/skycrypt_images");
			if (skyCryptFiles.exists()) {
				skyCryptFiles.delete();
			}
			skyCryptFiles.mkdirs();

			// Heads
			for (JsonElement i : sbItems) {
				String id = higherDepth(i, "id").getAsString();
				if (higherDepth(i, "skin") != null && allSbItems.contains(id)) {
					out.addProperty(
						id,
						"https://sky.shiiyu.moe/head/" +
						higherDepth(
							JsonParser.parseString(new String(Base64.getDecoder().decode(higherDepth(i, "skin").getAsString()))),
							"textures.SKIN.url",
							""
						)
							.split("://textures.minecraft.net/texture/")[1]
					);
					allSbItems.remove(id);
				}
			}

			// Gets all images from resource pack
			JsonObject processedImages = processDir(new File("src/main/java/com/skyblockplus/json/cit"));
			for (Map.Entry<String, JsonElement> entry : processedImages.entrySet()) {
				if (allSbItems.contains(entry.getKey())) {
					out.add(entry.getKey(), entry.getValue());
					allSbItems.remove(entry.getKey());
				}
			}

			// Enchants and pets
			File enchantedBook = new File(skyCryptFiles.getPath() + "/ENCHANTED_BOOK_COPY.png");
			ImageIO.write(ImageIO.read(new URL("https://sky.shiiyu.moe/item/ENCHANTED_BOOK")), "png", enchantedBook);
			for (String sbItem : allSbItems) {
				String split = sbItem.split(";")[0];
				if (PET_NAMES.contains(split)) {
					out.addProperty(sbItem, getPetUrl(split));
				} else if (ENCHANT_NAMES.contains(split)) {
					out.addProperty(sbItem, enchantedBook.getPath());
				}
			}
			allSbItems.removeIf(out::has);

			System.out.println("Attempting to get " + allSbItems.size() + " items from SkyCrypt...");
			int count = 0;
			for (String sbItem : allSbItems) {
				try {
					File imgFile;
					File[] imgFiles = skyCryptFiles.listFiles(f -> f.getName().split(".png")[0].equals(sbItem));
					if (imgFiles.length == 0) {
						System.out.println(sbItem);
						imgFile = new File(skyCryptFiles.getPath() + "/" + sbItem + ".png");
						ImageIO.write(ImageIO.read(new URL("https://sky.shiiyu.moe/item/" + sbItem)), "png", imgFile);
					} else {
						if(imgFiles.length > 1) {
							System.out.println(sbItem + " - "  + Arrays.toString(imgFiles));
						}
						imgFile = imgFiles[0];
					}
					out.addProperty(sbItem, imgFile.getPath());
					TimeUnit.MILLISECONDS.sleep(250);
				} catch (Exception ignored) {}

				if (count != 0 && count % 50 == 0) {
					System.out.println("Finished " + count + "/" + allSbItems.size());
				}
				count++;
			}
			System.out.println("Finished processing SkyCrypt items");

			if (!out.has("POTATO_CROWN")) {
				out.addProperty("POTATO_CROWN", "https://sky.shiiyu.moe/item/GOLD_HELMET");
			}

			return out;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getEnchantedItems(){
		try {
			File neuDir = new File("src/main/java/com/skyblockplus/json/neu");
			if (neuDir.exists()) {
				FileUtils.deleteDirectory(neuDir);
			}
			neuDir.mkdir();

			Git
					.cloneRepository()
					.setURI("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO.git")
					.setDirectory(neuDir)
					.call();

			List<String> enchantedItems = Arrays.stream(new File(neuDir.getPath() + "/items").listFiles(f -> {
				try {
					JsonElement json = JsonParser.parseReader(new FileReader(f));
					return !higherDepth(json ,"itemid").getAsString().equals("minecraft:skull") && higherDepth(json, "nbttag").getAsString().startsWith("{ench:[");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			})).map(f -> f.getName().split(".json")[0]).collect(Collectors.toList());

			FileUtils.deleteDirectory(neuDir);

			return enchantedItems;
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public static JsonObject processDir(File dir) {
		JsonObject out = new JsonObject();
		try {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					if (!file.getName().equals("model")) {
						JsonObject o = processDir(file);
						for (Map.Entry<String, JsonElement> e : o.entrySet()) {
							out.add(e.getKey(), e.getValue());
						}
					}
				} else {
					if (file.getName().endsWith(".properties")) {
						Properties appProps = new Properties();
						try {
							appProps.load(new FileInputStream(file));
						} catch (Exception e) {
							continue;
						}

						Object id = appProps.getOrDefault("nbt.ExtraAttributes.id", null);
						if (id == null) {
							continue;
						}

						String sId = ((String) id);
						String altId = null;
						File textureFile = Arrays
							.stream(file.getParentFile().listFiles())
							.filter(file1 -> file1.getName().equals(file.getName().replace(".properties", ".png")))
							.findFirst()
							.orElse(null);
						if (sId.equals("ipattern:*STONE_BLADE")) {
							sId = "STONE_BLADE";
						} else if (sId.equals("pattern:*STARRED_STONE_BLADE")) {
							sId = "STARRED_STONE_BLADE";
						} else if (sId.equals("ipattern:*LAST_BREATH")) {
							sId = "LAST_BREATH";
						} else if (sId.equals("GENERALS_HOPE_OF_THE_RESISTANCE")) {
							textureFile = new File(file.getParentFile() + "/staff_of_the_rising_sun.png");
						} else if (sId.equals("SPIDER_QUEENS_STINGER")) {
							textureFile = new File(file.getParentFile() + "/spider_queen_stinger.png");
						} else if (sId.equals("STARRED_SPIDER_QUEENS_STINGER")) {
							textureFile = new File(file.getParentFile() + "/starred_spider_queen_stinger.png");
						} else if (sId.equals("STARRED_VENOMS_TOUCH")) {
							textureFile = new File(file.getParentFile() + "/starred_venom_touch.png");
						} else if (sId.equals("GOD_POTION")) {
							textureFile = new File(file.getParentFile() + "/god_potion.png");
						} else if (sId.equals("REFUND_COOKIE") || sId.equals("FREE_COOKIE")) {
							textureFile = new File(file.getParentFile() + "/booster_cookie.png");
						} else if (sId.startsWith("regex:")) {
							String sIdTemp = sId.split("regex:")[1];
							sId = sIdTemp.replaceAll("\\(\\?:.*\\)\\?", "");
							altId = sIdTemp.replaceAll("\\(\\?:(.*)\\)\\?", "$1");
						}
						if (textureFile == null) {
							continue;
						}

						if (getInternalJsonMappings().keySet().contains(sId)) {
							BufferedImage image = ImageIO.read(textureFile);
							if (image.getWidth() != 16) {
								throw new IllegalArgumentException("Image width is not 16 pixels: " + file.getPath());
							}
							if (image.getHeight() != 16) {
								int rows = image.getHeight() / 16;
								int subImageHeight = image.getHeight() / rows;
								BufferedImage[] images = new BufferedImage[rows];
								for (int i = 0; i < rows; i++) {
									images[i] = new BufferedImage(128, 128, image.getType());
									Graphics2D imgCreator = images[i].createGraphics();
									int srcFirstY = subImageHeight * i;
									int dstCornerY = subImageHeight * i + subImageHeight;
									imgCreator.setRenderingHint(
										RenderingHints.KEY_INTERPOLATION,
										RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
									);
									imgCreator.drawImage(image, 0, 0, 128, 128, 0, srcFirstY, image.getWidth(), dstCornerY, null);
									imgCreator.dispose();
								}
								File gifFile = new File(textureFile.getParentFile().getPath() + "/" + sId + "_gif.gif");
								if (gifFile.exists()) {
									gifFile.delete();
								}
								ImageIO.write(images[images.length - 1], "gif", gifFile);
								BufferedImage firstImage = images[0];
								try (ImageOutputStream output = new FileImageOutputStream(gifFile)) {
									GifWriter writer = new GifWriter(output, firstImage.getType(), 1, true);
									writer.writeToSequence(firstImage);
									for (int i = 1; i < images.length - 1; i++) {
										writer.writeToSequence(images[i]);
									}
									writer.close();
								}
								out.addProperty(sId, gifFile.getPath());
								if (altId != null) {
									out.addProperty(altId, gifFile.getPath());
								}
							} else {
								File scaledFile = new File(textureFile.getParentFile().getPath() + "/" + sId + "_scaled.png");
								if (scaledFile.exists()) {
									scaledFile.delete();
								}
								BufferedImage scaledImage = new BufferedImage(128, 128, image.getType());
								Graphics2D graphics2D = scaledImage.createGraphics();
								graphics2D.setRenderingHint(
									RenderingHints.KEY_INTERPOLATION,
									RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
								);
								graphics2D.drawImage(image, 0, 0, 128, 128, null);
								graphics2D.dispose();
								ImageIO.write(scaledImage, "png", scaledFile);
								out.addProperty(sId, scaledFile.getPath());
								if (altId != null) {
									out.addProperty(altId, scaledFile.getPath());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}

	public static void runEmojis(String parsedItemsUrl) {
		idToEmoji = new JsonObject();
		String last = "";
		try {
			JsonObject allParsedItems = getJson(parsedItemsUrl).getAsJsonObject();
			Set<String> added = JsonParser
				.parseReader(new FileReader("src/main/java/com/skyblockplus/json/IdToEmojiMappings.json"))
				.getAsJsonObject()
				.keySet();
			JsonObject allItems = new JsonObject();
			for (Map.Entry<String, JsonElement> entry : allParsedItems.entrySet()) {
				if (!added.contains(entry.getKey())) {
					allItems.add(entry.getKey(), entry.getValue());
				}
			}

			List<Guild> guildList = jda
				.getGuilds()
				.stream()
				.filter(g -> {
					try {
						return Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1]) > 0;
					} catch (Exception e) {
						return false;
					}
				})
				.filter(g -> g.getEmotes().size() < g.getMaxEmotes())
				.sorted(Comparator.comparingInt(g -> Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1])))
				.collect(Collectors.toList());
			int guildCount = 0;

			for (Map.Entry<String, JsonElement> entry : allItems.entrySet()) {
				try {
					last = entry.toString();
					String name = idToName(entry.getKey())
						.toLowerCase()
						.replace("⚚ ", "starred ")
						.replace(" ", "_")
						.replace("'", "")
						.replace("-", "_");

					Guild curGuild = guildList.get(guildCount);
					if (curGuild.getEmotes().size() >= curGuild.getMaxEmotes()) {
						guildCount++;
						curGuild = guildList.get(guildCount);
						TimeUnit.SECONDS.sleep(5);
						System.out.println("Switched to " + curGuild.getName());
					}

					String urlOrPath = entry.getValue().getAsString();
					Emote emoji;
					if (urlOrPath.startsWith("src/main/java/com/skyblockplus/json")) {
						emoji = curGuild.createEmote(name, Icon.from(new File(urlOrPath))).complete();
					} else {
						URLConnection urlConn = new URL(urlOrPath).openConnection();
						urlConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
						emoji = curGuild.createEmote(name, Icon.from(urlConn.getInputStream())).complete();
					}

					System.out.println("Created emoji - " + last);
					idToEmoji.addProperty(entry.getKey(), emoji.getAsMention());
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (Exception e) {
					System.out.println("Failed - " + last);
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			System.out.println("Failed - " + last);
			e.printStackTrace();
		}
	}
}
