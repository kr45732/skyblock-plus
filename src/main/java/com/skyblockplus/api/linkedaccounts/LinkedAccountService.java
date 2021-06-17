package com.skyblockplus.api.linkedaccounts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class LinkedAccountService {

    private final LinkedAccountRepository settingsRepository;

    @Autowired
    public LinkedAccountService(LinkedAccountRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public List<LinkedAccountModel> getAllLinkedAccounts() {
        return settingsRepository.findAll();
    }

    public ResponseEntity<?> getByDiscordId(String discordId) {
        if (accountByDiscordIdExists(discordId)) {
            return new ResponseEntity<>(settingsRepository.findByDiscordId(discordId), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private boolean accountByDiscordIdExists(String discordId) {
        return settingsRepository.findByDiscordId(discordId) != null;
    }

    public ResponseEntity<?> getByMinecraftUuid(String minecraftUuid) {
        if (accountByMinecraftUuidExists(minecraftUuid)) {
            return new ResponseEntity<>(settingsRepository.findByMinecraftUuid(minecraftUuid), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private boolean accountByMinecraftUuidExists(String minecraftUuid) {
        return settingsRepository.findByMinecraftUuid(minecraftUuid) != null;
    }

    public ResponseEntity<?> getByMinecraftUsername(String minecraftUsername) {
        if (accountByMinecraftUsernameExists(minecraftUsername)) {
            return new ResponseEntity<>(settingsRepository.findByMinecraftUsername(minecraftUsername), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private boolean accountByMinecraftUsernameExists(String minecraftUsername) {
        return (settingsRepository.findByMinecraftUsername(minecraftUsername) != null);
    }

    public void deleteByMinecraftUsername(String minecraftUsername) {
        settingsRepository.deleteByMinecraftUsername(minecraftUsername);
    }

    public void deleteByDiscordId(String discordId) {
        settingsRepository.deleteByDiscordId(discordId);
    }

    public void deleteByMinecraftUuid(String minecraftUuid) {
        settingsRepository.deleteByMinecraftUuid(minecraftUuid);
    }

    public ResponseEntity<HttpStatus> addNewLinkedAccount(LinkedAccountModel linkedAccountModel) {
        if (settingsRepository.existsByMinecraftUsername(linkedAccountModel.getMinecraftUsername())) {
            deleteByMinecraftUsername(linkedAccountModel.getMinecraftUsername());
        } else if (settingsRepository.existsByMinecraftUuid(linkedAccountModel.getMinecraftUuid())) {
            deleteByMinecraftUuid(linkedAccountModel.getMinecraftUuid());
        } else if (settingsRepository.existsByDiscordId(linkedAccountModel.getDiscordId())) {
            deleteByDiscordId(linkedAccountModel.getDiscordId());
        }

        settingsRepository.save(linkedAccountModel);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
