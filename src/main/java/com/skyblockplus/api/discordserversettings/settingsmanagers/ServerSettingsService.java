package com.skyblockplus.api.discordserversettings.settingsmanagers;

import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class ServerSettingsService {
    private final ServerSettingsRepository settingsRepository;

    @Autowired
    public ServerSettingsService(ServerSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }


    public List<ServerSettingsModel> getAllServerSettings() {
        return settingsRepository.findAll();
    }

    public boolean serverByServerIdExists(String serverId) {
        return settingsRepository.findServerByServerId(serverId) != null;
    }

    public ResponseEntity<HttpStatus> addNewServerSettings(String serverId, ServerSettingsModel newServerSettings) {
        if (!serverByServerIdExists(serverId)) {
            ServerSettingsModel tempSettingsModel = new ServerSettingsModel();
            tempSettingsModel.setServerId(newServerSettings.getServerId());
            tempSettingsModel.setServerName(newServerSettings.getServerName());
            settingsRepository.save(tempSettingsModel);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> getServerSettingsById(String serverId) {
        if (serverByServerIdExists(serverId)) {
            return new ResponseEntity<>(settingsRepository.findServerByServerId(serverId), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<HttpStatus> updateServerSettings(String serverId, ServerSettingsModel newServerSettings) {
        if (serverByServerIdExists(serverId)) {
            settingsRepository.save(newServerSettings);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> getVerifySettings(String serverId) {
        if (serverByServerIdExists(serverId)) {
            return new ResponseEntity<>(settingsRepository.findServerByServerId(serverId).getAutomatedVerify(),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<HttpStatus> updateVerifySettings(String serverId, AutomatedVerify newVerifySettings) {
        if (serverByServerIdExists(serverId)) {
            ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);
            currentServerSettings.setAutomatedVerify(newVerifySettings);
            settingsRepository.save(currentServerSettings);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> getApplySettings(String serverId) {
        if (serverByServerIdExists(serverId)) {
            return new ResponseEntity<>(settingsRepository.findServerByServerId(serverId).getAutomatedApplication(),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<HttpStatus> updateApplySettings(String serverId, AutomatedApplication newApplySettings) {
        if (serverByServerIdExists(serverId)) {
            ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);
            currentServerSettings.setAutomatedApplication(newApplySettings);
            settingsRepository.save(currentServerSettings);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    }

    public ResponseEntity<?> getRolesSettings(String serverId) {
        if (serverByServerIdExists(serverId)) {
            return new ResponseEntity<>(settingsRepository.findServerByServerId(serverId).getAutomatedRoles(),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> getRoleSettings(String serverId, String roleName) {
        if (serverByServerIdExists(serverId)) {
            AutomatedRoles currentRoleSettings = settingsRepository.findServerByServerId(serverId).getAutomatedRoles();
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
            }
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<HttpStatus> updateRolesSettings(String serverId, AutomatedRoles newRoleSettings) {
        if (serverByServerIdExists(serverId)) {
            ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);
            currentServerSettings.setAutomatedRoles(newRoleSettings);
            settingsRepository.save(currentServerSettings);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    }

    @Transactional
    public ResponseEntity<HttpStatus> updateRoleSettings(String serverId, RoleModel newRoleSettings, String roleName) {
        if (serverByServerIdExists(serverId)) {
            ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);
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
            }
            currentServerSettings.setAutomatedRoles(currentRoleSettings);
            settingsRepository.save(currentServerSettings);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<HttpStatus> updateRolesEnable(String serverId, String newEnableSetting) {
        if (serverByServerIdExists(serverId)) {
            ServerSettingsModel currentServerSettings = settingsRepository.findServerByServerId(serverId);
            AutomatedRoles currentRoleSettings = currentServerSettings.getAutomatedRoles();
            currentRoleSettings.setEnable(newEnableSetting);
            currentServerSettings.setAutomatedRoles(currentRoleSettings);
            settingsRepository.save(currentServerSettings);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<HttpStatus> removeServerSettings(String serverId) {
        if(serverByServerIdExists(serverId)){
            settingsRepository.deleteByServerId(serverId);
            if(!serverByServerIdExists(serverId)){
                return new ResponseEntity<>(HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}
