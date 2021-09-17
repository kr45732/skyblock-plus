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

package com.skyblockplus.api.linkedaccounts;

import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class LinkedAccountService {

	private final LinkedAccountRepository settingsRepository;

	@Autowired
	public LinkedAccountService(LinkedAccountRepository settingsRepository) {
		this.settingsRepository = settingsRepository;
	}

	public List<LinkedAccountModel> getAllLinkedAccounts() {
		return settingsRepository.findAll();
	}

	public ResponseEntity<?> getByDiscordId(String discordId) {
		if (accountByDiscordIdExists(discordId)) {
			return new ResponseEntity<>(settingsRepository.findByDiscordId(discordId), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	private boolean accountByDiscordIdExists(String discordId) {
		return settingsRepository.findByDiscordId(discordId) != null;
	}

	public ResponseEntity<?> getByMinecraftUuid(String minecraftUuid) {
		if (accountByMinecraftUuidExists(minecraftUuid)) {
			return new ResponseEntity<>(settingsRepository.findByMinecraftUuid(minecraftUuid), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	private boolean accountByMinecraftUuidExists(String minecraftUuid) {
		return settingsRepository.findByMinecraftUuid(minecraftUuid) != null;
	}

	public ResponseEntity<?> getByMinecraftUsername(String minecraftUsername) {
		if (accountByMinecraftUsernameExists(minecraftUsername)) {
			return new ResponseEntity<>(settingsRepository.findByMinecraftUsername(minecraftUsername), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	private boolean accountByMinecraftUsernameExists(String minecraftUsername) {
		return (settingsRepository.findByMinecraftUsername(minecraftUsername) != null);
	}

	public void deleteByMinecraftUsername(String minecraftUsername) {
		settingsRepository.deleteByMinecraftUsername(minecraftUsername);
	}

	public void deleteByDiscordId(String discordId) {
		settingsRepository.deleteByDiscordId(discordId);
	}

	public void deleteByMinecraftUuid(String minecraftUuid) {
		settingsRepository.deleteByMinecraftUuid(minecraftUuid);
	}

	public ResponseEntity<HttpStatus> addNewLinkedAccount(LinkedAccountModel linkedAccountModel) {
		if (settingsRepository.existsByMinecraftUsername(linkedAccountModel.getMinecraftUsername())) {
			deleteByMinecraftUsername(linkedAccountModel.getMinecraftUsername());
		} else if (settingsRepository.existsByMinecraftUuid(linkedAccountModel.getMinecraftUuid())) {
			deleteByMinecraftUuid(linkedAccountModel.getMinecraftUuid());
		} else if (settingsRepository.existsByDiscordId(linkedAccountModel.getDiscordId())) {
			deleteByDiscordId(linkedAccountModel.getDiscordId());
		}

		settingsRepository.save(linkedAccountModel);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
