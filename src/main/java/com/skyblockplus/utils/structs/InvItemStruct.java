package com.skyblockplus.utils.structs;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class InvItemStruct {
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
    private List<InvItemStruct> backpackItems = new ArrayList<>();

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

    public String getRarity() {
        if (lore != null) {
            String[] loreArr = lore.split("\n");
            return loreArr[loreArr.length - 1].trim().split(" ")[0];
        }

        return null;
    }

    public void setBackpackItems(Collection<InvItemStruct> backpackItems) {
        this.backpackItems.clear();
        this.backpackItems.addAll(backpackItems);
    }
}
