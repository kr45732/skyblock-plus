package com.skyblockplus.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.skills.SkillsStruct;
import com.skyblockplus.weight.Weight;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

import static com.skyblockplus.utils.Utils.*;

public class Player {
    public String invMissing = "";
    private boolean validPlayer = false;
    private JsonElement profileJson;
    private JsonElement levelTables;
    private JsonElement outerProfileJson;
    private JsonElement petLevelTable;
    private JsonElement hypixelProfileJson;
    private String playerUuid;
    private String playerUsername;
    private String profileName;
    private String playerGuildRank;

    public Player(String username) {
        if (usernameToUuid(username)) {
            return;
        }

        try {
            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();

            if (getLatestProfile(profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public Player(String playerUuid, JsonElement levelTables, String playerGuildRank) {
        this.playerUuid = playerUuid;
        this.playerUsername = uuidToUsername(playerUuid);
        this.levelTables = levelTables;
        this.playerGuildRank = playerGuildRank;

        try {
            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();

            if (getLatestProfile(profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public Player(String username, String profileName) {
        if (usernameToUuid(username)) {
            return;
        }

        try {

            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();
            if (!profileIdFromName(profileName, profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public boolean usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            this.playerUsername = higherDepth(usernameJson, "name").getAsString();
            this.playerUuid = higherDepth(usernameJson, "id").getAsString();
            return false;
        } catch (Exception ignored) {
        }
        return true;

    }

    public boolean isValid() {
        return validPlayer;
    }

    public int getSlayer() {
        return getWolfXp() + getZombieXp() + getSpiderXp();
    }

    public int getSlayer(String slayerName) {
        switch (slayerName) {
            case "sven":
                return getWolfXp();
            case "rev":
                return getZombieXp();
            case "tara":
                return getSpiderXp();
        }
        return -1;
    }

    public int getWolfXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : 0;
    }

    public int getZombieXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : 0;
    }

    public int getSpiderXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : 0;
    }

    public double getCatacombsLevel() {
        SkillsStruct catacombsInfo = getCatacombsSkill();
        if (catacombsInfo != null) {
            return catacombsInfo.skillLevel + catacombsInfo.progressToNext;
        }
        return 0;
    }

    public SkillsStruct getCatacombsSkill() {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        try {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(profileJson, "dungeons"), "dungeon_types"), "catacombs"),
                    "experience").getAsDouble();
            return skillInfoFromExp(skillExp, "catacombs");
        } catch (Exception e) {
            return null;
        }

    }

    public SkillsStruct getSkill(String skillName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        try {
            double skillExp = higherDepth(profileJson, "experience_skill_" + skillName).getAsDouble();
            return skillInfoFromExp(skillExp, skillName);
        } catch (Exception ignored) {
        }
        return null;
    }

    public double getSkillAverage() {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

        List<String> skills = getJsonKeys(skillsCap);
        skills.remove("catacombs");
        skills.remove("runecrafting");
        skills.remove("carpentry");

        double progressSA = 0;
        for (String skill : skills) {
            try {
                double skillExp = higherDepth(profileJson, "experience_skill_" + skill).getAsDouble();
                SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
                progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
            } catch (Exception ignored) {
            }
        }
        progressSA /= skills.size();
        return progressSA;
    }

    public SkillsStruct skillInfoFromExp(double skillExp, String skill) {
        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

        JsonArray skillsTable;
        if (skill.equals("catacombs")) {
            skillsTable = higherDepth(levelTables, "catacombs").getAsJsonArray();
        } else if (skill.equals("runecrafting")) {
            skillsTable = higherDepth(levelTables, "runecrafting_xp").getAsJsonArray();
        } else {
            skillsTable = higherDepth(levelTables, "leveling_xp").getAsJsonArray();
        }
        int maxLevel;
        try {
            maxLevel = higherDepth(skillsCap, skill).getAsInt();
        } catch (Exception e) {
            maxLevel = 50;
        }

        if (skill.equals("farming")) {
            maxLevel += getFarmingCapUpgrade();
        }

        long xpTotal = 0L;
        int level = 1;
        for (int i = 0; i < maxLevel; i++) {
            xpTotal += skillsTable.get(i).getAsLong();

            if (xpTotal > skillExp) {
                xpTotal -= skillsTable.get(i).getAsLong();
                break;
            } else {
                level = (i + 1);
            }
        }

        long xpCurrent = (long) Math.floor(skillExp - xpTotal);
        long xpForNext = 0;
        if (level < maxLevel)
            xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());

        double progress = xpForNext > 0 ? Math.max(0, Math.min(((double) xpCurrent) / xpForNext, 1)) : 0;

        return new SkillsStruct(skill, level, maxLevel, (long) skillExp, xpCurrent, xpForNext, progress);
    }

    public double getBankBalance() {
        try {
            return higherDepth(higherDepth(outerProfileJson, "banking"), "balance").getAsDouble();
        } catch (Exception e) {
            return -1;
        }
    }

    public int getFairySouls() {
        try {
            return higherDepth(profileJson, "fairy_souls_collected").getAsInt();
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean getLatestProfile(JsonArray profilesArray) {
        try {
            String lastProfileSave = "";
            for (int i = 0; i < profilesArray.size(); i++) {
                String lastSaveLoop;
                try {
                    lastSaveLoop = higherDepth(
                            higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid), "last_save")
                            .getAsString();
                } catch (Exception e) {
                    continue;
                }

                if (i == 0) {
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    this.outerProfileJson = profilesArray.get(i);
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                } else if (Instant.ofEpochMilli(Long.parseLong(lastSaveLoop))
                        .isAfter(Instant.ofEpochMilli(Long.parseLong(lastProfileSave)))) {
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    this.outerProfileJson = profilesArray.get(i);
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                }
            }
            return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
        try {
            for (int i = 0; i < profilesArray.size(); i++) {
                String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                if (currentProfileName.equalsIgnoreCase(profileName)) {
                    this.profileName = currentProfileName;
                    this.outerProfileJson = profilesArray.get(i);
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    return true;
                }
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    public String getUsername() {
        return this.playerUsername;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getUuid() {
        return this.playerUuid;
    }

    public String getGuildRank() {
        return playerGuildRank;
    }

    public JsonArray getPets() {
        return higherDepth(profileJson, "pets").getAsJsonArray();
    }

    public int petLevelFromXp(long petExp, String rarity) {
        if (this.petLevelTable == null) {
            this.petLevelTable = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/pets.json");
        }

        int petRarityOffset = higherDepth(higherDepth(petLevelTable, "pet_rarity_offset"), rarity.toUpperCase())
                .getAsInt();
        JsonArray petLevelsXpPer = higherDepth(petLevelTable, "pet_levels").getAsJsonArray();
        long totalExp = 0;
        for (int i = petRarityOffset; i < petLevelsXpPer.size(); i++) {
            totalExp += petLevelsXpPer.get(i).getAsLong();
            if (totalExp >= petExp) {
                return (i - petRarityOffset + 1);
            }
        }
        return 100;
    }

    public int getNumberMinionSlots() {
        try {
            int[] craftedMinionsToSlots = new int[]{0, 5, 15, 30, 50, 75, 100, 125, 150, 175, 200, 225, 250, 275, 300,
                    350, 400, 450, 500, 550, 600};

            int prevMax = 0;
            int craftedMinions = higherDepth(profileJson, "crafted_generators").getAsJsonArray().size();
            for (int i = 0; i < craftedMinionsToSlots.length; i++) {
                if (craftedMinions >= craftedMinionsToSlots[i]) {
                    prevMax = i;
                } else {
                    break;
                }
            }

            return (prevMax + 5);
        } catch (Exception e) {
            return 0;
        }
    }

    public double getPurseCoins() {
        try {
            return higherDepth(profileJson, "coin_purse").getAsLong();
        } catch (Exception e) {
            return -1;
        }
    }

    public int getSlayerLevel(String slayerName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        switch (slayerName) {
            case "sven":
                JsonArray wolfLevelArray = higherDepth(higherDepth(levelTables, "slayer_xp"), "wolf").getAsJsonArray();
                int wolfXp = getWolfXp();
                int prevWolfLevel = 0;
                for (int i = 0; i < wolfLevelArray.size(); i++) {
                    if (wolfXp >= wolfLevelArray.get(i).getAsInt()) {
                        prevWolfLevel = i;
                    } else {
                        break;
                    }
                }
                return (prevWolfLevel + 1);
            case "rev":
                JsonArray zombieLevelArray = higherDepth(higherDepth(levelTables, "slayer_xp"), "zombie")
                        .getAsJsonArray();
                int zombieXp = getZombieXp();
                int prevZombieMax = 0;
                for (int i = 0; i < zombieLevelArray.size(); i++) {
                    if (zombieXp >= zombieLevelArray.get(i).getAsInt()) {
                        prevZombieMax = i;
                    } else {
                        break;
                    }
                }
                return (prevZombieMax + 1);
            case "tara":
                JsonArray spiderLevelArray = higherDepth(higherDepth(levelTables, "slayer_xp"), "spider")
                        .getAsJsonArray();
                int spiderXp = getSpiderXp();
                int prevSpiderMax = 0;
                for (int i = 0; i < spiderLevelArray.size(); i++) {
                    if (spiderXp >= spiderLevelArray.get(i).getAsInt()) {
                        prevSpiderMax = i;
                    } else {
                        break;
                    }
                }
                return (prevSpiderMax + 1);
        }
        return 0;
    }

    public JsonElement getProfileJson() {
        return profileJson;
    }

    public Map<Integer, ArmorStruct> getWardrobe() {
        try {
            String encodedWardrobeContents = higherDepth(higherDepth(profileJson, "wardrobe_contents"), "data")
                    .getAsString();
            int equippedSlot = higherDepth(profileJson, "wardrobe_equipped_slot").getAsInt();
            NBTCompound decodedWardrobeContents = NBTReader.readBase64(encodedWardrobeContents);

            NBTList wardrobeFrames = decodedWardrobeContents.getList(".i");
            Map<Integer, String> wardrobeFramesMap = new HashMap<>();
            for (int i = 0; i < wardrobeFrames.size(); i++) {
                NBTCompound displayName = wardrobeFrames.getCompound(i).getCompound("tag.display");
                if (displayName != null) {
                    wardrobeFramesMap.put(i,
                            displayName.getString("Name", "Empty").replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|§7", ""));
                } else {
                    wardrobeFramesMap.put(i, "Empty");
                }
            }

            Map<Integer, ArmorStruct> armorStructMap = new HashMap<>(18);
            for (int i = 0; i < 9; i++) {
                ArmorStruct pageOneStruct = new ArmorStruct();
                for (int j = i; j < wardrobeFramesMap.size() / 2; j += 9) {
                    String currentArmorPiece = wardrobeFramesMap.get(j);
                    if ((j - i) / 9 == 0) {
                        pageOneStruct.setHelmet(currentArmorPiece);
                    } else if ((j - i) / 9 == 1) {
                        pageOneStruct.setChestplate(currentArmorPiece);
                    } else if ((j - i) / 9 == 2) {
                        pageOneStruct.setLeggings(currentArmorPiece);
                    } else if ((j - i) / 9 == 3) {
                        pageOneStruct.setBoots(currentArmorPiece);
                    }
                }
                armorStructMap.put(i, pageOneStruct);

                ArmorStruct pageTwoStruct = new ArmorStruct();
                for (int j = (wardrobeFramesMap.size() / 2) + i; j < wardrobeFramesMap.size(); j += 9) {
                    String currentArmorPiece = wardrobeFramesMap.get(j);
                    if ((j - i) / 9 == 4) {
                        pageTwoStruct.setHelmet(currentArmorPiece);
                    } else if ((j - i) / 9 == 5) {
                        pageTwoStruct.setChestplate(currentArmorPiece);
                    } else if ((j - i) / 9 == 6) {
                        pageTwoStruct.setLeggings(currentArmorPiece);
                    } else if ((j - i) / 9 == 7) {
                        pageTwoStruct.setBoots(currentArmorPiece);
                    }
                }
                armorStructMap.put(i + 9, pageTwoStruct);
            }
            if (equippedSlot > 0) {
                armorStructMap.replace((equippedSlot - 1), getInventoryArmor().makeBold());
            }

            return armorStructMap;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<Integer, String> getTalismanBagMap() {
        try {
            String encodedTalismanContents = higherDepth(higherDepth(profileJson, "talisman_bag"), "data")
                    .getAsString();
            NBTCompound decodedTalismanContents = NBTReader.readBase64(encodedTalismanContents);

            NBTList talismanFrames = decodedTalismanContents.getList(".i");
            Map<Integer, String> talismanFramesMap = new HashMap<>();
            for (int i = 0; i < talismanFrames.size(); i++) {
                NBTCompound displayName = talismanFrames.getCompound(i).getCompound("tag.display");
                if (displayName != null) {
                    talismanFramesMap.put(i,
                            displayName.getString("Name", "Empty").replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|", ""));
                } else {
                    talismanFramesMap.put(i, "Empty");
                }
            }

            return talismanFramesMap;

        } catch (Exception e) {
            return null;
        }
    }

    public ArmorStruct getInventoryArmor() {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "inv_armor"), "data").getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList talismanFrames = decodedInventoryContents.getList(".i");

            Map<Integer, String> armorFramesMap = new HashMap<>();
            for (int i = 0; i < talismanFrames.size(); i++) {
                NBTCompound displayName = talismanFrames.getCompound(i).getCompound("tag.display");
                if (displayName != null) {
                    armorFramesMap.put(i,
                            displayName.getString("Name", "Empty").replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|", ""));
                } else {
                    armorFramesMap.put(i, "Empty");
                }
            }
            return new ArmorStruct(armorFramesMap.get(3), armorFramesMap.get(2), armorFramesMap.get(1),
                    armorFramesMap.get(0));

        } catch (Exception e) {
            return null;
        }
    }

    public String[] getInventory() {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "inv_contents"), "data")
                    .getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList invFrames = decodedInventoryContents.getList(".i");
            Map<Integer, String> invFramesMap = new TreeMap<>();
            for (int i = 0; i < invFrames.size(); i++) {
                NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
                if (displayName != null) {
                    invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
                } else {
                    invFramesMap.put(i + 1, "empty");
                }
            }

            StringBuilder outputStringPart1 = new StringBuilder();
            StringBuilder outputStringPart2 = new StringBuilder();
            StringBuilder curNine = new StringBuilder();
            for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
                if (i.getKey() <= 9 || i.getKey() >= 28) {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart1.insert(0, curNine + "\n");
                        curNine = new StringBuilder();
                    }
                } else {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart2.append(curNine).append("\n");
                        curNine = new StringBuilder();
                    }
                }
            }
            return new String[]{outputStringPart2.toString(), outputStringPart1.toString()};

        } catch (Exception ignored) {
        }
        return null;
    }

    public String itemToEmoji(String itemName) {
        Map<String, String> emojiMap = new HashMap<>();
        emojiMap.put("empty", "<:empty:814669776201711637>");
        emojiMap.put("hyperion", "<:hyperion:814675220455096321>");
        emojiMap.put("rogue_sword", "<:rogue_sword:814675777479114803>");
        emojiMap.put("grappling_hook", "<:grappling_hook:814676007118176257>");
        emojiMap.put("runaans_bow", "<:runaans_bow:814676456897511424>");
        emojiMap.put("overflux_power_orb", "<:overflux_power_orb:814676605514678292>");
        emojiMap.put("superboom_tnt", "<:superboom_tnt:814690915318235146>");
        emojiMap.put("spirit_leap", "<:spirit_leap:814677057107263508>");
        emojiMap.put("skyblock_menu", "<:skyblock_menu:814676947602374698>");
        emojiMap.put("greater_backpack", "<:greater_backpack:814679082667081769>");
        emojiMap.put("dungeon_stone", "<:dungeon_stone:814680424994570291>");
        emojiMap.put("defuse_kit", "<:defuse_kit:814680645724012597>");
        emojiMap.put("rabbit_hat", "<:rabbit_hat:814680929117011988>");
        emojiMap.put("jerry_staff", "<:jerry_staff:814681305488818197>");
        emojiMap.put("flower_of_truth", "<:flower_of_truth:814687420413902849>");
        emojiMap.put("bone_boomerang", "<:bone_boomerang:814687704104435732>");
        emojiMap.put("snow_block", "<:snow_block:814690652569993248>");
        emojiMap.put("auger_rod", "<:auger_rod:814688099044687872>");
        emojiMap.put("death_bow", "<:death_bow:814688302707769354>");
        emojiMap.put("diver_fragment", "<:diver_fragment:814688560816324639>");
        emojiMap.put("blue_ice_hunk", "<:blue_ice_hunk:814688991769133087>");
        emojiMap.put("aspect_of_the_end", "<:aspect_of_the_end:814689179110735902>");
        emojiMap.put("enchanted_book", "<:enchanted_book:814689302960930826>");
        emojiMap.put("ice_hunk", "<:ice_hunk:814689461307703363>");
        emojiMap.put("golden_apple", "<:golden_apple:814689788359082004>");
        emojiMap.put("stonk_pickaxe", "<:stonk_pickaxe:814689918311596044>");
        emojiMap.put("white_gift", "<:white_gift:814690119591919696>");
        emojiMap.put("item_spirit_bow", "<:item_spirit_bow:815283134416551997>");
        emojiMap.put("ice_spray_wand", "<:ice_spray_wand:815283295896993813>");
        emojiMap.put("jumbo_backpack", "<:jumbo_backpack:815283480476254219>");
        emojiMap.put("kismet_feather", "<:kismet_feather:815284259517759508>");
        emojiMap.put("wither_cloak", "<:wither_cloak:815283770189021204>");
        emojiMap.put("dungeon_chest_key", "<:dungeon_chest_key:815284007767769100>");
        emojiMap.put("florid_zombie_sword", "<:florid_zombie_sword:815284420567236649>");
        emojiMap.put("medium_backpack", "<:medium_backpack:815284582353731635>");
        emojiMap.put("phantom_rod", "<:phantom_rod:815284719868182568>");
        emojiMap.put("fel_pearl", "<:fel_pearl:815286137576488980>");
        emojiMap.put("holy_fragment", "<:holy_fragment:815285660214493184>");
        emojiMap.put("unstable_fragment", "<:unstable_fragment:815285660264038400>");
        emojiMap.put("young_fragment", "<:young_fragment:815285660230352897>");
        emojiMap.put("enchanted_bone", "<:enchanted_bone:815286708764934214>");
        emojiMap.put("enchanted_rotten_flesh", "<:enchanted_rotten_flesh:815287217316560896>");
        emojiMap.put("training_weights", "<:training_weights:815287498560503820>");
        emojiMap.put("beastmaster_crest_rare", "<:beastmaster_crest_rare:815288186170245140>");
        emojiMap.put("enchanted_ice", "<:enchanted_ice:815288002649522217>");
        emojiMap.put("zombie_knight_helmet", "<:zombie_knight_helmet:815300154311049236>");
        emojiMap.put("earth_shard", "<:earth_shard:815300345345474563>");
        emojiMap.put("pumpkin_dicer", "<:pumpkin_dicer:815300807980220436>");
        emojiMap.put("infinite_superboom_tnt", "<:infinite_superboom_tnt:815305646194688000>");
        emojiMap.put("sniper_bow", "<:sniper_bow:816310296309137428>");
        emojiMap.put("thorns_boots", "<:spirit_boots:816310296217518131>");
        emojiMap.put("shadow_fury", "<:shadow_fury:816310296179245076>");
        emojiMap.put("skeleton_master_boots", "<:skeleton_master_boots:816310296096276550>");
        emojiMap.put("machine_gun_bow", "<:machine_gun_bow:816310296053678141>");
        emojiMap.put("beastmaster_crest_common", "<:beastmaster_crest_common:816310296045813770>");
        emojiMap.put("last_breath", "<:last_breath:816310296041357342>");
        emojiMap.put("beastmaster_crest_uncommon", "<:beastmaster_crest_uncommon:816310296037163068>");
        emojiMap.put("beastmaster_crest_legendary", "<:beastmaster_crest_legendary:816310296032313344>");
        emojiMap.put("crypt_bow", "<:crypt_bow:816310296011735050>");
        emojiMap.put("crypt_dreadlord_sword", "<:crypt_dreadlord_sword:816310295990370304>");
        emojiMap.put("beastmaster_crest_epic", "<:beastmaster_crest_epic:816310295907270698>");
        emojiMap.put("skeleton_master_helmet", "<:skeleton_master_helmet:816310295785242676>");
        emojiMap.put("bonzo_mask", "<:bonzo_mask:816132749982171156>");
        emojiMap.put("broken_piggy_bank", "<:broken_piggy_bank:816129549536329738>");
        emojiMap.put("cracked_piggy_bank", "<:cracked_piggy_bank:816129538601779250>");
        emojiMap.put("piggy_bank", "<:piggy_bank:816129528224546826>");
        emojiMap.put("sword_of_revelations", "<:sword_of_revelations:815310159617589288>");
        emojiMap.put("shaman_sword", "<:shaman_sword:815309622658465882>");
        emojiMap.put("adaptive_blade", "<:adaptive_blade:815309431038410833>");
        emojiMap.put("raider_axe", "<:raider_axe:815308927020564491>");
        emojiMap.put("wither_boots", "<:wither_boots:815308403219365929>");
        emojiMap.put("soul_whip", "<:soul_whip:815307925052457010>");
        emojiMap.put("rotten_leggings", "<:rotten_leggings:815307870937284699>");
        emojiMap.put("super_heavy_chestplate", "<:super_heavy_chestplate:815307825504976916>");
        emojiMap.put("large_backpack", "<:large_backpack:815299347822149632>");
        emojiMap.put("midas_sword", "<:midas_sword:815307715467018242>");
        emojiMap.put("bonzo_staff", "<:bonzo_staff:815307683883647076>");
        emojiMap.put("flaming_sword", "<:flaming_sword:815307168109953024>");
        emojiMap.put("midas_staff", "<:midas_staff:815306843751448597>");
        emojiMap.put("hunter_knife", "<:hunter_knife:815306466596356137>");
        emojiMap.put("fancy_sword", "<:fancy_sword:815306055533461544>");
        emojiMap.put("sorrow", "<:sorrow:816324126956453929>");
        emojiMap.put("plasma", "<:plasma:816324127412977694>");
        emojiMap.put("volta", "<:volta:816324127307595797>");
        emojiMap.put("bag_of_cash", "<:bag_of_cash:816324127253332038>");
        emojiMap.put("ancient_claw", "<:ancient_claw:816324552405549117>");
        emojiMap.put("summoning_ring", "<:summoning_ring:816433732331241483>");
        emojiMap.put("zombie_knight_sword", "<:zombie_knight_sword:816433732331634719>");
        emojiMap.put("enchanted_iron", "<:enchanted_iron:816433732587225158>");
        emojiMap.put("enchanted_ancient_claw", "<:enchanted_ancient_claw:816433732624842842>");
        emojiMap.put("griffin_feather", "<:griffin_feather:816433732624973884>");
        emojiMap.put("potion", "<:potion:816433732688281650>");
        emojiMap.put("enchanted_gold", "<:enchanted_gold:816433732721573938>");
        emojiMap.put("emerald_blade", "<:emerald_blade:816433732763910166>");
        emojiMap.put("ancestral_spade", "<:ancestral_spade:816433732969693214>");
        emojiMap.put("magma_bow", "<:magma_bow:816435478076850187>");
        emojiMap.put("end_stone_sword", "<:end_stone_sword:816435478223519766>");
        emojiMap.put("gold_axe", "<:gold_axe:816435478471114773>");
        emojiMap.put("aatrox_batphone", "<:aatrox_batphone:816435478580428841>");
        emojiMap.put("giants_sword", "<:giants_sword:816435478609133619>");
        emojiMap.put("rod_of_the_sea", "<:rod_of_the_sea:816438156445745203>");
        emojiMap.put("theoretical_hoe_warts_3", "<:theoretical_hoe_warts_3:816438156680626176>");
        emojiMap.put("treecapitator_axe", "<:treecapitator:816438156873302086>");
        emojiMap.put("theoretical_hoe_cane_3", "<:theoretical_hoe_cane_3:816438156953518150>");
        emojiMap.put("fel_sword", "<:fel_sword:816442343816822785>");
        emojiMap.put("jingle_bells", "<:jingle_bells:816442343900446731>");
        emojiMap.put("aspect_of_the_dragon", "<:aspect_of_the_dragon:816442343922204674>");
        emojiMap.put("gift_compass", "<:gift_compass:816442344014348300>");
        emojiMap.put("magical_bucket", "<:magical_bucket:816442344055242773>");
        emojiMap.put("mana_flux_power_orb", "<:mana_flux_power_orb:816442344060092477>");
        emojiMap.put("sea_lantern", "<:sea_lantern:816442344093646849>");
        emojiMap.put("valkyrie", "<:valkyrie:816442344260763649>");
        emojiMap.put("livid_dagger", "<:livid_dagger:816442344264957952>");
        emojiMap.put("wise_wither_chestplate", "<:storm_chestplate:816442344265744395>");
        emojiMap.put("diamond_pickaxe", "<:diamond_pickaxe:816442344269414421>");
        emojiMap.put("rune", "<:rune:816442344320532532>");
        emojiMap.put("wither_goggles", "<:wither_goggles:816442344382922783>");
        emojiMap.put("skeleton_master_leggings", "<:skeleton_master_leggings:816442344387641385>");
        emojiMap.put("wise_wither_leggings", "<:storm_leggings:816442344459075584>");
        emojiMap.put("personal_compactor_7000", "<:personal_compactor_7000:816442344462090281>");
        emojiMap.put("wise_wither_helmet", "<:storm_helmet:816442344525791253>");
        emojiMap.put("wise_wither_boots", "<:storm_boots:816442344534310963>");
        emojiMap.put("slime_hat", "<:slime_hat:816442344559083580>");
        emojiMap.put("tarantula_boots", "<:tarantula_boots:816442344572190760>");
        emojiMap.put("zombie_soldier_boots", "<:zombie_soldier_boots:816442344617934868>");
        emojiMap.put("shadow_assassin_helmet", "<:shadow_assassin_helmet:816444761559793694>");
        emojiMap.put("wither_catalyst", "<:wither_catalyst:816444761493864448>");
        emojiMap.put("zombie_soldier_chestplate", "<:zombie_soldier_chestplate:816445031946780693>");
        emojiMap.put("cake_soul", "<:cake_soul:816445265480777818>");
        emojiMap.put("power_wither_helmet", "<:necron_helmet:816450192366370866>");
        emojiMap.put("tank_wither_helmet", "<:goldor_helmet:816450192198991872>");
        emojiMap.put("mimic_fragment", "<:mimic_fragment:816452177928519709>");
        emojiMap.put("reaper_sword", "<:reaper_falchion:816452177882775552>");
        emojiMap.put("speed_wither_chestplate", "<:maxor_chestplate:816450192131227679>");
        emojiMap.put("builders_wand", "<:builders_wand:816452177487724575>");
        emojiMap.put("zombie_soldier_cutlass", "<:zombie_soldier_cutlass:816450973651304448>");
        emojiMap.put("tank_wither_chestplate", "<:goldor_chestplate:816450192139223080>");
        emojiMap.put("power_wither_leggings", "<:necron_leggings:816450192084566037>");
        emojiMap.put("speed_wither_leggings", "<:maxor_leggings:816450191904473118>");
        emojiMap.put("personal_compactor_6000", "<:personal_compactor_6000:816452177588650025>");
        emojiMap.put("speed_wither_helmet", "<:maxor_helmet:816450192487350279>");
        emojiMap.put("skeleton_soldier_helmet", "<:skeleton_soldier_helmet:816452177794826291>");
        emojiMap.put("tank_wither_leggings", "<:goldor_leggings:816450192336879669>");
        emojiMap.put("tank_wither_boots", "<:goldor_boots:816450192203055137>");
        emojiMap.put("remnant_of_the_eye", "<:remnant_of_the_eye:816438156840271953>");
        emojiMap.put("speed_wither_boots", "<:maxor_boots:816450192240672828>");
        emojiMap.put("power_wither_chestplate", "<:necron_chestplate:816450192285892608>");
        emojiMap.put("power_wither_boots", "<:necron_boots:816450192270163989>");
        emojiMap.put("wither_blood", "<:wither_blood:816450973681320006>");
        emojiMap.put("ornate_zombie_sword", "<:ornatezombiesword:816452177567678475>");
        emojiMap.put("blank", "<:blank:817050888186101761>");

        emojiMap.put("goblin_egg", "<:goblin_egg:816457683606437918>");
        emojiMap.put("leaping_sword", "<:leaping_sword:816457683237470209>");
        emojiMap.put("healing_ring", "<:healing_ring:817495522372747336>");
        emojiMap.put("speed_artifact", "<:speed_artifact:817495522213625920>");
        emojiMap.put("night_vision_charm", "<:night_vision_charm:817495522133803010>");
        emojiMap.put("cheetah_talisman", "<:cheetah_talisman:817495522201960470>");
        emojiMap.put("red_claw_artifact", "<:red_claw_artifact:817495522410627072>");
        emojiMap.put("artifact_potion_affinity", "<:potion_artifact:817495522192392224>");
        emojiMap.put("green_gift", "<:green_gift:816457683636322324>");
        emojiMap.put("healing_talisman", "<:healing_talisman:817495522096316497>");
        emojiMap.put("treasure_ring", "<:treasure_ring:817495522775793674>");
        emojiMap.put("wand_of_healing", "<:wand_of_healing:816457683862945793>");
        emojiMap.put("spider_artifact", "<:spider_artifact:817495522570272768>");
        emojiMap.put("enchanted_titanium", "<:enchanted_titanium:816457683703300168>");
        emojiMap.put("farming_talisman", "<:farming_talisman:817495522407481414>");
        emojiMap.put("skeleton_talisman", "<:skeleton_talisman:817495522390442025>");
        emojiMap.put("lava_talisman", "<:lava_talisman:817495522457812992>");
        emojiMap.put("fractured_mithril_pickaxe", "<a:fractured_mithril_pickaxe:816455519106629652>");
        emojiMap.put("auto_recombobulator", "<:auto_recombobulator:817495522369601627>");
        emojiMap.put("snow_cannon", "<:snow_cannon:816457683775258674>");
        emojiMap.put("shady_ring", "<:shady_ring:817495522427797504>");
        emojiMap.put("enchanted_slime_ball", "<:enchanted_slime_ball:816458357333557248>");
        emojiMap.put("sea_creature_talisman", "<:sea_creature_talisman:817495522520727582>");
        emojiMap.put("red_claw_talisman", "<:red_claw_talisman:817495522427797514>");
        emojiMap.put("red_gift", "<:red_gift:816457683737640980>");
        emojiMap.put("survivor_cube", "<:survivor_cube:817495522662154240>");
        emojiMap.put("eternal_hoof", "<:eternal_hoof:817495522390048768>");
        emojiMap.put("potion_ring", "<:potion_ring:817495522385461268>");
        emojiMap.put("slime_ball", "<:slime_ball:816458704768729088>");
        emojiMap.put("radiant_power_orb", "<:radiant_power_orb:816457683699630150>");
        emojiMap.put("grand_exp_bottle", "<:grand_exp_bottle:816457683695042580>");
        emojiMap.put("haste_ring", "<:haste_ring:817495522369470484>");
        emojiMap.put("voodoo_doll", "<:voodoo_doll:816457683589660694>");
        emojiMap.put("speed_talisman", "<:speed_talisman:817495522611822612>");
        emojiMap.put("treasure_artifact", "<:treasure_artifact:817495522704752671>");
        emojiMap.put("hegemony_artifact", "<:hegemony_artifact:817495522389655602>");
        emojiMap.put("mithril_ore", "<:mithril_ore:816457683615612989>");
        emojiMap.put("scavenger_talisman", "<:scavenger_talisman:817495522381529138>");
        emojiMap.put("sea_creature_ring", "<:sea_creature_ring:817495522260156428>");
        emojiMap.put("speed_ring", "<:speed_ring:817495522511945820>");
        emojiMap.put("sea_creature_artifact", "<:seacreature_artifact:817495522067480588>");
        emojiMap.put("glacite_jewel", "<:glacite_jewel:816457683707101234>");
        emojiMap.put("lucky_hoof", "<:lucky_hoof:817495522544713758>");
        emojiMap.put("enchanted_mithril", "<:enchanted_mithril:816457683942244412>");
        emojiMap.put("seal_of_the_family", "<:seal_of_the_family:817495522377203732>");
        emojiMap.put("experience_artifact", "<:experience_artifact:817495522075213865>");
        emojiMap.put("red_claw_ring", "<:red_claw_ring:817495521992245360>");
        emojiMap.put("spider_ring", "<:spider_ring:817495522227126293>");
        emojiMap.put("snow_blaster", "<:snow_blaster:816457683845513276>");
        emojiMap.put("potion_talisman", "<:potion_talisman:817495522373271559>");

        emojiMap.put("frozen_chicken", "<:frozen_chicken:817502293259059251>");
        emojiMap.put("wolf_talisman", "<:wolf_talisman:817502250045538335>");
        emojiMap.put("fish_affinity_talisman", "<:fishing_talisman:817502293229436939>");
        emojiMap.put("magnetic_talisman", "<:magnetic_talisman:817502250187620362>");
        emojiMap.put("wood_talisman", "<:wood_talisman:817502250331144272>");
        emojiMap.put("bat_talisman", "<:bat_talisman:817502293296676864>");
        emojiMap.put("spider_talisman", "<:spider_talisman:817502250536534036>");
        emojiMap.put("feather_artifact", "<:feather_artifact:817502293116977163>");
        emojiMap.put("personal_compactor_4000", "<:personal_compactor_4000:817502250313449502>");
        emojiMap.put("coin_talisman", "<:coin_talisman:817502293099806771>");
        emojiMap.put("bits_talisman", "<:bits_talisman:817502293347663922>");
        emojiMap.put("candy_artifact", "<:candy_artifact:817502293317648395>");
        emojiMap.put("bait_ring", "<:bait_ring:817502293280161869>");
        emojiMap.put("wither_artifact", "<:wither_artifact:817502250410049566>");
        emojiMap.put("vaccine_talisman", "<:vaccine_talisman:817502250283827200>");
        emojiMap.put("devour_ring", "<:devour_ring:817502293347926036>");
        emojiMap.put("bat_artifact", "<:bat_artifact:817502293086830614>");
        emojiMap.put("fire_talisman", "<:fire_talisman:817502293489745950>");
        emojiMap.put("potato_talisman", "<:potato_talisman:817502250255515668>");
        emojiMap.put("emerald_ring", "<:emerald_ring:817502293305851945>");
        emojiMap.put("hunter_ring", "<:hunter_ring:817502293095088189>");
        emojiMap.put("tarantula_talisman", "<:tarantula_talisman:817502250082762763>");
        emojiMap.put("titanium_artifact", "<:titanium_artifact:817502250334683136>");
        emojiMap.put("hunter_talisman", "<:hunter_talisman:817502250237821011>");
        emojiMap.put("zombie_talisman", "<:zombie_talisman:817502250279632937>");
        emojiMap.put("feather_ring", "<:feather_ring:817502293472575576>");
        emojiMap.put("gravity_talisman", "<:gravity_talisman:817502293544927294>");
        emojiMap.put("zombie_artifact", "<:zombie_artifact:817502250427482152>");
        emojiMap.put("personal_compactor_5000", "<:personal_compactor_5000:817502250309910539>");
        emojiMap.put("new_year_cake_bag", "<:cake_bag:817502293489877052>");
        emojiMap.put("village_talisman", "<:village_talisman:817502250439016458>");
        emojiMap.put("candy_relic", "<:candy_relic:817502292953399357>");
        emojiMap.put("crooked_ring", "<:crooked_ring:817502293410709524>");
        emojiMap.put("wolf_paw", "<:wolf_paw:817502250439016448>");
        emojiMap.put("intimidation_artifact", "<:intimidation_artifact:817502250150658098>");
        emojiMap.put("wolf_ring", "<:wolf_ring:817502250242015252>");
        emojiMap.put("catacombs_expert_ring", "<:catacombs_expert_ring:817502293501935618>");
        emojiMap.put("zombie_ring", "<:zombie_ring:817502250037411871>");
        emojiMap.put("ender_artifact", "<:ender_artifact:817502293817557012>");
        emojiMap.put("feather_talisman", "<:feather_talisman:817502293104525393>");
        emojiMap.put("intimidation_ring", "<:intimidation_ring:817502250117103637>");
        emojiMap.put("candy_ring", "<:candy_ring:817502293321580624>");
        emojiMap.put("bat_ring", "<:bat_ring:817502293435088966>");
        emojiMap.put("intimidation_talisman", "<:intimidation_talisman:817502250263511061>");
        emojiMap.put("mineral_talisman", "<:mineral_talisman:817502250279895062>");
        emojiMap.put("candy_talisman", "<:candy_talisman:817502293116977203>");
        emojiMap.put("treasure_talisman", "<:treasure_talisman:817502250347790416>");
        emojiMap.put("mine_talisman", "<:mine_talisman:817502249889955912>");
        emojiMap.put("wither_relic", "<:wither_relic:817502249986293871>");
        emojiMap.put("farmer_orb", "<:farmer_orb:817502293670756362>");

        itemName = itemName.replace("starred_", "");
        if (emojiMap.containsKey(itemName)) {
            return emojiMap.get(itemName);
        }

        if (!invMissing.contains(itemName)) {
            invMissing += "\n• " + itemName;
        }
        return "❓";
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Integer> getPlayerSacks() {
        JsonElement sacksJson = higherDepth(profileJson, "sacks_counts");
        return new Gson().fromJson(sacksJson, HashMap.class);
    }

    public JsonArray getBankHistory() {
        try {
            return higherDepth(higherDepth(outerProfileJson, "banking"), "transactions").getAsJsonArray();
        } catch (Exception e) {
            return null;
        }
    }

    public int getSkillMaxLevel(String skillName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }
        if (skillName.equals("farming")) {
            return higherDepth(higherDepth(levelTables, "leveling_caps"), skillName).getAsInt()
                    + getFarmingCapUpgrade();
        }

        return higherDepth(higherDepth(levelTables, "leveling_caps"), skillName).getAsInt();
    }

    public double getSkillXp(String skillName) {
        try {
            if (skillName.equals("catacombs")) {
                return higherDepth(
                        higherDepth(higherDepth(higherDepth(profileJson, "dungeons" + skillName), "dungeon_types"),
                                "catacombs"),
                        "experience").getAsDouble();
            }
            return higherDepth(profileJson, "experience_skill_" + skillName).getAsDouble();
        } catch (Exception ignored) {
        }
        return -1;
    }

    public double getDungeonClassLevel(String className) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        SkillsStruct dungeonClassLevel = skillInfoFromExp(getDungeonClassXp(className), "catacombs");
        return dungeonClassLevel.skillLevel + dungeonClassLevel.progressToNext;
    }

    public SkillsStruct getDungeonClass(String className) {
        return skillInfoFromExp(getDungeonClassXp(className), "catacombs");
    }

    public double getDungeonClassXp(String className) {
        try {
            return higherDepth(
                    higherDepth(higherDepth(higherDepth(profileJson, "dungeons"), "player_classes"), className),
                    "experience").getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    public int getFarmingCapUpgrade() {
        try {
            return higherDepth(higherDepth(higherDepth(profileJson, "jacob2"), "perks"), "farming_level_cap")
                    .getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public double getWeight() {
        Weight playerWeight = new Weight(this);
        return playerWeight.getTotalWeight();
    }

    public Map<Integer, String[]> getInventoryItem(String itemName) {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "inv_contents"), "data")
                    .getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList invFrames = decodedInventoryContents.getList(".i");
            Map<Integer, String[]> invFramesMap = new HashMap<>();
            for (int i = 0; i < invFrames.size(); i++) {
                NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
                if (displayName != null) {
                    if (displayName.getString("id", "empty").equalsIgnoreCase(itemName)) {
                        StringBuilder loreString = new StringBuilder();
                        for (Object loreLine : invFrames.getCompound(i).getCompound("tag.display").getList("Lore")) {
                            loreString.append("\n").append(((String) loreLine).replaceAll(
                                    "§ka|§0|§1|§2|§3|§4|§5|§6|§7|§8|§9|§a|§b|§c|§d|§e|§f|§k|§l|§m|§n|§o|§r", ""));
                        }
                        invFramesMap.put(i + 1, new String[]{
                                invFrames.getCompound(i).getString("Count", "0").toLowerCase().replace("b", "") + "x",
                                loreString.toString()});
                    }
                }
            }
            return invFramesMap;

        } catch (Exception ignored) {
        }
        return null;
    }

    public int getDungeonSecrets() {
        if (hypixelProfileJson == null) {
            this.hypixelProfileJson = getJson(
                    "https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid);
        }

        try {
            return higherDepth(higherDepth(higherDepth(hypixelProfileJson, "player"), "achievements"),
                    "skyblock_treasure_hunter").getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getSelectedDungeonClass() {
        try {
            return higherDepth(higherDepth(profileJson, "dungeons"), "selected_dungeon_class").getAsString();
        } catch (Exception e) {
            return "none";
        }
    }

    public String getHyperion() {
        if (getInventory()[0].contains("hyperion") || getInventory()[1].contains("hyperion")) {
            return "yes";
        }
        return "no";
    }

    public int getBonemerang() {
        if (getInventory()[0].contains("bone_boomerang") || getInventory()[1].contains("bone_boomerang")) {
            return StringUtils.countOccurrencesOf(getInventory()[0], "bone_boomerang")
                    + StringUtils.countOccurrencesOf(getInventory()[1], "bone_boomerang");
        }
        return 0;
    }

    public List<String[]> getEnderChest() {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "ender_chest_contents"), "data")
                    .getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList invFrames = decodedInventoryContents.getList(".i");

            Map<Integer, String> invFramesMap = new TreeMap<>();
            for (int i = 0; i < invFrames.size(); i++) {
                NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
                if (displayName != null) {
                    invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
                } else {
                    invFramesMap.put(i + 1, "empty");
                }
            }

            StringBuilder outputStringPart1 = new StringBuilder();
            StringBuilder outputStringPart2 = new StringBuilder();
            List<String[]> enderChestPages = new ArrayList<>();
            StringBuilder curNine = new StringBuilder();
            int page = 0;
            for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
                if ((i.getKey() - page) <= 27) {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart1.append(curNine).append("\n");
                        curNine = new StringBuilder();
                    }
                } else {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart2.append(curNine).append("\n");
                        curNine = new StringBuilder();
                    }
                }

                if (i.getKey() != 0 && i.getKey() % 45 == 0) {
                    enderChestPages.add(new String[]{outputStringPart1.toString(), outputStringPart2.toString()});
                    outputStringPart1 = new StringBuilder();
                    outputStringPart2 = new StringBuilder();
                    page += 45;
                }
            }
            return enderChestPages;
        } catch (Exception ignored) {
        }
        return null;
    }

    public List<String[]> getTalismanBag() {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "talisman_bag"), "data")
                    .getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList invFrames = decodedInventoryContents.getList(".i");

            Map<Integer, String> invFramesMap = new TreeMap<>();
            for (int i = 0; i < invFrames.size(); i++) {
                NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
                if (displayName != null) {
                    invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
                } else {
                    invFramesMap.put(i + 1, "empty");
                }
            }

            if (invFramesMap.size() % 45 != 0) {
                int toAdd = 45 - (invFramesMap.size() % 45);
                int initialSize = invFramesMap.size();
                for (int i = 0; i < toAdd; i++) {
                    invFramesMap.put(initialSize + 1 + i, "blank");
                }
            }

            StringBuilder outputStringPart1 = new StringBuilder();
            StringBuilder outputStringPart2 = new StringBuilder();
            List<String[]> enderChestPages = new ArrayList<>();
            StringBuilder curNine = new StringBuilder();
            int page = 0;
            for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
                if ((i.getKey() - page) <= 27) {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart1.append(curNine).append("\n");
                        curNine = new StringBuilder();
                    }
                } else {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart2.append(curNine).append("\n");
                        curNine = new StringBuilder();
                    }
                }

                if (i.getKey() != 0 && i.getKey() % 45 == 0) {
                    enderChestPages.add(new String[]{outputStringPart1.toString(), outputStringPart2.toString()});
                    outputStringPart1 = new StringBuilder();
                    outputStringPart2 = new StringBuilder();
                    page += 45;
                }
            }
            return enderChestPages;
        } catch (Exception ignored) {
        }
        return null;
    }
}
