package com.skyblockplus.api.discordserversettings;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public List<SettingsModel> getGuilds() {
        return settingsRepository.findAll();
    }

}
