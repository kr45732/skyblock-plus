package com.skyblockplus.api.discordserversettings.settingsmanagers;

import com.skyblockplus.api.discordserversettings.automatedapplication.AutomatedApplication;
import com.skyblockplus.api.discordserversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedverify.AutomatedVerify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"https://skyblock-plus.herokuapp.com/", "http://localhost:8080/"})
@RequestMapping("/api/discord/serverSettings")
public class ServerSettingsController {

    private final ServerSettingsService settingsService;

    @Autowired
    public ServerSettingsController(ServerSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/get/all")
    public List<ServerSettingsModel> getAllServerSettings() {
        return settingsService.getAllServerSettings();
    }

    @GetMapping("/get/byId")
    public ResponseEntity<?> getServerSettings(@RequestParam(value = "serverId") String serverId) {
        return settingsService.getServerSettingsById(serverId);
    }

    @PostMapping("/add/byId")
    public ResponseEntity<HttpStatus> addNewServerSettings(@RequestParam(value = "serverId") String serverId,
                                                           @RequestBody ServerSettingsModel serverSettingsModel) {
        return settingsService.addNewServerSettings(serverId, serverSettingsModel);
    }

    @PostMapping("/update/byId")
    public ResponseEntity<HttpStatus> updateServerSettings(@RequestParam(value = "serverId") String serverId,
                                                           @RequestBody ServerSettingsModel serverSettingsModel) {
        return settingsService.updateServerSettings(serverId, serverSettingsModel);
    }

    @GetMapping("/get/verify")
    public ResponseEntity<?> getVerifySettings(@RequestParam(value = "serverId") String serverId) {
        return settingsService.getVerifySettings(serverId);
    }

    @PostMapping("/update/verify")
    public ResponseEntity<HttpStatus> updateVerifySettings(@RequestParam(value = "serverId") String serverId,
                                                           @RequestBody AutomatedVerify newVerifySettings) {
        return settingsService.updateVerifySettings(serverId, newVerifySettings);
    }

    @GetMapping("/get/apply")
    public ResponseEntity<?> getApplySettings(@RequestParam(value = "serverId") String serverId) {
        return settingsService.getApplySettings(serverId);
    }

    @PostMapping("/update/apply")
    public ResponseEntity<HttpStatus> updateApplySettings(@RequestParam(value = "serverId") String serverId,
                                                          @RequestBody AutomatedApplication newApplySettings) {
        return settingsService.updateApplySettings(serverId, newApplySettings);
    }

    @GetMapping("/get/roles")
    public ResponseEntity<?> getRolesSettings(@RequestParam(value = "serverId") String serverId) {
        return settingsService.getRolesSettings(serverId);
    }

    @PostMapping("/update/roles")
    public ResponseEntity<HttpStatus> updateRolesSettings(@RequestParam(value = "serverId") String serverId,
                                                          @RequestBody AutomatedRoles newRoleSettings) {
        return settingsService.updateRolesSettings(serverId, newRoleSettings);
    }

    @PostMapping("/update/roles/enable")
    public ResponseEntity<HttpStatus> updateRolesEnable(@RequestParam(value = "serverId") String serverId,
                                                        @RequestParam(value = "enable") String enable) {
        return settingsService.updateRolesEnable(serverId, enable);
    }

    @GetMapping("/get/role")
    public ResponseEntity<?> getRoleSettings(@RequestParam(value = "serverId") String serverId,
                                             @RequestParam(value = "roleName") String roleName) {
        return settingsService.getRoleSettings(serverId, roleName);
    }

    @PostMapping("/update/role")
    public ResponseEntity<HttpStatus> updateRoleSettings(@RequestParam(value = "serverId") String serverId,
                                                         @RequestParam(value = "roleName") String roleName, @RequestBody RoleModel newRoleSettings) {
        return settingsService.updateRoleSettings(serverId, newRoleSettings, roleName);
    }

}
