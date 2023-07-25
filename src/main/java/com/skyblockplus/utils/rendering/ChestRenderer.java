/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.utils.Utils.getEmoji;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class ChestRenderer {

	private static final Map<String, BufferedImage> CACHED_IMAGES = new HashMap<>();
	private static final int CHEST_SCALE = 3;
	public static final int CHEST_ROWS = 6;
	public static final int CHEST_COLUMNS = 9;

	public static BufferedImage renderChest(List<Image> slots) throws IOException {
		int marginTop = 18 * CHEST_SCALE;
		int marginLeft = 8 * CHEST_SCALE;
		int itemSize = 16 * CHEST_SCALE;
		int itemSpacing = itemSize + 2 * CHEST_SCALE;

		BufferedImage chestRender = ImageIO.read(new File("src/main/java/com/skyblockplus/json/chestrenderer/background_image.png"));
		Graphics2D graphics = chestRender.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		for (int row = 0; row < CHEST_ROWS; row++) {
			for (int column = 0; column < CHEST_COLUMNS; column++) {
				int slotIndex = row * CHEST_COLUMNS + column;
				Image image = slots.get(slotIndex);
				if (image != null) {
					int x = marginLeft + column * itemSpacing;
					int y = marginTop + row * itemSpacing;
					graphics.drawImage(slots.get(slotIndex), x, y, itemSize, itemSize, null);
				}
			}
		}

		graphics.dispose();
		return chestRender;
	}

	public static BufferedImage getItemImage(String id) throws IOException {
		if (CACHED_IMAGES.containsKey(id)) {
			return CACHED_IMAGES.get(id);
		}

		BufferedImage image;
		try {
			image = ImageIO.read(new File("src/main/java/com/skyblockplus/json/chestrenderer/imagecache/" + id + ".png"));
		} catch (Exception e) {
			String emojiId = Emoji.fromFormatted(getEmoji(id)).asCustom().getId();
			image = ImageIO.read(new URL("https://cdn.discordapp.com/emojis/" + emojiId + ".png?quality=lossless"));
			File outputfile = new File("src/main/java/com/skyblockplus/json/chestrenderer/imagecache/" + id + ".png");
			ImageIO.write(image, "png", outputfile);
		}

		if (id.equals("STAINED_GLASS_PANE:15") || id.equals("INK_SACK:8") || id.equals("INK_SACK:10")) {
			CACHED_IMAGES.put(id, image);
		}

		return image;
	}
}
