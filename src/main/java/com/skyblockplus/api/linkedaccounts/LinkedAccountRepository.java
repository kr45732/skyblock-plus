package com.skyblockplus.api.linkedaccounts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccountModel, Long> {
    LinkedAccountModel findByDiscordId(String discordId);

    LinkedAccountModel findByMinecraftUuid(String minecraftUuid);

    LinkedAccountModel findByMinecraftUsername(String minecraftUsername);

    void deleteByDiscordId(String discordId);

    void deleteByMinecraftUuid(String minecraftUuid);

    void deleteByMinecraftUsername(String minecraftUsername);

    boolean existsByDiscordId(String discordId);

    boolean existsByMinecraftUuid(String minecraftUuid);

    boolean existsByMinecraftUsername(String minecraftUsername);

}
