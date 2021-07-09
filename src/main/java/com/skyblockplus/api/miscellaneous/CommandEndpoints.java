package com.skyblockplus.api.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.templates.ErrorTemplate;
import com.skyblockplus.api.templates.Template;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/public")
public class CommandEndpoints {

	@GetMapping("/essence/information")
	public ResponseEntity<?> getEssenceInformation(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			System.out.println("/api/public/essence/information?name=" + name);

			JsonElement essenceJson = getEssenceCostsJson();
			if (essenceJson == null) {
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = name;
			preFormattedItem = convertToInternalName(preFormattedItem);

			if (higherDepth(essenceJson, preFormattedItem) == null) {
				String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(essenceJson));
				preFormattedItem = closestMatch != null ? closestMatch : preFormattedItem;
			}

			JsonElement itemJson = higherDepth(essenceJson, preFormattedItem);
			if (itemJson != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", convertFromInternalName(preFormattedItem));
				for (String level : getJsonKeys(itemJson)) {
					template.addData(level, higherDepth(itemJson, level));
				}

				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/bin")
	public ResponseEntity<?> getLowestBin(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			System.out.println("/api/public/bin?name=" + name);

			JsonElement lowestBinJson = getLowestBinJson();
			if (lowestBinJson == null) {
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = convertToInternalName(name);

			if (higherDepth(lowestBinJson, preFormattedItem) != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", convertFromInternalName(preFormattedItem));
				template.addData("price", higherDepth(lowestBinJson, preFormattedItem).getAsLong());
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			JsonElement enchantsJson = higherDepth(getEnchantsJson(), "enchants_min_level");

			List<String> enchantNames = enchantsJson
				.getAsJsonObject()
				.keySet()
				.stream()
				.map(String::toUpperCase)
				.collect(Collectors.toCollection(ArrayList::new));
			enchantNames.add("ULTIMATE_JERRY");

			Map<String, String> rarityMap = new HashMap<>();
			rarityMap.put("LEGENDARY", ";4");
			rarityMap.put("EPIC", ";3");
			rarityMap.put("RARE", ";2");
			rarityMap.put("UNCOMMON", ";1");
			rarityMap.put("COMMON", ";0");

			String formattedName;
			for (String i : enchantNames) {
				if (preFormattedItem.contains(i)) {
					try {
						int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
						String enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
						formattedName = i + ";" + enchantLevel;
						Template template = new Template(true);
						template.addData("id", enchantName);
						template.addData("name", convertFromInternalName(enchantName));
						template.addData("price", higherDepth(lowestBinJson, formattedName).getAsLong());
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			JsonElement petJson = getPetNumsJson();
			List<String> petNames = getJsonKeys(petJson);
			for (String i : petNames) {
				if (preFormattedItem.contains(i)) {
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : rarityMap.entrySet()) {
						if (preFormattedItem.contains(j.getKey())) {
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = getJsonKeys(higherDepth(petJson, formattedName));
						for (String j : petRarities) {
							if (higherDepth(lowestBinJson, formattedName + rarityMap.get(j)) != null) {
								formattedName += rarityMap.get(j);
								break;
							}
						}
					}

					try {
						Template template = new Template(true);
						template.addData("id", formattedName);
						template.addData("name", convertFromInternalName(formattedName));
						template.addData("price", higherDepth(lowestBinJson, formattedName).getAsLong());
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(lowestBinJson));

			if (closestMatch != null && higherDepth(lowestBinJson, closestMatch) != null) {
				Template template = new Template(true);
				template.addData("id", closestMatch);
				template.addData("name", convertFromInternalName(closestMatch));
				template.addData("price", higherDepth(lowestBinJson, closestMatch).getAsLong());
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	/*
	@GetMapping("/average")
	public ResponseEntity<?> getAverageAuction(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			System.out.println("/api/public/average?name=" + name);

			JsonElement averageAhJson = getAverageAuctionJson();
			if (averageAhJson == null) {
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String internalName = convertToInternalName(name);

			if (higherDepth(averageAhJson, internalName) != null) {
				Template template = new Template(true);
				template.addData("id", internalName);
				template.addData("name", convertFromInternalName(internalName));

				JsonElement itemJson = higherDepth(averageAhJson, internalName);
				if (higherDepth(itemJson, "clean_price") != null) {
					template.addData("price", higherDepth(itemJson, "clean_price").getAsLong());
				} else {
					template.addData("price", higherDepth(itemJson, "price").getAsLong());
				}

				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			JsonElement enchantsJson = higherDepth(getEnchantsJson(), "enchants_min_level");

			List<String> enchantNames = enchantsJson
					.getAsJsonObject()
					.keySet()
					.stream()
					.map(String::toUpperCase)
					.collect(Collectors.toCollection(ArrayList::new));
			enchantNames.add("ULTIMATE_JERRY");

			Map<String, String> rarityMap = new HashMap<>();
			rarityMap.put("LEGENDARY", ";4");
			rarityMap.put("EPIC", ";3");
			rarityMap.put("RARE", ";2");
			rarityMap.put("UNCOMMON", ";1");
			rarityMap.put("COMMON", ";0");


			String formattedName;
			for (String i : enchantNames) {
				if (internalName.contains(i)) {
					String enchantName;
					try {
						int enchantLevel = Integer.parseInt(internalName.replaceAll("\\D+", ""));
						enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
						formattedName = i + ";" + enchantLevel;

						JsonElement itemJson = higherDepth(averageAhJson, formattedName);
						EmbedBuilder eb;

						if (higherDepth(itemJson, "clean_price") != null) {
							eb = defaultEmbed("Average auction (clean)");
							eb.addField(capitalizeString(enchantName), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
						} else {
							eb = defaultEmbed("Average auction");
							eb.addField(capitalizeString(enchantName), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
						}

						eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
						return eb;
					} catch (Exception ignored) {
					}
				}
			}

			JsonElement petJson = getPetNumsJson();
			List<String> petNames = getJsonKeys(petJson);
			for (String i : petNames) {
				if (internalName.contains(i)) {
					String petName = "";
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : rarityMap.entrySet()) {
						if (internalName.contains(j.getKey())) {
							petName = j.getKey().toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = higherDepth(petJson, formattedName)
								.getAsJsonObject()
								.entrySet()
								.stream()
								.map(j -> j.getKey().toUpperCase())
								.collect(Collectors.toCollection(ArrayList::new));

						for (String j : petRarities) {
							if (higherDepth(averageAhJson, formattedName + rarityMap.get(j)) != null) {
								petName = j.toLowerCase() + " " + formattedName.toLowerCase().replace("_", " ");
								formattedName += rarityMap.get(j);
								break;
							}
						}
					}
					JsonElement itemJson = higherDepth(averageAhJson, formattedName);
					EmbedBuilder eb;

					if (higherDepth(itemJson, "clean_price") != null) {
						eb = defaultEmbed("Average auction (clean)");
						eb.addField(capitalizeString(petName + " pet"), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
					} else {
						eb = defaultEmbed("Average auction");
						eb.addField(capitalizeString(petName + " pet"), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
					}

					eb.setThumbnail(getPetUrl(formattedName.split(";")[0]));
					return eb;
				}
			}

			String closestMatch = getClosestMatch(internalName, getJsonKeys(averageAhJson));

			if (closestMatch != null && higherDepth(averageAhJson, closestMatch) != null) {
				EmbedBuilder eb = defaultEmbed("Average Auction");
				JsonElement itemJson = higherDepth(averageAhJson, closestMatch);

				if (enchantNames.contains(closestMatch.split(";")[0].trim())) {
					String itemName = closestMatch.toLowerCase().replace("_", " ").replace(";", " ");
					if (higherDepth(itemJson, "clean_price") != null) {
						eb = defaultEmbed("Average auction (clean)");
						eb.addField(capitalizeString(itemName), formatNumber(higherDepth(itemJson, "clean_price").getAsLong()), false);
					} else {
						eb = defaultEmbed("Average auction");
						eb.addField(capitalizeString(itemName), formatNumber(higherDepth(itemJson, "price").getAsLong()), false);
					}

					eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
				} else if (petNames.contains(closestMatch.split(";")[0].trim())) {
					Map<String, String> rarityMapRev = new HashMap<>();
					rarityMapRev.put("4", "LEGENDARY");
					rarityMapRev.put("3", "EPIC");
					rarityMapRev.put("2", "RARE");
					rarityMapRev.put("1", "UNCOMMON");
					rarityMapRev.put("0", "COMMON");
					eb.setThumbnail(getPetUrl(closestMatch.split(";")[0]));
					String[] itemS = closestMatch.toLowerCase().replace("_", " ").split(";");

					if (higherDepth(itemJson, "clean_price") != null) {
						eb = defaultEmbed("Average auction (clean)");
						eb.addField(
								capitalizeString(rarityMapRev.get(itemS[1].toUpperCase()) + " " + itemS[0]),
								formatNumber(higherDepth(itemJson, "clean_price").getAsLong()),
								false
						);
					} else {
						eb = defaultEmbed("Average auction");
						eb.addField(
								capitalizeString(rarityMapRev.get(itemS[1].toUpperCase()) + " " + itemS[0]),
								formatNumber(higherDepth(itemJson, "price").getAsLong()),
								false
						);
					}
				} else {
					if (higherDepth(itemJson, "clean_price") != null) {
						eb = defaultEmbed("Average auction (clean)");
						eb.addField(
								capitalizeString(closestMatch.toLowerCase().replace("_", " ")),
								formatNumber(higherDepth(itemJson, "clean_price").getAsLong()),
								false
						);
					} else {
						eb = defaultEmbed("Average auction");
						eb.addField(
								capitalizeString(closestMatch.toLowerCase().replace("_", " ")),
								formatNumber(higherDepth(itemJson, "price").getAsLong()),
								false
						);
					}
					eb.setThumbnail("https://sky.lea.moe/item.gif/" + closestMatch);
				}

				return eb;
			}

			return defaultEmbed("No auctions found for " + capitalizeString(item.toLowerCase()));



			JsonElement lowestBinJson = getLowestBinJson();
			if (lowestBinJson == null) {
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = convertToInternalName(name);

			if (higherDepth(lowestBinJson, preFormattedItem) != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", convertFromInternalName(preFormattedItem));
				template.addData("price", higherDepth(lowestBinJson, preFormattedItem).getAsLong());
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			JsonElement enchantsJson = higherDepth(getEnchantsJson(), "enchants_min_level");

			List<String> enchantNames = enchantsJson
					.getAsJsonObject()
					.keySet()
					.stream()
					.map(String::toUpperCase)
					.collect(Collectors.toCollection(ArrayList::new));
			enchantNames.add("ULTIMATE_JERRY");

			Map<String, String> rarityMap = new HashMap<>();
			rarityMap.put("LEGENDARY", ";4");
			rarityMap.put("EPIC", ";3");
			rarityMap.put("RARE", ";2");
			rarityMap.put("UNCOMMON", ";1");
			rarityMap.put("COMMON", ";0");

			String formattedName;
			for (String i : enchantNames) {
				if (preFormattedItem.contains(i)) {
					try {
						int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
						String enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
						formattedName = i + ";" + enchantLevel;
						Template template = new Template(true);
						template.addData("id", enchantName);
						template.addData("name", convertFromInternalName(enchantName));
						template.addData("price", higherDepth(lowestBinJson, formattedName).getAsLong());
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {
					}
				}
			}

			JsonElement petJson = getPetNumsJson();
			List<String> petNames = getJsonKeys(petJson);
			for (String i : petNames) {
				if (preFormattedItem.contains(i)) {
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : rarityMap.entrySet()) {
						if (preFormattedItem.contains(j.getKey())) {
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = getJsonKeys(higherDepth(petJson, formattedName));
						for (String j : petRarities) {
							if (higherDepth(lowestBinJson, formattedName + rarityMap.get(j)) != null) {
								formattedName += rarityMap.get(j);
								break;
							}
						}
					}

					try {
						Template template = new Template(true);
						template.addData("id", formattedName);
						template.addData("name", convertFromInternalName(formattedName));
						template.addData("price", higherDepth(lowestBinJson, formattedName).getAsLong());
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {
					}
				}
			}

			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(lowestBinJson));

			if (closestMatch != null && higherDepth(lowestBinJson, closestMatch) != null) {
				Template template = new Template(true);
				template.addData("id", closestMatch);
				template.addData("name", convertFromInternalName(closestMatch));
				template.addData("price", higherDepth(lowestBinJson, closestMatch).getAsLong());
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		}catch (Exception e){
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	*/
}
