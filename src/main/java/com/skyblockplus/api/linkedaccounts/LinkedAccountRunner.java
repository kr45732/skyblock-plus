package com.skyblockplus.api.linkedaccounts;

import com.skyblockplus.api.discordserversettings.linkedaccounts.LinkedAccount;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsModel;
import com.skyblockplus.api.discordserversettings.settingsmanagers.ServerSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Component
public class LinkedAccountRunner {
ServerSettingsService settingsService;
    @Autowired
    public LinkedAccountRunner( ServerSettingsService settingsService){
        this.settingsService = settingsService;
    }

    @Bean
    CommandLineRunner commandLineRunner(LinkedAccountRepository linkedAccountRepository) {
        for(ServerSettingsModel serverSettingsModel: settingsService.getAllServerSettings()){
            for(LinkedAccount linkedAccount: serverSettingsModel.getLinkedAccounts()){
                System.out.println(linkedAccount);
            }
        }

        return args -> {
            linkedAccountRepository.deleteAll();
            LinkedAccountModel userOne = new LinkedAccountModel(1L, "" + Instant.now().toEpochMilli(), "1.1", "1.2", "1.3");
            LinkedAccountModel userTwo = new LinkedAccountModel(2L, "" + Instant.now().toEpochMilli(), "2.1", "2.2", "2.3");
            linkedAccountRepository.saveAll(Arrays.asList(userOne, userTwo));
        };
    }
}