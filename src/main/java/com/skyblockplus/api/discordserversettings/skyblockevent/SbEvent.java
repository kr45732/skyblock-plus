package com.skyblockplus.api.discordserversettings.skyblockevent;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

@Data
@AllArgsConstructor
@Embeddable
@Transactional
public class SbEvent {

  @Embedded
  RunningEvent runningEvent = new RunningEvent();

  private String eventActive = "false";

  public SbEvent() {}
}
