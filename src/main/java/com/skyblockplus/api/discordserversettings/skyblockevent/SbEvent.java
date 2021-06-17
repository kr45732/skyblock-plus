package com.skyblockplus.api.discordserversettings.skyblockevent;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Data
@AllArgsConstructor
@Embeddable
@Transactional
public class SbEvent {

    @Embedded
    RunningEvent runningEvent = new RunningEvent();

    private String eventActive = "false";

    public SbEvent() {
    }
}
