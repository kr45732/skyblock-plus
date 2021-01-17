package com.SkyblockBot.Skills;

public class SkillsStruct {
    public String skillName;
    public int skillLevel;
    public int maxSkillLevel;
    public long totalSkillExp;
    public long expCurrent;
    public long expForNext;
    public double progressToNext;

    public SkillsStruct(String skillName, int skillLevel, int maxSkillLevel, long totalSkillExp, long expCurrent,
            long expForNext, double progressToNext) {
        this.skillName = skillName;
        this.skillLevel = skillLevel;
        this.maxSkillLevel = maxSkillLevel;
        this.totalSkillExp = totalSkillExp;
        this.expCurrent = expCurrent;
        this.expForNext = expForNext;
        this.progressToNext = progressToNext;
    }
}
