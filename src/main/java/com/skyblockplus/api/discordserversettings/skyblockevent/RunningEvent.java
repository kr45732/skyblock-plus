package com.skyblockplus.api.discordserversettings.skyblockevent;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Embeddable
@Transactional
public class RunningEvent {
    private String eventType = "";
    private String announcementId = "";
    private String timeEndingSeconds = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private Map<Integer, String> prizeMap = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<EventMember> membersList = new ArrayList<>();

    private String eventGuildId = "";

    public RunningEvent() {
    }
}
