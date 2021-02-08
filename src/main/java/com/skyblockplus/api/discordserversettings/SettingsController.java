package com.skyblockplus.api.discordserversettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SettingsController {

    private final SettingsService settingsService;

    @Autowired
    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/api/discord/serverSettings/list")
    public List<SettingsModel> getAllGuildSettings() {
        return settingsService.getGuilds();
    }
}
