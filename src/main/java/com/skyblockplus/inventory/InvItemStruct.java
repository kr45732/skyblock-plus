package com.skyblockplus.inventory;

import lombok.Data;

@Data
public class InvItemStruct {
    private String name;
    private String lore;
    private int count;
    private String modifier;
    private String creationOrigin;
    private String id;
    private String creationTimestamp;
}
