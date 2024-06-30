/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.api.controller;

import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Handles requests to haste subdomain */
@RestController
@RequestMapping(headers = "X-Subdomain-Internal=haste")
public class HasteController {

	@GetMapping(value = "/{id}")
	public ResponseEntity<?> getHaste(@PathVariable String id) {
		String haste = leaderboardDatabase.getHaste(id);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain");
		headers.add(HttpHeaders.ACCEPT, "text/plain");

		return haste == null
			? new ResponseEntity<>("{\"message\": \"Document not found\"}", headers, HttpStatus.NOT_FOUND)
			: new ResponseEntity<>(haste, headers, HttpStatus.OK);
	}
}
