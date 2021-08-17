package com.skyblockplus.api.serversettings.managers;

import static com.skyblockplus.utils.Utils.DEFAULT_PREFIX;

import com.skyblockplus.api.serversettings.automatedapply.ApplyRequirements;
import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedroles.RoleModel;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.mee6roles.Mee6Data;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.api.serversettings.skyblockevent.RunningEvent;
import com.skyblockplus.api.serversettings.skyblockevent.SbEvent;
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

	public List<ServerSettingsModel> getAllServerSettings() {
		List<ServerSettingsModel> serverSettingsModels = new ArrayList<>();
		settingsRepository.findAll().forEach(o1 -> serverSettingsModels.add(o1.copy(true)));

		return serverSettingsModels;
	}

	public boolean serverByServerIdExists(String serverId) {
		return settingsRepository.findServerByServerId(serverId) != null;
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

	public ResponseEntity<?> getServerSettingsById(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.copy(true), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

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

	public ResponseEntity<HttpStatus> setServerSettings(String serverId, ServerSettingsModel newServerSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			settingsRepository.save(newServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

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
			switch (roleName) {
				case "sven":
					return new ResponseEntity<>(currentRoleSettings.getSven(), HttpStatus.OK);
				case "rev":
					return new ResponseEntity<>(currentRoleSettings.getRev(), HttpStatus.OK);
				case "tara":
					return new ResponseEntity<>(currentRoleSettings.getTara(), HttpStatus.OK);
				case "bank_coins":
					return new ResponseEntity<>(currentRoleSettings.getBank_coins(), HttpStatus.OK);
				case "alchemy":
					return new ResponseEntity<>(currentRoleSettings.getAlchemy(), HttpStatus.OK);
				case "combat":
					return new ResponseEntity<>(currentRoleSettings.getCombat(), HttpStatus.OK);
				case "fishing":
					return new ResponseEntity<>(currentRoleSettings.getFishing(), HttpStatus.OK);
				case "foraging":
					return new ResponseEntity<>(currentRoleSettings.getForaging(), HttpStatus.OK);
				case "carpentry":
					return new ResponseEntity<>(currentRoleSettings.getCarpentry(), HttpStatus.OK);
				case "farming":
					return new ResponseEntity<>(currentRoleSettings.getFarming(), HttpStatus.OK);
				case "mining":
					return new ResponseEntity<>(currentRoleSettings.getMining(), HttpStatus.OK);
				case "taming":
					return new ResponseEntity<>(currentRoleSettings.getTaming(), HttpStatus.OK);
				case "enchanting":
					return new ResponseEntity<>(currentRoleSettings.getEnchanting(), HttpStatus.OK);
				case "catacombs":
					return new ResponseEntity<>(currentRoleSettings.getCatacombs(), HttpStatus.OK);
				case "guild_member":
					return new ResponseEntity<>(currentRoleSettings.getGuild_member(), HttpStatus.OK);
				case "fairy_souls":
					return new ResponseEntity<>(currentRoleSettings.getFairy_souls(), HttpStatus.OK);
				case "slot_collector":
					return new ResponseEntity<>(currentRoleSettings.getSlot_collector(), HttpStatus.OK);
				case "pet_enthusiast":
					return new ResponseEntity<>(currentRoleSettings.getPet_enthusiast(), HttpStatus.OK);
				case "doom_slayer":
					return new ResponseEntity<>(currentRoleSettings.getDoom_slayer(), HttpStatus.OK);
				case "all_slayer_nine":
					return new ResponseEntity<>(currentRoleSettings.getAll_slayer_nine(), HttpStatus.OK);
				case "skill_average":
					return new ResponseEntity<>(currentRoleSettings.getSkill_average(), HttpStatus.OK);
				case "pet_score":
					return new ResponseEntity<>(currentRoleSettings.getPet_score(), HttpStatus.OK);
				case "dungeon_secrets":
					return new ResponseEntity<>(currentRoleSettings.getDungeon_secrets(), HttpStatus.OK);
				case "guild_ranks":
					return new ResponseEntity<>(currentRoleSettings.getGuild_ranks(), HttpStatus.OK);
				case "enderman":
					return new ResponseEntity<>(currentRoleSettings.getEnderman(), HttpStatus.OK);
				case "weight":
					return new ResponseEntity<>(currentRoleSettings.getWeight(), HttpStatus.OK);
				case "total_slayer":
					return new ResponseEntity<>(currentRoleSettings.getTotal_slayer(), HttpStatus.OK);
			}
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
			switch (roleName) {
				case "sven":
					currentRoleSettings.setSven(newRoleSettings);
					break;
				case "rev":
					currentRoleSettings.setRev(newRoleSettings);
					break;
				case "tara":
					currentRoleSettings.setTara(newRoleSettings);
					break;
				case "bank_coins":
					currentRoleSettings.setBank_coins(newRoleSettings);
					break;
				case "alchemy":
					currentRoleSettings.setAlchemy(newRoleSettings);
					break;
				case "combat":
					currentRoleSettings.setCombat(newRoleSettings);
					break;
				case "fishing":
					currentRoleSettings.setFishing(newRoleSettings);
					break;
				case "foraging":
					currentRoleSettings.setForaging(newRoleSettings);
					break;
				case "carpentry":
					currentRoleSettings.setCarpentry(newRoleSettings);
					break;
				case "farming":
					currentRoleSettings.setFarming(newRoleSettings);
					break;
				case "mining":
					currentRoleSettings.setMining(newRoleSettings);
					break;
				case "taming":
					currentRoleSettings.setTaming(newRoleSettings);
					break;
				case "enchanting":
					currentRoleSettings.setEnchanting(newRoleSettings);
					break;
				case "catacombs":
					currentRoleSettings.setCatacombs(newRoleSettings);
					break;
				case "guild_member":
					currentRoleSettings.setGuild_member(newRoleSettings);
					break;
				case "fairy_souls":
					currentRoleSettings.setFairy_souls(newRoleSettings);
					break;
				case "slot_collector":
					currentRoleSettings.setSlot_collector(newRoleSettings);
					break;
				case "pet_enthusiast":
					currentRoleSettings.setPet_enthusiast(newRoleSettings);
					break;
				case "doom_slayer":
					currentRoleSettings.setDoom_slayer(newRoleSettings);
					break;
				case "all_slayer_nine":
					currentRoleSettings.setAll_slayer_nine(newRoleSettings);
					break;
				case "skill_average":
					currentRoleSettings.setSkill_average(newRoleSettings);
					break;
				case "pet_score":
					currentRoleSettings.setPet_score(newRoleSettings);
					break;
				case "dungeon_secrets":
					currentRoleSettings.setDungeon_secrets(newRoleSettings);
					break;
				case "guild_ranks":
					currentRoleSettings.setGuild_ranks(newRoleSettings);
					break;
				case "enderman":
					currentRoleSettings.setEnderman(newRoleSettings);
					break;
				case "weight":
					currentRoleSettings.setWeight(newRoleSettings);
					break;
				case "total_slayer":
					currentRoleSettings.setTotal_slayer(newRoleSettings);
					break;
			}
			currentServerSettings.setAutomatedRoles(currentRoleSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setRolesEnable(String serverId, String newEnableSetting) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			AutomatedRoles currentRoleSettings = currentServerSettings.getAutomatedRoles();
			currentRoleSettings.setEnable(newEnableSetting);
			currentServerSettings.setAutomatedRoles(currentRoleSettings);
			settingsRepository.save(currentServerSettings);
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

	public ResponseEntity<?> getApplyUsersCache(String serverId, String name) {
		AutomatedApply automatedApplication = getApplySettings(serverId, name);

		if (automatedApplication != null) {
			return new ResponseEntity<>(automatedApplication.getApplyUsersCache(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setApplyUsersCache(String serverId, String name, String newApplyCacheJsonString) {
		AutomatedApply automatedApplication = getApplySettings(serverId, name);

		if (automatedApplication != null) {
			automatedApplication.setApplyUsersCache(newApplyCacheJsonString);

			return setApplySettings(serverId, automatedApplication);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getSkyblockEventSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getSbEvent(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getRunningSkyblockEventSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				return new ResponseEntity<>(currentServerSettings.getSbEvent().getRunningEvent(), HttpStatus.OK);
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public boolean getSkyblockEventActive(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			try {
				if (currentServerSettings.getSbEvent().getEventActive().equals("true")) {
					return true;
				}
			} catch (Exception ignored) {}
		}
		return false;
	}

	public ResponseEntity<HttpStatus> setSkyblockRunningEvent(String serverId, RunningEvent newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			SbEvent currentSbEventSettings = currentServerSettings.getSbEvent();
			currentSbEventSettings.setRunningEvent(newSettings);
			currentServerSettings.setSbEvent(currentSbEventSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setSkyblockEventSettings(String serverId, SbEvent newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setSbEvent(newSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> addEventMemberToRunningEvent(String serverId, EventMember newEventMember) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				RunningEvent runningEvent = currentServerSettings.getSbEvent().getRunningEvent();
				List<EventMember> eventMembers = runningEvent.getMembersList();
				eventMembers.add(newEventMember);
				runningEvent.setMembersList(eventMembers);

				return new ResponseEntity<>(setSkyblockRunningEvent(serverId, runningEvent).getStatusCode());
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getSkyblockEventGuildId(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				RunningEvent runningEvent = currentServerSettings.getSbEvent().getRunningEvent();

				return new ResponseEntity<>(runningEvent.getEventGuildId(), HttpStatus.OK);
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> removeEventMemberFromRunningEvent(String serverId, String minecraftUuid) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				RunningEvent runningEvent = currentServerSettings.getSbEvent().getRunningEvent();
				List<EventMember> eventMembers = runningEvent.getMembersList();
				eventMembers.removeIf(eventMember -> eventMember.getUuid().equals(minecraftUuid));
				runningEvent.setMembersList(eventMembers);

				return new ResponseEntity<>(setSkyblockRunningEvent(serverId, runningEvent).getStatusCode());
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public boolean eventHasMemberByUuid(String serverId, String minecraftUuid) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (getSkyblockEventActive(serverId)) {
				RunningEvent runningEvent = currentServerSettings.getSbEvent().getRunningEvent();
				List<EventMember> eventMembers = runningEvent.getMembersList();
				for (EventMember eventMember : eventMembers) {
					if (eventMember.getUuid().equals(minecraftUuid)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public ResponseEntity<?> getApplyReqs(String serverId, String name) {
		AutomatedApply applySettings = getApplySettings(serverId, name);

		if (applySettings != null) {
			return new ResponseEntity<>(applySettings.getApplyReqs(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setApplyReqs(String serverId, String name, ApplyRequirements[] newReqs) {
		AutomatedApply applySettings = getApplySettings(serverId, name);

		if (applySettings != null) {
			applySettings.setApplyReqs(new ArrayList<>(Arrays.asList(newReqs)));
			return setApplySettings(serverId, applySettings);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public List<AutomatedApply> getAllApplySettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ArrayList<>(
				Arrays.asList(currentServerSettings.getAutomatedApplicationOne(), currentServerSettings.getAutomatedApplicationTwo())
			);
		}
		return null;
	}

	public ResponseEntity<HttpStatus> setApplySettings(String serverId, AutomatedApply newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			String applySettingsName = newSettings.getName();
			if (applySettingsName == null || applySettingsName.length() == 0) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			if (
				currentServerSettings.getAutomatedApplicationOne().getName() != null &&
				currentServerSettings.getAutomatedApplicationOne().getName().equalsIgnoreCase(applySettingsName)
			) {
				currentServerSettings.setAutomatedApplicationOne(newSettings);
			} else if (
				currentServerSettings.getAutomatedApplicationTwo().getName() != null &&
				currentServerSettings.getAutomatedApplicationTwo().getName().equalsIgnoreCase(applySettingsName)
			) {
				currentServerSettings.setAutomatedApplicationTwo(newSettings);
			} else {
				if (currentServerSettings.getAutomatedApplicationOne().getName() == null) {
					currentServerSettings.setAutomatedApplicationOne(newSettings);
				} else if (currentServerSettings.getAutomatedApplicationTwo().getName() == null) {
					currentServerSettings.setAutomatedApplicationTwo(newSettings);
				} else {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}

			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setGuildRoleSettings(String serverId, GuildRole newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			String name = newSettings.getName();
			if (name == null || name.length() == 0) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			if (
				currentServerSettings.getAutomaticGuildRolesOne().getName() != null &&
				currentServerSettings.getAutomaticGuildRolesOne().getName().equalsIgnoreCase(name)
			) {
				currentServerSettings.setAutomaticGuildRolesOne(newSettings);
			} else if (
				currentServerSettings.getAutomaticGuildRolesTwo().getName() != null &&
				currentServerSettings.getAutomaticGuildRolesTwo().getName().equalsIgnoreCase(name)
			) {
				currentServerSettings.setAutomaticGuildRolesTwo(newSettings);
			} else {
				if (currentServerSettings.getAutomaticGuildRolesOne().getName() == null) {
					currentServerSettings.setAutomaticGuildRolesOne(newSettings);
				} else if (currentServerSettings.getAutomaticGuildRolesTwo().getName() == null) {
					currentServerSettings.setAutomaticGuildRolesTwo(newSettings);
				} else {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}

			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public AutomatedApply getApplySettings(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (
				currentServerSettings.getAutomatedApplicationOne().getName() != null &&
				currentServerSettings.getAutomatedApplicationOne().getName().equalsIgnoreCase(name)
			) {
				return currentServerSettings.getAutomatedApplicationOne();
			} else if (
				currentServerSettings.getAutomatedApplicationTwo().getName() != null &&
				currentServerSettings.getAutomatedApplicationTwo().getName().equalsIgnoreCase(name)
			) {
				return currentServerSettings.getAutomatedApplicationTwo();
			}
		}
		return null;
	}

	public ResponseEntity<?> getApplySettingsExt(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (
				currentServerSettings.getAutomatedApplicationOne().getName() != null &&
				currentServerSettings.getAutomatedApplicationOne().getName().equalsIgnoreCase(name)
			) {
				return new ResponseEntity<>(currentServerSettings.getAutomatedApplicationOne(), HttpStatus.OK);
			} else if (
				currentServerSettings.getAutomatedApplicationTwo().getName() != null &&
				currentServerSettings.getAutomatedApplicationTwo().getName().equalsIgnoreCase(name)
			) {
				return new ResponseEntity<>(currentServerSettings.getAutomatedApplicationTwo(), HttpStatus.OK);
			}
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

	public ResponseEntity<HttpStatus> removeApplySettings(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			AutomatedApply applyOne = currentServerSettings.getAutomatedApplicationOne();
			AutomatedApply applyTwo = currentServerSettings.getAutomatedApplicationTwo();

			if (applyOne != null && applyOne.getName().equalsIgnoreCase(name)) {
				currentServerSettings.setAutomatedApplicationOne(new AutomatedApply());
			} else if (applyTwo != null && applyTwo.getName().equalsIgnoreCase(name)) {
				currentServerSettings.setAutomatedApplicationTwo(new AutomatedApply());
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> removeGuildSettings(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			GuildRole applyOne = currentServerSettings.getAutomaticGuildRolesOne();
			GuildRole applyTwo = currentServerSettings.getAutomaticGuildRolesTwo();

			if (applyOne != null && applyOne.getName().equalsIgnoreCase(name)) {
				currentServerSettings.setAutomaticGuildRolesOne(new GuildRole());
			} else if (applyTwo != null && applyTwo.getName().equalsIgnoreCase(name)) {
				currentServerSettings.setAutomaticGuildRolesTwo(new GuildRole());
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public List<GuildRole> getAllGuildRolesSettings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ArrayList<>(
				Arrays.asList(currentServerSettings.getAutomaticGuildRolesOne(), currentServerSettings.getAutomaticGuildRolesTwo())
			);
		}
		return null;
	}

	public ResponseEntity<?> getGuildRoleSettingsExt(String serverId, String name) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			if (
				currentServerSettings.getAutomaticGuildRolesOne().getName() != null &&
				currentServerSettings.getAutomaticGuildRolesOne().getName().equalsIgnoreCase(name)
			) {
				return new ResponseEntity<>(currentServerSettings.getAutomaticGuildRolesOne(), HttpStatus.OK);
			} else if (
				currentServerSettings.getAutomaticGuildRolesTwo().getName() != null &&
				currentServerSettings.getAutomaticGuildRolesTwo().getName().equalsIgnoreCase(name)
			) {
				return new ResponseEntity<>(currentServerSettings.getAutomaticGuildRolesTwo(), HttpStatus.OK);
			}
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<?> getMee6Settings(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			return new ResponseEntity<>(currentServerSettings.getMee6Data(), HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setMee6Settings(String serverId, Mee6Data newSettings) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setMee6Data(newSettings);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<HttpStatus> setPrefix(String serverId, String prefix) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			currentServerSettings.setPrefix(prefix);
			settingsRepository.save(currentServerSettings);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	public ResponseEntity<String> getPrefix(String serverId) {
		ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);

		if (currentServerSettings != null) {
			String dbPrefix = currentServerSettings.getPrefix();
			return new ResponseEntity<>(
				(dbPrefix != null && dbPrefix.length() > 0 && dbPrefix.length() <= 5) ? dbPrefix : DEFAULT_PREFIX,
				HttpStatus.OK
			);
		}

		return new ResponseEntity<>(DEFAULT_PREFIX, HttpStatus.BAD_REQUEST);
	}
}
