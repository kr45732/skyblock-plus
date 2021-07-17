package com.skyblockplus.api.miscellaneous;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.templates.ErrorTemplate;
import com.skyblockplus.api.templates.Template;
import com.skyblockplus.utils.Constants;
import java.util.List;
import java.util.Map;
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
				String closestMatch = getClosestMatch(preFormattedItem, essenceItemNames);
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
			for (String i : petNames) {
				if (preFormattedItem.contains(i)) {
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : Constants.rarityToNumberMap.entrySet()) {
						if (preFormattedItem.contains(j.getKey())) {
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = getJsonKeys(higherDepth(petJson, formattedName));
						for (String j : petRarities) {
							if (higherDepth(lowestBinJson, formattedName + Constants.rarityToNumberMap.get(j)) != null) {
								formattedName += Constants.rarityToNumberMap.get(j);
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

	@GetMapping("/average")
	public ResponseEntity<?> getAverageAuction(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			System.out.println("/api/public/average?name=" + name);

			JsonElement avgAhJson = getAverageAuctionJson();
			if (avgAhJson == null) {
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = convertToInternalName(name);

			if (higherDepth(avgAhJson, preFormattedItem) != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", convertFromInternalName(preFormattedItem));
				template.addData("price", getAvgPrice(higherDepth(avgAhJson, preFormattedItem)));
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

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
						template.addData("price", getAvgPrice(higherDepth(avgAhJson, formattedName)));
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			JsonElement petJson = getPetNumsJson();
			for (String i : petNames) {
				if (preFormattedItem.contains(i)) {
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : rarityToNumberMap.entrySet()) {
						if (preFormattedItem.contains(j.getKey())) {
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = getJsonKeys(higherDepth(petJson, formattedName));
						for (String j : petRarities) {
							if (higherDepth(avgAhJson, formattedName + rarityToNumberMap.get(j)) != null) {
								formattedName += rarityToNumberMap.get(j);
								break;
							}
						}
					}

					try {
						Template template = new Template(true);
						template.addData("id", formattedName);
						template.addData("name", convertFromInternalName(formattedName));
						template.addData("price", getAvgPrice(higherDepth(avgAhJson, formattedName)));
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(avgAhJson));

			if (closestMatch != null && higherDepth(avgAhJson, closestMatch) != null) {
				Template template = new Template(true);
				template.addData("id", closestMatch);
				template.addData("name", convertFromInternalName(closestMatch));
				template.addData("price", getAvgPrice(higherDepth(avgAhJson, closestMatch)));
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private long getAvgPrice(JsonElement itemJson) {
		return higherDepth(itemJson, "clean_price") != null
			? higherDepth(itemJson, "clean_price").getAsLong()
			: higherDepth(itemJson, "clean").getAsLong();
	}
}
