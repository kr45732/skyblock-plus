package com.SkyblockBot.Utils;

public class ArmorStruct {
    private String helmet;
    private String chestplate;
    private String leggings;
    private String boots;

    public ArmorStruct() {
        this.helmet = "Empty";
        this.chestplate = "Empty";
        this.leggings = "Empty";
        this.boots = "Empty";
    }

    public ArmorStruct(String helmet, String chestplate, String leggings, String boots) {
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }



    public String getHelmet() {
        return helmet;
    }

    public void setHelmet(String helmet) {
        this.helmet = helmet;
    }

    public String getChestplate() {
        return chestplate;
    }

    public void setChestplate(String chestplate) {
        this.chestplate = chestplate;
    }

    public String getLeggings() {
        return leggings;
    }

    public void setLeggings(String leggings) {
        this.leggings = leggings;
    }

    public String getBoots() {
        return boots;
    }

    public void setBoots(String boots) {
        this.boots = boots;
    }
}
