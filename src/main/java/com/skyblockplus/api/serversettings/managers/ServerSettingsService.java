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

package com.skyblockplus.api.serversettings.managers;

import com.skyblockplus.api.serversettings.automatedguild.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.blacklist.Blacklist;
import com.skyblockplus.api.serversettings.eventnotif.EventNotifSettings;
import com.skyblockplus.api.serversettings.jacob.JacobSettings;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServerSettingsService {

	private final ServerSettingsRepository settingsRepository;

	@Autowired
	public ServerSettingsService(ServerSettingsRepository settingsRepository) {
		this.settingsRepository = settingsRepository;
	}

	// General
	public boolean serverByServerIdExists(String serverId) {
		return settingsRepository.findServerByServerId(serverId) != null;
	}

	public List<ServerSettingsModel> getAllServerSettings() {
		List<ServerSettingsModel> serverSettingsModels = new ArrayList<>();
		settingsRepository.findAll().forEach(o1 -> serverSettingsModels.add(o1.copy(true)));

		return serverSettingsModels;
	}

	public ResponseEntity<?> getServerSettingsById(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.copy(true), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> addNewServerSettings(String serverId, ServerSettingsModel newServerSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings == null) {
			ServerSettingsModel tempSettingsModel = new ServerSettingsModel();
			tempSettingsModel.setServerId(newServerSettings.getServerId());
			tempSettingsModel.setServerName(newServerSettings.getServerName());
			settingsRepository.save(tempSettingsModel);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> deleteServerSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			settingsRepository.deleteByServerId(serverId);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setServerSettings(String serverId, ServerSettingsModel newServerSettings) {
		settingsRepository.deleteByServerId(serverId);
		settingsRepository.save(newServerSettings);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// Verify
	public ResponseEntity<?> getVerifySettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getAutomatedVerify(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setVerifySettings(String serverId, AutomatedVerify newVerifySettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setAutomatedVerify(newVerifySettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setVerifyRolesSettings(String serverId, String[] newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			AutomatedVerify verifySettings = currentServerSettings.getAutomatedVerify();
			verifySettings.setVerifiedRoles(new ArrayList<>(Arrays.asList(newSettings)));
			currentServerSettings.setAutomatedVerify(verifySettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	// Roles
	public ResponseEntity<?> getRolesSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getAutomatedRoles(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getRoleSettings(String serverId, String roleName) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			AutomatedRoles currentRoleSettings = currentServerSettings.getAutomatedRoles();
			try {
				return new ResponseEntity<>(
					new PropertyDescriptor(roleName, AutomatedRoles.class).getReadMethod().invoke(currentRoleSettings),
					HttpStatus.OK
				);
			} catch (Exception ignored) {}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setRolesSettings(String serverId, AutomatedRoles newRoleSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setAutomatedRoles(newRoleSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setRoleSettings(String serverId, RoleModel newRoleSettings, String roleName) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			AutomatedRoles currentRoleSettings = currentServerSettings.getAutomatedRoles();
			try {
				new PropertyDescriptor(roleName, AutomatedRoles.class).getWriteMethod().invoke(currentRoleSettings, newRoleSettings);
			} catch (Exception ignored) {}
			currentServerSettings.setAutomatedRoles(currentRoleSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	// Guild
	public ResponseEntity<?> getGuildSettings(String serverId, String name) {
		AutomatedGuild guildSettings = getGuildSettingsInt(serverId, name);
		return guildSettings != null ? new ResponseEntity<>(guildSettings, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setGuildSettings(String serverId, AutomatedGuild newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			String guildSettingsName = newSettings.getGuildName();
			if (guildSettingsName == null || guildSettingsName.length() == 0) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			if (
				currentServerSettings.getAutomatedGuildOne().getGuildName() != null &&
				currentServerSettings.getAutomatedGuildOne().getGuildName().equalsIgnoreCase(guildSettingsName)
			) {
				currentServerSettings.setAutomatedGuildOne(newSettings);
			} else if (
				currentServerSettings.getAutomatedGuildTwo().getGuildName() != null &&
				currentServerSettings.getAutomatedGuildTwo().getGuildName().equalsIgnoreCase(guildSettingsName)
			) {
				currentServerSettings.setAutomatedGuildTwo(newSettings);
			} else {
				if (currentServerSettings.getAutomatedGuildOne().getGuildName() == null) {
					currentServerSettings.setAutomatedGuildOne(newSettings);
				} else if (currentServerSettings.getAutomatedGuildTwo().getGuildName() == null) {
					currentServerSettings.setAutomatedGuildTwo(newSettings);
				} else {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}

			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public List<AutomatedGuild> getAllGuildSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ArrayList<>(
				Arrays.asList(currentServerSettings.getAutomatedGuildOne(), currentServerSettings.getAutomatedGuildTwo())
			);
		}
		return null;
	}

	public ResponseEntity<HttpStatus> removeGuildSettings(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			AutomatedGuild guildOne = currentServerSettings.getAutomatedGuildOne();
			AutomatedGuild guildTwo = currentServerSettings.getAutomatedGuildTwo();

			if (guildOne != null && guildOne.getGuildName() != null && guildOne.getGuildName().equalsIgnoreCase(name)) {
				currentServerSettings.setAutomatedGuildOne(new AutomatedGuild());
			} else if (guildTwo != null && guildTwo.getGuildName() != null && guildTwo.getGuildName().equalsIgnoreCase(name)) {
				currentServerSettings.setAutomatedGuildTwo(new AutomatedGuild());
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	private AutomatedGuild getGuildSettingsInt(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (
				currentServerSettings.getAutomatedGuildOne().getGuildName() != null &&
				currentServerSettings.getAutomatedGuildOne().getGuildName().equalsIgnoreCase(name)
			) {
				return currentServerSettings.getAutomatedGuildOne();
			} else if (
				currentServerSettings.getAutomatedGuildTwo().getGuildName() != null &&
				currentServerSettings.getAutomatedGuildTwo().getGuildName().equalsIgnoreCase(name)
			) {
				return currentServerSettings.getAutomatedGuildTwo();
			}
		}
		return null;
	}

	// Apply
	public ResponseEntity<?> getApplyUsersCache(String serverId, String name) {
		AutomatedGuild automatedGuild = getGuildSettingsInt(serverId, name);

		if (automatedGuild != null) {
			return new ResponseEntity<>(automatedGuild.getApplyUsersCache(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setApplyUsersCache(String serverId, String name, String newApplyCacheJsonString) {
		AutomatedGuild automatedGuild = getGuildSettingsInt(serverId, name);

		if (automatedGuild != null) {
			automatedGuild.setApplyUsersCache(newApplyCacheJsonString);

			return setGuildSettings(serverId, automatedGuild);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setApplyReqs(String serverId, String name, ApplyRequirements[] newReqs) {
		AutomatedGuild automatedGuild = getGuildSettingsInt(serverId, name);

		if (automatedGuild != null) {
			automatedGuild.setApplyReqs(new ArrayList<>(Arrays.asList(newReqs)));
			return setGuildSettings(serverId, automatedGuild);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getBlacklistSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getBlacklist(), HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setBlacklistSettings(String serverId, Blacklist blacklist) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setBlacklist(blacklist);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	// Event
	public boolean getSkyblockEventActive(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			try {
				if (currentServerSettings.getSbEvent().getEventType().length() > 0) {
					return true;
				}
			} catch (Exception ignored) {}
		}
		return false;
	}

	public boolean eventHasMemberByUuid(String serverId, String minecraftUuid) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				EventSettings eventSettings = currentServerSettings.getSbEvent();
				List<EventMember> eventMembers = eventSettings.getMembersList();
				for (EventMember eventMember : eventMembers) {
					if (eventMember.getUuid().equals(minecraftUuid)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public ResponseEntity<?> getSkyblockEventSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getSbEvent(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setSkyblockEventSettings(String serverId, EventSettings newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setSbEvent(newSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> addMemberToSkyblockEvent(String serverId, EventMember newEventMember) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				EventSettings eventSettings = currentServerSettings.getSbEvent();
				List<EventMember> eventMembers = eventSettings.getMembersList();
				eventMembers.add(newEventMember);
				eventSettings.setMembersList(eventMembers);

				return new ResponseEntity<>(setSkyblockEventSettings(serverId, eventSettings).getStatusCode());
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> removeMemberFromSkyblockEvent(String serverId, String minecraftUuid) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				EventSettings eventSettings = currentServerSettings.getSbEvent();
				List<EventMember> eventMembers = eventSettings.getMembersList();
				eventMembers.removeIf(eventMember -> eventMember.getUuid().equals(minecraftUuid));
				eventSettings.setMembersList(eventMembers);

				return new ResponseEntity<>(setSkyblockEventSettings(serverId, eventSettings).getStatusCode());
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	// Misc
	public ResponseEntity<?> getServerHypixelApiKey(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getHypixelApiKeyInt(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setServerHypixelApiKey(String serverId, String newKey) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setHypixelApiKey(newKey);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setApplyGuestRole(String serverId, String newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setApplyGuestRole(newSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setJacobSettings(String serverId, JacobSettings newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setJacobSettings(newSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setEventSettings(String serverId, EventNotifSettings newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setEventNotif(newSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setFetchurChannel(String serverId, String newChannelId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setFetchurChannel(newChannelId);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setFetchurRole(String serverId, String newRoleId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setFetchurRole(newRoleId);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setMayorChannel(String serverId, String newChannelId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setMayorChannel(newChannelId);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setMayorRole(String serverId, String newRoleId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setMayorRole(newRoleId);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setBotManagerRoles(String serverId, String[] newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setBotManagerRoles(new ArrayList<>(Arrays.asList(newSettings)));
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<Object> setChannelBlacklist(String serverId, String[] newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setChannelBlacklist(new ArrayList<>(Arrays.asList(newSettings)));
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setLogChannel(String serverId, String newChannelId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setLogChannel(newChannelId);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
}
