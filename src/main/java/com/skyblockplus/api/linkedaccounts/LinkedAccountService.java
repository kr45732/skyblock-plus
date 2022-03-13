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

package com.skyblockplus.api.linkedaccounts;

import static com.skyblockplus.utils.Utils.*;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LinkedAccountService {

	private final HikariDataSource dataSource;

	public LinkedAccountService() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(LINKED_USER_URL);
		config.setUsername(LINKED_USER_USERNAME);
		config.setPassword(LINKED_USER_PASSWORD);
		dataSource = new HikariDataSource(config);
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public boolean insertLinkedAccount(LinkedAccount linkedAccount) {
		try {
			String discord = linkedAccount.discord();
			long lastUpdated = linkedAccount.lastUpdated();
			String username = linkedAccount.username();
			String uuid = linkedAccount.uuid();

			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"DELETE FROM linked_account WHERE discord = ? OR username = ? or uuid = ?"
				)
			) {
				statement.setString(1, discord);
				statement.setString(2, username);
				statement.setString(3, uuid);
				statement.executeUpdate();
			}

			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO linked_account (last_updated, discord, username, uuid) VALUES (?, ?, ?, ?)"
				)
			) {
				statement.setLong(1, lastUpdated);
				statement.setString(2, discord);
				statement.setString(3, username);
				statement.setString(4, uuid);
				return statement.executeUpdate() == 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public LinkedAccount getByUsername(String username) {
		return getBy("username", username);
	}

	public LinkedAccount getByUuid(String uuid) {
		return getBy("uuid", uuid);
	}

	public LinkedAccount getByDiscord(String discord) {
		return getBy("discord", discord);
	}

	public List<LinkedAccount> getAllLinkedAccounts() {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM linked_account")
		) {
			try (ResultSet response = statement.executeQuery()) {
				List<LinkedAccount> linkedAccounts = new ArrayList<>();
				while (response.next()) {
					linkedAccounts.add(responseToRecord(response));
				}
				return linkedAccounts;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public List<String> getClosestLinkedAccounts(String toMatch) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(
				"SELECT username FROM linked_account ORDER BY SIMILARITY(username, ?) DESC LIMIT 25"
			)
		) {
			statement.setString(1, toMatch);
			try (ResultSet response = statement.executeQuery()) {
				List<String> usernames = new ArrayList<>();
				while (response.next()) {
					usernames.add(response.getString("username"));
				}
				return usernames;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public boolean deleteByDiscord(String discord) {
		return deleteBy("discord", discord);
	}

	public boolean deleteByUuid(String uuid) {
		return deleteBy("uuid", uuid);
	}

	public boolean deleteByUsername(String username) {
		return deleteBy("username", username);
	}

	private LinkedAccount getBy(String type, String value) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM linked_account where " + type + " = ?")
		) {
			statement.setString(1, value);
			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					return responseToRecord(response);
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	private boolean deleteBy(String type, String value) {
		try (
			Connection connection = getConnection();
			PreparedStatement statement = connection.prepareStatement("DELETE FROM linked_account WHERE " + type + " = ?")
		) {
			statement.setString(1, value);
			return statement.executeUpdate() == 1;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private LinkedAccount responseToRecord(ResultSet response) throws SQLException {
		return new LinkedAccount(
			response.getLong("last_updated"),
			response.getString("discord"),
			response.getString("uuid"),
			response.getString("username")
		);
	}
}
