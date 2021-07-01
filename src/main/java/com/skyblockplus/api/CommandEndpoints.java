package com.skyblockplus.api;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.api.templates.ErrorTemplate;
import com.skyblockplus.api.templates.Template;
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
		System.out.println("/api/public/essence/information?name=" + name);

		String preFormattedItem = name;
		preFormattedItem = convertToInternalName(preFormattedItem);

		if (higherDepth(getEssenceCostsJson(), preFormattedItem) == null) {
			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(getEssenceCostsJson()));
			preFormattedItem = closestMatch != null ? closestMatch : preFormattedItem;
		}

		JsonElement itemJson = higherDepth(getEssenceCostsJson(), preFormattedItem);
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
	}
}
