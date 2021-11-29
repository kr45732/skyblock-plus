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
		LinkedAccountModel linkedAccount = settingsRepository.findByDiscordId(discordId);
		if (linkedAccount != null) {
			return new ResponseEntity<>(linkedAccount, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getByMinecraftUuid(String minecraftUuid) {
		LinkedAccountModel linkedAccount = settingsRepository.findByMinecraftUuid(minecraftUuid);
		if (linkedAccount != null) {
			return new ResponseEntity<>(linkedAccount, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getByMinecraftUsername(String minecraftUsername) {
		LinkedAccountModel linkedAccount = settingsRepository.findByMinecraftUsername(minecraftUsername);
		if (linkedAccount != null) {
			return new ResponseEntity<>(linkedAccount, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
		}
		if (settingsRepository.existsByMinecraftUuid(linkedAccountModel.getMinecraftUuid())) {
			deleteByMinecraftUuid(linkedAccountModel.getMinecraftUuid());
		}
		if (settingsRepository.existsByDiscordId(linkedAccountModel.getDiscordId())) {
			deleteByDiscordId(linkedAccountModel.getDiscordId());
		}

		settingsRepository.save(linkedAccountModel);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
