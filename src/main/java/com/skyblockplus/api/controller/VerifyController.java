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

package com.skyblockplus.api.controller;

import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.oauth.TokenData;
import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(headers = "X-Subdomain-Internal=verify")
public class VerifyController {

	@GetMapping("/")
	public ResponseEntity<?> getDiscordVerify(HttpServletResponse res, HttpServletRequest req) {
		try {
			String redirectUri = ServletUriComponentsBuilder.fromRequest(req).replacePath("/callback").build().toUriString();

			String state = oAuthClient.generateState(redirectUri);
			Cookie stateCookie = new Cookie("clientState", state);
			stateCookie.setMaxAge(1000 * 60 * 5);
			res.addCookie(stateCookie);

			return ResponseEntity.status(HttpStatus.FOUND).location(oAuthClient.createAuthorizationUri(state)).build();
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("callback")
	public ResponseEntity<?> getDiscordOauth(
		HttpServletResponse res,
		HttpServletRequest req,
		@RequestParam(value = "code") String code,
		@RequestParam(value = "state") String state,
		@CookieValue(value = "clientState") String clientState
	) {
		try {
			if (!Objects.equals(clientState, state)) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			String redirectUri = oAuthClient.consumeState(state);
			if (redirectUri == null) {
				return new ResponseEntity<>(HttpStatus.FORBIDDEN);
			}

			TokenData tokenData = oAuthClient.postToken(code, redirectUri);
			String userId = oAuthClient.getDiscord(tokenData);

			LinkedAccount linkedAccount = database.getByDiscord(userId);
			Player player = null;
			if (linkedAccount != null) {
				player = Player.create(linkedAccount.uuid());
			}
			if (TokenData.updateLinkedRolesMetadata(userId, linkedAccount, player, false).get()) {
				res.sendRedirect("/success.html");
				return new ResponseEntity<>(HttpStatus.FOUND);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
