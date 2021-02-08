package com.skyblockplus.api.discordserversettings;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SettingsConfig {

    @Bean
    CommandLineRunner commandLineRunner(SettingsRepository settingsRepository) {
        return args -> {
            SettingsModel guildSettingsOne = new SettingsModel(
                    1L,
                    "Test Server 1",
                    new AutomatedApplication()
            );

            settingsRepository.save(guildSettingsOne);
        };
    }
}
