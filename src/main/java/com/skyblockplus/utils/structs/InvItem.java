package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.Utils.parseMcCodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Data;

@Data
public class InvItem {

  private String name;
  private String lore;
  private int count = 1;
  private String modifier;
  private String creationOrigin;
  private String id;
  private String creationTimestamp;
  private List<String> enchantsFormatted = new ArrayList<>();
  private int hbpCount = 0;
  private int fumingCount = 0;
  private boolean recombobulated = false;
  private List<String> extraStats = new ArrayList<>();
  private List<InvItem> backpackItems = new ArrayList<>();
  private String rarity;
  private int dungeonFloor = 0;
  private String nbtTag;

  public void setHbpCount(int hbpCount) {
    if (hbpCount > 10) {
      this.fumingCount = hbpCount - 10;
      this.hbpCount = 10;
    } else {
      this.hbpCount = hbpCount;
    }
  }

  public void addExtraValue(String itemId) {
    extraStats.add(itemId);
  }

  public void setLore(String lore) {
    this.lore = lore;
    if (lore != null) {
      String[] loreArr = parseMcCodes(lore).split("\n");
      this.rarity = loreArr[loreArr.length - 1].trim().split(" ")[0];
    }
  }

  public void setBackpackItems(Collection<InvItem> backpackItems) {
    this.backpackItems.clear();
    this.backpackItems.addAll(backpackItems);
  }
}
