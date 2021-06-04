package com.skyblockplus.utils.structs;

public class SkillsStruct {

	public final String skillName;
	public final int skillLevel;
	public final int maxSkillLevel;
	public final long totalSkillExp;
	public final long expCurrent;
	public final long expForNext;
	public final double progressToNext;

	public SkillsStruct(
		String skillName,
		int skillLevel,
		int maxSkillLevel,
		long totalSkillExp,
		long expCurrent,
		long expForNext,
		double progressToNext
	) {
		this.skillName = skillName;
		this.skillLevel = skillLevel;
		this.maxSkillLevel = maxSkillLevel;
		this.totalSkillExp = totalSkillExp;
		this.expCurrent = expCurrent;
		this.expForNext = expForNext;
		this.progressToNext = progressToNext;
	}

	@Override
	public String toString() {
		return "SkillsStruct{" +
				"skillName='" + skillName + '\'' +
				", skillLevel=" + skillLevel +
				", maxSkillLevel=" + maxSkillLevel +
				", totalSkillExp=" + totalSkillExp +
				", expCurrent=" + expCurrent +
				", expForNext=" + expForNext +
				", progressToNext=" + progressToNext +
				'}';
	}
}
