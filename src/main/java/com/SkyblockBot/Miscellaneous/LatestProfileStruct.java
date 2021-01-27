package com.SkyblockBot.Miscellaneous;

import com.google.gson.JsonElement;

public class LatestProfileStruct {
    public String profileName;
    public String profileID;
    public JsonElement currentProfile;

    public LatestProfileStruct(String profileName, String profileID) {
        this.profileName = profileName;
        this.profileID = profileID;
    }

    public LatestProfileStruct(String profileName, JsonElement currentProfile) {
        this.profileName = profileName;
        this.currentProfile = currentProfile;
    }
}
