package com.skyblockplus.api.linkedaccounts;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
public class LinkedAccountModel {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String lastUpdated = "";
  private String discordId = "";
  private String minecraftUuid = "";
  private String minecraftUsername = "";

  public LinkedAccountModel(
    String lastUpdated,
    String discordId,
    String minecraftUuid,
    String minecraftUsername
  ) {
    this.lastUpdated = lastUpdated;
    this.discordId = discordId;
    this.minecraftUuid = minecraftUuid;
    this.minecraftUsername = minecraftUsername;
  }

  public LinkedAccountModel() {}
}
