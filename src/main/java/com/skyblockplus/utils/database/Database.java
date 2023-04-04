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

package com.skyblockplus.utils.database;

import static com.skyblockplus.utils.utils.Utils.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.api.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.serversettings.automatedguild.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.blacklist.Blacklist;
import com.skyblockplus.api.serversettings.eventnotif.EventNotifSettings;
import com.skyblockplus.api.serversettings.jacob.JacobSettings;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.api.serversettings.managers.ServerSettingsService;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import com.skyblockplus.general.UnlinkSlashCommand;
import com.skyblockplus.utils.oauth.TokenData;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Database {

	public final ServerSettingsService settingsService;
	public final HikariDataSource dataSource;

	@Autowired
	public Database(ServerSettingsService settingsService, HikariDataSource dataSource) {
		this.settingsService = settingsService;
		this.dataSource = dataSource;
	}

	public int removeGuildSettings(String serverId, String name) {
		return settingsService.removeGuildSettings(serverId, name).getStatusCode().value();
	}

	public List<ServerSettingsModel> getAllServerSettings() {
		return settingsService.getAllServerSettings();
	}

	public JsonElement getServerSettings(String serverId) {
		return gson.toJsonTree(settingsService.getServerSettingsById(serverId).getBody());
	}

	public int newServerSettings(String serverId, ServerSettingsModel serverSettingsModel) {
		return settingsService.addNewServerSettings(serverId, serverSettingsModel).getStatusCode().value();
	}

	public int setServerSettings(ServerSettingsModel serverSettingsModel) {
		return settingsService.setServerSettings(serverSettingsModel).getStatusCode().value();
	}

	public int deleteServerSettings(String serverId) {
		return settingsService.deleteServerSettings(serverId).getStatusCode().value();
	}

	public JsonElement getVerifySettings(String serverId) {
		return gson.toJsonTree(settingsService.getVerifySettings(serverId).getBody());
	}

	public int setVerifySettings(String serverId, JsonElement newVerifySettings) {
		return settingsService.setVerifySettings(serverId, gson.fromJson(newVerifySettings, AutomatedVerify.class)).getStatusCode().value();
	}

	public JsonElement getRolesSettings(String serverId) {
		return gson.toJsonTree(settingsService.getRolesSettings(serverId).getBody());
	}

	public int setRolesSettings(String serverId, JsonElement newRoleSettings) {
		return settingsService.setRolesSettings(serverId, gson.fromJson(newRoleSettings, AutomatedRoles.class)).getStatusCode().value();
	}

	public RoleModel getRoleSettings(String serverId, String roleName) {
		return settingsService.getRoleSettings(serverId, roleName).getBody();
	}

	public int setRoleSettings(String serverId, RoleModel newRoleSettings) {
		return settingsService.setRoleSettings(serverId, newRoleSettings).getStatusCode().value();
	}

	public int removeRoleSettings(String serverId, String roleName) {
		return settingsService.removeRoleSettings(serverId, roleName).getStatusCode().value();
	}

	public int setApplyCacheSettings(String serverId, String name, String currentSettings) {
		return settingsService.setApplyUsersCache(serverId, name, currentSettings).getStatusCode().value();
	}

	public JsonArray getApplyCacheSettings(String serverId, String name) {
		try {
			return JsonParser.parseString((String) settingsService.getApplyUsersCache(serverId, name).getBody()).getAsJsonArray();
		} catch (Exception e) {
			return new JsonArray();
		}
	}

	public int deleteApplyCacheSettings(String serverId, String name) {
		return settingsService.setApplyUsersCache(serverId, name, "[]").getStatusCode().value();
	}

	public int setSkyblockEventSettings(String serverId, EventSettings currentSettings) {
		return settingsService.setSkyblockEventSettings(serverId, currentSettings).getStatusCode().value();
	}

	public int addMemberToSkyblockEvent(String serverId, EventMember newEventMember) {
		return settingsService.addMemberToSkyblockEvent(serverId, newEventMember).getStatusCode().value();
	}

	public JsonElement getSkyblockEventSettings(String serverId) {
		return gson.toJsonTree(settingsService.getSkyblockEventSettings(serverId).getBody());
	}

	public int removeMemberFromSkyblockEvent(String serverId, String minecraftUuid) {
		return settingsService.removeMemberFromSkyblockEvent(serverId, minecraftUuid).getStatusCode().value();
	}

	public boolean eventHasMemberByUuid(String serverId, String minecraftUuid) {
		return settingsService.eventHasMemberByUuid(serverId, minecraftUuid);
	}

	public int setApplyReqs(String serverId, String name, JsonArray newApplyReqs) {
		return settingsService.setApplyReqs(serverId, name, gson.fromJson(newApplyReqs, ApplyRequirements[].class)).getStatusCode().value();
	}

	public int setVerifyRolesSettings(String serverId, JsonArray newSettings) {
		return settingsService.setVerifyRolesSettings(serverId, gson.fromJson(newSettings, String[].class)).getStatusCode().value();
	}

	public String getServerHypixelApiKey(String serverId) {
		Object response = settingsService.getServerHypixelApiKey(serverId).getBody();
		return response != null ? (String) response : null;
	}

	public int setServerHypixelApiKey(String serverId, String newKey) {
		return settingsService.setServerHypixelApiKey(serverId, newKey).getStatusCode().value();
	}

	public JsonElement getBlacklistSettings(String serverId) {
		return gson.toJsonTree(settingsService.getBlacklistSettings(serverId).getBody());
	}

	public int setBlacklistSettings(String serverId, JsonElement newSettings) {
		return settingsService.setBlacklistSettings(serverId, gson.fromJson(newSettings, Blacklist.class)).getStatusCode().value();
	}

	public List<AutomatedGuild> getAllGuildSettings(String serverId) {
		return settingsService.getAllGuildSettings(serverId);
	}

	public JsonElement getGuildSettings(String serverId, String name) {
		return gson.toJsonTree(settingsService.getGuildSettings(serverId, name).getBody());
	}

	public int setGuildSettings(String serverId, JsonElement newSettings) {
		return settingsService.setGuildSettings(serverId, gson.fromJson(newSettings, AutomatedGuild.class)).getStatusCode().value();
	}

	public int setApplyGuestRole(String serverId, String newSettings) {
		return settingsService.setApplyGuestRole(serverId, newSettings).getStatusCode().value();
	}

	public int setJacobSettings(String serverId, JsonElement newSettings) {
		return settingsService.setJacobSettings(serverId, gson.fromJson(newSettings, JacobSettings.class)).getStatusCode().value();
	}

	public int setEventSettings(String serverId, JsonElement newSettings) {
		return settingsService.setEventSettings(serverId, gson.fromJson(newSettings, EventNotifSettings.class)).getStatusCode().value();
	}

	public int setFetchurChannel(String serverId, String newSettings) {
		return settingsService.setFetchurChannel(serverId, newSettings).getStatusCode().value();
	}

	public int setFetchurRole(String serverId, String newSettings) {
		return settingsService.setFetchurRole(serverId, newSettings).getStatusCode().value();
	}

	public int setMayorChannel(String serverId, String newSettings) {
		return settingsService.setMayorChannel(serverId, newSettings).getStatusCode().value();
	}

	public int setMayorRole(String serverId, String newSettings) {
		return settingsService.setMayorRole(serverId, newSettings).getStatusCode().value();
	}

	public int setBotManagerRoles(String serverId, JsonArray newSettings) {
		return settingsService.setBotManagerRoles(serverId, gson.fromJson(newSettings, String[].class)).getStatusCode().value();
	}

	public int setLogChannel(String serverId, String newSettings) {
		return settingsService.setLogChannel(serverId, newSettings).getStatusCode().value();
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public boolean insertLinkedAccount(LinkedAccount linkedAccount, Member member, JsonElement verifySettings) {
		try {
			String discord = linkedAccount.discord();
			long lastUpdated = linkedAccount.lastUpdated();
			String username = linkedAccount.username();
			String uuid = linkedAccount.uuid();

			//			try (
			//				Connection connection = getConnection();
			//				PreparedStatement statement = connection.prepareStatement(
			//					"INSERT INTO linked_account (last_updated, discord, username, uuid) VALUES (?, ?, ?, ?) ON CONFLICT (discord, username, uuid) DO UPDATE SET last_updated = EXCLUDED.last_updated, discord = EXCLUDED.discord, username = EXCLUDED.username, uuid = EXCLUDED.uuid RETURNING discord"
			//				)
			//			) {
			//				statement.setLong(1, lastUpdated);
			//				statement.setString(2, discord);
			//				statement.setString(3, username);
			//				statement.setString(4, uuid);
			//				try (ResultSet response = statement.executeQuery()) {
			//					if (response.next() && member != null && verifySettings != null) {
			//						String discordOld = response.getString("discord");
			//						if (!discord.equals(discordOld)) {
			//							UnlinkSlashCommand.unlinkAccount(member, verifySettings);
			//						}
			//					}
			//					return true;
			//				}
			//			}

			try (
				Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(
					"DELETE FROM linked_account WHERE discord = ? OR username = ? or uuid = ? RETURNING discord"
				)
			) {
				statement.setString(1, discord);
				statement.setString(2, username);
				statement.setString(3, uuid);
				try (ResultSet response = statement.executeQuery()) {
					if (response.next() && member != null && verifySettings != null) {
						String discordOld = response.getString("discord");
						if (!discord.equals(discordOld)) {
							UnlinkSlashCommand.unlinkAccount(member, verifySettings);
						}
					}
				}
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
			PreparedStatement statement = connection.prepareStatement("DELETE FROM linked_account WHERE " + type + " = ? RETURNING discord")
		) {
			statement.setString(1, value);

			try (ResultSet response = statement.executeQuery()) {
				if (response.next()) {
					TokenData.updateLinkedRolesMetadata(response.getString("discord"), null, null, false);
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
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
