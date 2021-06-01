package com.skyblockplus.api.linkedaccounts;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discord/linkedAccounts")
public class LinkedAccountController {

	private final LinkedAccountService settingsService;

	@Autowired
	public LinkedAccountController(LinkedAccountService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/get/all")
	public List<LinkedAccountModel> getAllServerSettings() {
		return settingsService.getAllLinkedAccounts();
	}

	@GetMapping("/get/by/discordId")
	public ResponseEntity<?> getByDiscordId(@RequestParam(value = "discordId") String discordId) {
		return settingsService.getByDiscordId(discordId);
	}

	@GetMapping("/get/by/minecraftUuid")
	public ResponseEntity<?> getByMinecraftUuid(@RequestParam(value = "minecraftUuid") String minecraftUuid) {
		return settingsService.getByMinecraftUuid(minecraftUuid);
	}

	@GetMapping("/get/by/minecraftUsername")
	public ResponseEntity<?> getByMinecraftUsername(@RequestParam(value = "minecraftUsername") String minecraftUsername) {
		return settingsService.getByMinecraftUsername(minecraftUsername);
	}

	@PostMapping("/add")
	public ResponseEntity<HttpStatus> addNewLinkedAccount(@RequestBody LinkedAccountModel linkedAccountModel) {
		return settingsService.addNewLinkedAccount(linkedAccountModel);
	}
}
