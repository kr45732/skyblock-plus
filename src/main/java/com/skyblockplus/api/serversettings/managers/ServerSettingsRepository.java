package com.skyblockplus.api.serversettings.managers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerSettingsRepository extends JpaRepository<ServerSettingsModel, Long> {
	ServerSettingsModel findServerByServerId(String serverId);

	void deleteByServerId(String ServerId);
}
