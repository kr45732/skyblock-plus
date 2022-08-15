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

package com.skyblockplus.utils.rendering;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

public class LoreRenderer {

	private static final Map<String, BufferedImage> fontCache = Arrays
		.stream(new File("src/main/java/com/skyblockplus/utils/rendering/font").listFiles())
		.collect(
			Collectors.toMap(
				File::getName,
				file -> {
					try {
						return ImageIO.read(file);
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}
			)
		);
	private static final String ASCII_ALPHABET =
		"ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000";
	private static final int GLYPH_SIZE = 8;
	private static final Map<Character, Glyph> glyphCache = new ConcurrentHashMap<>();

	public static BufferedImage renderLore(List<String> loreLines) {
		int scale = 2;
		int margin = 10;
		MinecraftColors defaultColor = MinecraftColors.DARK_PURPLE;

		List<List<String>> formattedLines = loreLines
			.stream()
			.map(line -> {
				List<String> parts = Arrays.stream(line.split("§")).collect(Collectors.toCollection(ArrayList::new));
				parts.set(0, "r" + parts.get(0));
				return parts;
			})
			.toList();

		int width = formattedLines
			.stream()
			.map(line -> {
				int x = 0;
				boolean bold = false;

				for (String part : line) {
					char colorCode = part.charAt(0);
					String text = part.substring(1);

					switch (Character.toLowerCase(colorCode)) {
						case 'l' -> bold = true;
						case 'r' -> bold = false;
						default -> {}
					}

					for (char ch : text.toCharArray()) {
						x += getGlyph(ch).getWidth() * scale;
						if (bold) {
							x += scale;
						}
					}
				}
				return x;
			})
			.max(Integer::compare)
			.get();
		int height = formattedLines.size() * (GLYPH_SIZE * scale + 5);

		BufferedImage image = new BufferedImage(width + margin * 2, height + margin * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(16, 1, 16));
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		int y = margin;
		for (List<String> line : formattedLines) {
			int x = margin;
			boolean bold = false;
			graphics.setColor(defaultColor.awtColor);

			for (String part : line) {
				char colorCode = part.charAt(0);
				String text = part.substring(1);

				switch (Character.toLowerCase(colorCode)) {
					case 'l' -> bold = true;
					case 'r' -> {
						bold = false;
						graphics.setColor(defaultColor.awtColor);
					}
					default -> {
						MinecraftColors color = MinecraftColors.byColorCode(colorCode);
						if (color != null) {
							graphics.setColor(color.awtColor);
						}
					}
				}

				for (char ch : text.toCharArray()) {
					Glyph glpyh = getGlyph(ch);
					if (bold) {
						glpyh.renderOnto(graphics, x + scale, y, scale);
					}
					glpyh.renderOnto(graphics, x, y, scale);
					x += glpyh.getWidth() * scale;
					if (bold) {
						x += scale;
					}
				}
			}
			y += GLYPH_SIZE * scale + 5;
		}

		return image;
	}

	private static Glyph getGlyph(char char1) {
		return glyphCache.computeIfAbsent(char1, LoreRenderer::getGlyph0);
	}

	private static Glyph getGlyph0(char char1) {
		if (char1 == ' ') {
			return new SpaceGlyph();
		}

		int funkyIndex = ASCII_ALPHABET.indexOf(char1);
		if (funkyIndex != -1) {
			BufferedImage fontImage;
			if (fontCache.containsKey("ascii.png")) {
				fontImage = fontCache.get("ascii.png");
			} else {
				return new MissingGlyph();
			}

			return createGlyph(fontImage, funkyIndex);
		}

		int lastByte = char1 & 0xFF;
		int page = char1 >> 8 & 0xFF;
		String pageName = "unicode_page_" + String.format("%02x", page);

		BufferedImage fontImage;
		if (fontCache.containsKey(pageName + ".png")) {
			fontImage = fontCache.get(pageName + ".png");
		} else {
			return new MissingGlyph();
		}

		return createGlyph(fontImage, lastByte);
	}

	private static RealGlyph createGlyph(BufferedImage fontImage, int index) {
		int xOffset = (index % 16) * (fontImage.getWidth() / 16);
		int yOffset = (index / 16) * (fontImage.getHeight() / 16);
		int scale = fontImage.getWidth() / 16 / GLYPH_SIZE;
		return new RealGlyph(fontImage, xOffset, yOffset, findGlyphWidth(fontImage, xOffset, yOffset, scale), scale);
	}

	private static int findGlyphWidth(BufferedImage fontImage, int xOffset, int yOffset, int scale) {
		for (int i = scale * GLYPH_SIZE - 1; i >= 0; i--) {
			int finalI = i;
			if (
				IntStream.range(0, scale * GLYPH_SIZE).anyMatch(it -> (fontImage.getRGB(xOffset + finalI, yOffset + it) >> 24 & 0xFF) != 0)
			) {
				return (int) Math.ceil(((double) i + 2) / scale);
			}
		}
		return GLYPH_SIZE;
	}

	private enum MinecraftColors {
		DARK_RED(0xAA0000, '4'),
		RED(0xFF5555, 'c'),
		GOLD(0xFFAA00, '6'),
		YELLOW(0xFFFF55, 'e'),
		DARK_GREEN(0x00AA00, '2'),
		GREEN(0x55FF55, 'a'),
		AQUA(0x55FFFF, 'b'),
		DARK_AQUA(0x00AAAA, '3'),
		DARK_BLUE(0x0000AA, '1'),
		BLUE(0x5555FF, '9'),
		LIGHT_PURPLE(0xFF55FF, 'd'),
		DARK_PURPLE(0xAA00AA, '5'),
		WHITE(0xFFFFFF, 'f'),
		GRAY(0xAAAAAA, '7'),
		DARK_GRAY(0x555555, '8'),
		BLACK(0x000000, '0');

		private final Color awtColor;
		private final char colorCode;

		MinecraftColors(int rgb, char colorCode) {
			this.awtColor = new Color(rgb, false);
			this.colorCode = colorCode;
		}

		public static MinecraftColors byColorCode(char code) {
			return Arrays.stream(values()).filter(value -> value.colorCode == code).findFirst().orElse(null);
		}
	}

	private interface Glyph {
		void renderOnto(Graphics2D graphics, int xPos, int yPos, int scale);

		int getWidth();
	}

	private static class SpaceGlyph implements Glyph {

		@Override
		public void renderOnto(Graphics2D graphics, int xPos, int yPos, int scale) {}

		@Override
		public int getWidth() {
			return 4;
		}
	}

	private static class MissingGlyph implements Glyph {

		@Override
		public void renderOnto(Graphics2D graphics, int xPos, int yPos, int scale) {
			graphics.drawRect(xPos, yPos, GLYPH_SIZE * scale, GLYPH_SIZE * scale);
		}

		@Override
		public int getWidth() {
			return GLYPH_SIZE;
		}
	}

	private static class RealGlyph implements Glyph {

		private final int width;
		private final int scale;
		private final BufferedImage thinTexture;

		public RealGlyph(BufferedImage texture, int xOffset, int yOffset, int width, int scale) {
			this.width = width;
			this.scale = scale;
			this.thinTexture = texture.getSubimage(xOffset, yOffset, GLYPH_SIZE * scale, GLYPH_SIZE * scale);
		}

		@Override
		public void renderOnto(Graphics2D graphics, int xPos, int yPos, int scale) {
			int actualScale = scale / this.scale;
			for (int x = 0; x < GLYPH_SIZE * this.scale; x++) {
				for (int y = 0; y < GLYPH_SIZE * this.scale; y++) {
					boolean isSet = (thinTexture.getRGB(x, y) >> 24 & 0xFF) != 0x00;
					if (isSet) {
						graphics.fillRect(xPos + x * actualScale, yPos + y * actualScale, actualScale, actualScale);
					}
				}
			}
		}

		@Override
		public int getWidth() {
			return width;
		}
	}
}
