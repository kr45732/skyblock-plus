/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils;

import static com.skyblockplus.miscellaneous.BestiaryCommand.*;
import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.miscellaneous.weight.lily.LilyWeight;
import com.skyblockplus.miscellaneous.weight.senither.SenitherWeight;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.structs.*;
import java.time.Instant;
import java.util.*;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.groovy.util.Maps;

public class Player {

	public static final Map<String, String> COLLECTION_NAME_TO_ID = Maps.of(
		"cocoa_beans",
		"INK_SACK:3",
		"carrot",
		"CARROT_ITEM",
		"cactus",
		"CACTUS",
		"raw_chicken",
		"RAW_CHICKEN",
		"sugar_cane",
		"SUGAR_CANE",
		"pumpkin",
		"PUMPKIN",
		"wheat",
		"WHEAT",
		"seeds",
		"SEEDS",
		"mushroom",
		"MUSHROOM_COLLECTION",
		"raw_rabbit",
		"RABBIT",
		"nether_wart",
		"NETHER_STALK",
		"mutton",
		"MUTTON",
		"melon",
		"MELON",
		"potato",
		"POTATO_ITEM",
		"leather",
		"LEATHER",
		"raw_porkchop",
		"PORK",
		"feather",
		"FEATHER",
		"lapis_lazuli",
		"INK_SACK:4",
		"redstone",
		"REDSTONE",
		"coal",
		"COAL",
		"mycelium",
		"MYCEL",
		"end_stone",
		"ENDER_STONE",
		"nether_quartz",
		"QUARTZ",
		"sand",
		"SAND",
		"iron_ingot",
		"IRON_INGOT",
		"gemstone",
		"GEMSTONE_COLLECTION",
		"obsidian",
		"OBSIDIAN",
		"diamond",
		"DIAMOND",
		"cobblestone",
		"COBBLESTONE",
		"glowstone_dust",
		"GLOWSTONE_DUST",
		"gold_ingot",
		"GOLD_INGOT",
		"gravel",
		"GRAVEL",
		"hard_stone",
		"HARD_STONE",
		"mithril",
		"MITHRIL_ORE",
		"emerald",
		"EMERALD",
		"red_sand",
		"SAND:1",
		"ice",
		"ICE",
		"sulphur",
		"SULPHUR_ORE",
		"netherrack",
		"NETHERRACK",
		"ender_pearl",
		"ENDER_PEARL",
		"slimeball",
		"SLIME_BALL",
		"magma_cream",
		"MAGMA_CREAM",
		"ghast_tear",
		"GHAST_TEAR",
		"gunpowder",
		"SULPHUR",
		"rotten_flesh",
		"ROTTEN_FLESH",
		"spider_eye",
		"SPIDER_EYE",
		"bone",
		"BONE",
		"blaze_rod",
		"BLAZE_ROD",
		"string",
		"STRING",
		"acacia_wood",
		"LOG_2",
		"spruce_wood",
		"LOG:1",
		"jungle_wood",
		"LOG:3",
		"birch_wood",
		"LOG:2",
		"oak_wood",
		"LOG",
		"dark_oak_wood",
		"LOG_2:1",
		"lily_pad",
		"WATER_LILY",
		"prismarine_shard",
		"PRISMARINE_SHARD",
		"ink_sack",
		"INK_SACK",
		"raw_fish",
		"RAW_FISH",
		"pufferfish",
		"RAW_FISH:3",
		"clownfish",
		"RAW_FISH:2",
		"raw_salmon",
		"RAW_FISH:1",
		"magmafish",
		"MAGMA_FISH",
		"prismarine_crystals",
		"PRISMARINE_CRYSTALS",
		"clay",
		"CLAY_BALL",
		"sponge",
		"SPONGE"
	);
	public static final List<String> STATS_LIST = List.of(
		"deaths",
		"deaths_void",
		"kills",
		"kills_emerald_slime",
		"auctions_bids",
		"auctions_highest_bid",
		"kills_zombie",
		"auctions_won",
		"auctions_bought_rare",
		"auctions_gold_spent",
		"kills_chicken",
		"deaths_zombie",
		"deaths_skeleton",
		"highest_crit_damage",
		"kills_skeleton",
		"kills_spider",
		"auctions_bought_uncommon",
		"kills_diamond_skeleton",
		"kills_diamond_zombie",
		"kills_zombie_villager",
		"kills_redstone_pigman",
		"kills_invisible_creeper",
		"kills_witch",
		"items_fished",
		"items_fished_normal",
		"kills_sea_walker",
		"kills_pond_squid",
		"items_fished_large_treasure",
		"kills_sea_guardian",
		"items_fished_treasure",
		"kills_unburried_zombie",
		"deaths_unburried_zombie",
		"kills_ruin_wolf",
		"kills_horseman_zombie",
		"kills_lapis_zombie",
		"deaths_fire",
		"kills_splitter_spider",
		"kills_weaver_spider",
		"kills_voracious_spider",
		"kills_splitter_spider_silverfish",
		"kills_jockey_shot_silverfish",
		"kills_dasher_spider",
		"kills_jockey_skeleton",
		"kills_spider_jockey",
		"kills_wither_skeleton",
		"deaths_wither_skeleton",
		"kills_fireball_magma_cube",
		"kills_rabbit",
		"kills_sheep",
		"end_race_best_time",
		"deaths_fall",
		"deaths_spider",
		"kills_pig",
		"kills_cow",
		"auctions_bought_epic",
		"kills_enderman",
		"kills_random_slime",
		"kills_respawning_skeleton",
		"auctions_created",
		"auctions_fees",
		"auctions_completed",
		"auctions_sold_common",
		"auctions_gold_earned",
		"kills_watcher",
		"kills_zealot_enderman",
		"kills_obsidian_wither",
		"kills_endermite",
		"deaths_unknown",
		"auctions_sold_epic",
		"kills_bat_pinata",
		"deaths_drowning",
		"kills_blaze",
		"auctions_sold_special",
		"kills_generator_ghast",
		"kills_old_wolf",
		"auctions_bought_common",
		"deaths_wolf",
		"kills_magma_cube",
		"kills_pigman",
		"kills_pack_spirit",
		"kills_howling_spirit",
		"kills_soul_of_the_alpha",
		"kills_night_respawning_skeleton",
		"auctions_sold_rare",
		"highest_critical_damage",
		"kills_sea_archer",
		"kills_zombie_deep",
		"kills_catfish",
		"kills_chicken_deep",
		"deaths_old_wolf",
		"auctions_sold_uncommon",
		"auctions_sold_legendary",
		"ender_crystals_destroyed",
		"kills_wise_dragon",
		"kills_unstable_dragon",
		"kills_strong_dragon",
		"kills_protector_dragon",
		"gifts_received",
		"gifts_given",
		"kills_liquid_hot_magma",
		"most_winter_snowballs_hit",
		"most_winter_damage_dealt",
		"most_winter_magma_damage_dealt",
		"deaths_player",
		"deaths_liquid_hot_magma",
		"deaths_magma_cube",
		"kills_night_squid",
		"deaths_sea_leech",
		"kills_old_dragon",
		"deaths_strong_dragon",
		"deaths_superior_dragon",
		"kills_sea_leech",
		"kills_brood_mother_spider",
		"kills_brood_mother_cave_spider",
		"auctions_no_bids",
		"kills_young_dragon",
		"kills_superior_dragon",
		"auctions_bought_legendary",
		"kills_cave_spider",
		"kills_player",
		"dungeon_hub_giant_mushroom_anything_no_return_best_time",
		"dungeon_hub_precursor_ruins_anything_no_return_best_time",
		"dungeon_hub_crystal_core_anything_no_return_best_time",
		"kills_zombie_grunt",
		"kills_skeleton_grunt",
		"kills_dungeon_respawning_skeleton",
		"kills_crypt_lurker",
		"kills_crypt_dreadlord",
		"kills_crypt_tank_zombie",
		"kills_scared_skeleton",
		"kills_diamond_guy",
		"deaths_lost_adventurer",
		"kills_crypt_souleater",
		"kills_skeleton_soldier",
		"kills_crypt_undead",
		"kills_watcher_summon_undead",
		"kills_bonzo_summon_undead",
		"kills_lost_adventurer",
		"deaths_blaze",
		"deaths_enderman",
		"deaths_lapis_zombie",
		"deaths_ruin_wolf",
		"deaths_emerald_slime",
		"deaths_weaver_spider",
		"deaths_dasher_spider",
		"deaths_diamond_zombie",
		"deaths_splitter_spider",
		"deaths_splitter_spider_silverfish",
		"deaths_redstone_pigman",
		"deaths_spider_jockey",
		"deaths_diamond_skeleton",
		"deaths_fireball_magma_cube",
		"deaths_zombie_deep",
		"deaths_watcher",
		"deaths_obsidian_wither",
		"deaths_endermite",
		"kills_generator_slime",
		"kills_slime",
		"kills_ghast",
		"deaths_generator_slime",
		"deaths_zealot_enderman",
		"deaths_old_dragon",
		"deaths_wise_dragon",
		"kills_forest_island_bat",
		"kills_magma_cube_boss",
		"deaths_magma_cube_boss",
		"kills_generator_magma_cube",
		"deaths_cave_spider",
		"kills_sea_witch",
		"kills_creeper",
		"kills_guardian_defender",
		"kills_deep_sea_protector",
		"deaths_water_hydra",
		"kills_water_hydra",
		"deaths_protector_dragon",
		"chicken_race_best_time",
		"kills_frozen_steve",
		"kills_frosty_the_snowman",
		"chicken_race_best_time_2",
		"kills_guardian_emperor",
		"kills_skeleton_emperor",
		"kills_carrot_king",
		"kills_yeti",
		"deaths_yeti",
		"deaths_pack_spirit",
		"deaths_soul_of_the_alpha",
		"shredder_bait",
		"shredder_fished",
		"kills_grinch",
		"deaths_guardian_emperor",
		"auctions_bought_special",
		"foraging_race_best_time",
		"pet_milestone_sea_creatures_killed",
		"pet_milestone_ores_mined",
		"deaths_revenant_zombie",
		"kills_revenant_zombie",
		"dungeon_hub_crystal_core_no_pearls_no_return_best_time",
		"dungeon_hub_crystal_core_no_abilities_no_return_best_time",
		"kills_cellar_spider",
		"kills_sniper_skeleton",
		"deaths_watcher_summon_undead",
		"deaths_crypt_lurker",
		"kills_corrupted_protector",
		"deaths_skeleton_emperor",
		"kills_horseman_bat",
		"deaths_corrupted_protector",
		"dungeon_hub_precursor_ruins_no_pearls_no_return_best_time",
		"dungeon_hub_precursor_ruins_anything_with_return_best_time",
		"dungeon_hub_precursor_ruins_no_pearls_with_return_best_time",
		"kills_horseman_horse",
		"kills_dungeon_secret_bat",
		"kills_skeleton_master",
		"deaths_scarf_warrior",
		"kills_scarf_warrior",
		"kills_scarf_mage",
		"deaths_crypt_dreadlord",
		"deaths_skeleton_soldier",
		"kills_parasite",
		"deaths_scarf",
		"kills_lonely_spider",
		"deaths_scarf_mage",
		"kills_scarf_priest",
		"kills_blaze_higher_or_lower",
		"kills_zombie_soldier",
		"deaths_skeletor",
		"deaths_skeleton_grunt",
		"kills_scarf_archer",
		"deaths_crypt_souleater",
		"deaths_skeleton_master",
		"kills_dungeon_respawning_skeleton_skull",
		"deaths_trap",
		"kills_crypt_undead_pieter",
		"kills_crypt_undead_valentin",
		"kills_shadow_assassin",
		"kills_skeletor",
		"deaths_shadow_assassin",
		"deaths_deathmite",
		"kills_watcher_bonzo",
		"kills_professor_guardian_summon",
		"deaths_professor",
		"kills_zombie_knight",
		"deaths_professor_mage_guardian",
		"deaths_scared_skeleton",
		"kills_crypt_undead_christian",
		"deaths_diamond_guy",
		"dungeon_hub_giant_mushroom_no_pearls_no_return_best_time",
		"kills_crypt_undead_nicholas",
		"kills_crypt_undead_bernhard",
		"kills_crypt_undead_friedrich",
		"kills_crypt_undead_alexander",
		"kills_crypt_undead_marius",
		"kills_king_midas",
		"dungeon_hub_giant_mushroom_anything_with_return_best_time",
		"dungeon_hub_giant_mushroom_no_pearls_with_return_best_time",
		"dungeon_hub_crystal_core_anything_with_return_best_time",
		"dungeon_hub_crystal_core_no_pearls_with_return_best_time",
		"kills_deathmite",
		"deaths_suffocation",
		"deaths_young_dragon",
		"deaths_deep_sea_protector",
		"kills_tarantula_spider",
		"deaths_tarantula_spider",
		"most_winter_cannonballs_hit",
		"deaths_dungeon_respawning_skeleton",
		"deaths_professor_guardian_summon",
		"deaths_sniper_skeleton",
		"deaths_wither",
		"deaths_unstable_dragon",
		"deaths_generator_ghast",
		"dungeon_hub_giant_mushroom_no_abilities_no_return_best_time",
		"dungeon_hub_giant_mushroom_nothing_no_return_best_time",
		"deaths_pigman",
		"deaths_catfish",
		"deaths_guardian_defender",
		"dungeon_hub_crystal_core_nothing_no_return_best_time",
		"dungeon_hub_crystal_core_no_abilities_with_return_best_time",
		"dungeon_hub_crystal_core_nothing_with_return_best_time",
		"dungeon_hub_giant_mushroom_no_abilities_with_return_best_time",
		"dungeon_hub_giant_mushroom_nothing_with_return_best_time",
		"dungeon_hub_precursor_ruins_no_abilities_no_return_best_time",
		"dungeon_hub_precursor_ruins_nothing_no_return_best_time",
		"dungeon_hub_precursor_ruins_no_abilities_with_return_best_time",
		"dungeon_hub_precursor_ruins_nothing_with_return_best_time",
		"deaths_zombie_villager",
		"deaths_howling_spirit",
		"deaths_professor_archer_guardian",
		"deaths_sea_guardian",
		"deaths_respawning_skeleton",
		"deaths_jockey_shot_silverfish",
		"kills_headless_horseman",
		"deaths_king_midas",
		"kills_super_archer",
		"kills_crypt_witherskeleton",
		"kills_spirit_wolf",
		"kills_spirit_bull",
		"kills_spirit_rabbit",
		"kills_spirit_chicken",
		"kills_spirit_bat",
		"kills_spirit_sheep",
		"deaths_spirit_chicken",
		"kills_spirit_miniboss",
		"deaths_spirit_bat",
		"kills_super_tank_zombie",
		"kills_watcher_scarf",
		"deaths_spirit_wolf",
		"kills_thorn",
		"deaths_spirit_miniboss",
		"deaths_spirit_rabbit",
		"deaths_spirit_bull",
		"deaths_watcher_scarf",
		"deaths_watcher_guardian",
		"deaths_zombie_knight",
		"deaths_crypt_tank_zombie",
		"deaths_zombie_soldier",
		"deaths_super_archer",
		"deaths_watcher_bonzo",
		"deaths_lonely_spider",
		"deaths_crypt_undead",
		"mythos_burrows_dug_next",
		"mythos_burrows_dug_next_common",
		"mythos_burrows_dug_combat",
		"mythos_burrows_dug_combat_common",
		"mythos_kills",
		"kills_siamese_lynx",
		"kills_minos_hunter",
		"mythos_burrows_dug_treasure",
		"mythos_burrows_dug_treasure_common",
		"mythos_burrows_chains_complete",
		"mythos_burrows_chains_complete_common",
		"mythos_burrows_dug_next_uncommon",
		"mythos_burrows_dug_combat_uncommon",
		"kills_minotaur",
		"mythos_burrows_dug_treasure_uncommon",
		"mythos_burrows_chains_complete_uncommon",
		"mythos_burrows_dug_next_rare",
		"mythos_burrows_dug_combat_rare",
		"kills_gaia_construct",
		"deaths_gaia_construct",
		"mythos_burrows_dug_next_legendary",
		"mythos_burrows_dug_combat_legendary",
		"deaths_minos_champion",
		"deaths_minotaur",
		"mythos_burrows_dug_treasure_legendary",
		"mythos_burrows_chains_complete_legendary",
		"kills_minos_champion",
		"kills_minos_inquisitor",
		"mythos_burrows_dug_next_null",
		"mythos_burrows_dug_combat_null",
		"mythos_burrows_dug_treasure_rare",
		"mythos_burrows_chains_complete_rare",
		"mythos_burrows_dug_next_epic",
		"mythos_burrows_dug_treasure_epic",
		"mythos_burrows_dug_combat_epic",
		"mythos_burrows_chains_complete_epic",
		"mythos_burrows_dug_treasure_null",
		"mythos_burrows_chains_complete_null",
		"deaths_minos_inquisitor",
		"kills_nurse_shark",
		"kills_tiger_shark",
		"kills_great_white_shark",
		"kills_blue_shark",
		"deaths_great_white_shark",
		"kills_tentaclees",
		"deaths_livid_clone",
		"deaths_livid",
		"deaths_tentaclees",
		"deaths_bonzo_summon_undead",
		"deaths_professor_warrior_guardian",
		"auctions_bought_mythic",
		"kills_watcher_livid",
		"deaths_sadan_statue",
		"kills_zombie_commander",
		"kills_sadan_statue",
		"kills_sadan_giant",
		"kills_sadan_golem",
		"kills_skeletor_prime",
		"deaths_sadan_giant",
		"deaths_sadan_golem",
		"deaths_sadan",
		"kills_mimic",
		"deaths_siamese_lynx",
		"deaths_bonzo",
		"deaths_skeletor_prime",
		"kills_wraith",
		"kills_wither_gourd",
		"kills_scary_jerry",
		"kills_phantom_spirit",
		"kills_trick_or_treater",
		"kills_bat_spooky",
		"kills_batty_witch",
		"kills_witch_bat",
		"kills_scarecrow",
		"kills_nightmare",
		"kills_phantom_fisherman",
		"kills_werewolf",
		"kills_grim_reaper",
		"auctions_sold_mythic",
		"deaths_crypt_witherskeleton",
		"kills_zombie_lord",
		"deaths_wither_miner",
		"kills_wither_miner",
		"kills_wither_guard",
		"deaths_maxor",
		"deaths_wither_guard",
		"kills_skeleton_lord",
		"kills_watcher_giant_laser",
		"kills_watcher_giant_boulder",
		"kills_watcher_giant_diamond",
		"kills_necron_guard",
		"deaths_watcher_livid",
		"deaths_necron_guard",
		"kills_watcher_giant_bigfoot",
		"deaths_watcher_giant_bigfoot",
		"deaths_watcher_giant_boulder",
		"deaths_super_tank_zombie",
		"deaths_crushed",
		"deaths_armor_stand",
		"kills_mayor_jerry_green",
		"kills_mayor_jerry_blue",
		"kills_mayor_jerry_purple",
		"kills_mayor_jerry_golden",
		"kills_ice_walker",
		"kills_goblin",
		"deaths_caverns_ghost",
		"kills_goblin_knife_thrower",
		"kills_goblin_weakling_melee",
		"kills_goblin_weakling_bow",
		"kills_treasure_hoarder",
		"kills_goblin_creepertamer",
		"kills_goblin_battler",
		"kills_goblin_murderlover",
		"kills_caverns_ghost",
		"kills_goblin_creeper",
		"kills_goblin_golem",
		"kills_crystal_sentry",
		"kills_powder_ghast",
		"deaths_goblin_murderlover",
		"deaths_goblin_knife_thrower",
		"deaths_ice_walker",
		"deaths_crypt_undead_hypixel",
		"deaths_crypt_undead_flameboy101",
		"deaths_goblin_weakling_bow",
		"kills_arachne_brood",
		"kills_arachne_keeper",
		"deaths_arachne",
		"kills_arachne",
		"deaths_arachne_brood",
		"deaths_arachne_keeper",
		"deaths_cellar_spider",
		"kills_master_sniper_skeleton",
		"kills_master_crypt_tank_zombie",
		"kills_master_zombie_grunt",
		"kills_master_crypt_lurker",
		"kills_master_scared_skeleton",
		"kills_master_skeleton_soldier",
		"kills_master_skeleton_grunt",
		"kills_master_crypt_souleater",
		"kills_master_dungeon_respawning_skeleton",
		"kills_master_lost_adventurer",
		"kills_master_crypt_dreadlord",
		"kills_master_cellar_spider",
		"kills_master_watcher_summon_undead",
		"deaths_master_watcher_summon_undead",
		"kills_master_bonzo_summon_undead",
		"deaths_master_bonzo",
		"deaths_spirit_sheep",
		"kills_master_crypt_undead",
		"kills_master_diamond_guy",
		"kills_master_skeleton_master",
		"deaths_master_scarf_archer",
		"kills_master_scarf_mage",
		"deaths_master_scarf",
		"kills_dante_goon",
		"kills_dante_slime_goon",
		"kills_rat",
		"kills_mushroom_cow",
		"kills_trapper_pig",
		"kills_trapper_chicken",
		"kills_trapper_sheep",
		"kills_trapper_cow",
		"kills_trapper_rabbit",
		"deaths_master_lost_adventurer",
		"deaths_zombie_grunt",
		"deaths_grim_reaper",
		"kills_oasis_sheep",
		"deaths_master_skeleton_soldier",
		"deaths_master_scarf_warrior",
		"kills_master_parasite",
		"kills_master_scarf_priest",
		"deaths_master_crypt_lurker",
		"kills_master_crypt_undead_bernhard",
		"kills_master_scarf_warrior",
		"kills_master_scarf_archer",
		"deaths_master_scarf_mage",
		"kills_master_skeletor",
		"deaths_master_skeletor",
		"kills_master_zombie_knight",
		"kills_master_zombie_soldier",
		"kills_master_lonely_spider",
		"deaths_master_watcher_bonzo",
		"kills_master_professor_guardian_summon",
		"deaths_master_professor_mage_guardian",
		"kills_master_super_tank_zombie",
		"kills_master_spirit_bat",
		"deaths_master_zombie_soldier",
		"deaths_master_livid_clone",
		"deaths_master_sniper_skeleton",
		"kills_master_watcher_bonzo",
		"deaths_master_spirit_bat",
		"deaths_master_spirit_sheep",
		"deaths_master_spirit_rabbit",
		"deaths_master_professor_guardian_summon",
		"kills_master_crypt_witherskeleton",
		"deaths_master_watcher_scarf",
		"kills_master_crypt_undead_pieter",
		"kills_master_spirit_wolf",
		"kills_master_spirit_rabbit",
		"kills_master_spirit_sheep",
		"kills_master_spirit_bull",
		"deaths_master_shadow_assassin",
		"deaths_master_spirit_chicken",
		"kills_master_tentaclees",
		"deaths_master_tentaclees",
		"kills_master_super_archer",
		"kills_master_shadow_assassin",
		"deaths_master_crypt_witherskeleton",
		"deaths_master_crypt_dreadlord",
		"deaths_master_skeleton_master",
		"deaths_master_zombie_knight",
		"deaths_master_livid",
		"deaths_master_crypt_souleater",
		"pumpkin_launcher_count",
		"kills_shrine_charged_creeper",
		"kills_shrine_skeleton_horseman",
		"deaths_master_diamond_guy",
		"deaths_master_dungeon_respawning_skeleton",
		"deaths_master_skeleton_grunt",
		"kills_oasis_rabbit",
		"kills_master_spirit_chicken",
		"kills_master_spirit_miniboss",
		"deaths_master_spirit_miniboss",
		"kills_voidling_fanatic",
		"kills_voidling_extremist",
		"deaths_voidling_extremist",
		"deaths_voidling_fanatic",
		"kills_voidling_enderman",
		"kills_thyst",
		"kills_sludge",
		"kills_automaton",
		"kills_key_guardian",
		"deaths_automaton",
		"kills_team_treasurite_viper",
		"kills_team_treasurite_sebastian",
		"kills_yog",
		"kills_goblin_flamethrower",
		"kills_team_treasurite_wendy",
		"kills_belle",
		"kills_fire_bat",
		"kills_worm",
		"kills_team_treasurite_grunt",
		"deaths_yog",
		"kills_silvo",
		"deaths_kalhuiki_tribe_man",
		"deaths_kalhuiki_tribe_woman",
		"kills_butterfly",
		"kills_cavitak",
		"deaths_sludge",
		"kills_trapped_sludge",
		"kills_team_treasurite_corleone",
		"kills_smog",
		"kills_vittomite",
		"kills_kalhuiki_elder",
		"kills_kalhuiki_tribe_man",
		"kills_kalhuiki_youngling",
		"kills_scatha",
		"deaths_master_crypt_tank_zombie",
		"kills_lava_pigman",
		"kills_lava_blaze",
		"kills_kalhuiki_tribe_woman",
		"deaths_zombie_commander",
		"deaths_entity",
		"kills_flaming_worm",
		"deaths_team_treasurite_corleone",
		"deaths_master_spirit_wolf",
		"kills_master_crypt_undead_valentin",
		"kills_master_crypt_undead_nicholas",
		"kills_master_crypt_undead_christian",
		"kills_master_crypt_undead_friedrich",
		"deaths_master_sadan_giant",
		"deaths_master_sadan_statue",
		"kills_master_skeletor_prime",
		"deaths_voracious_spider",
		"deaths_voidling_enderman",
		"total_pet_exp_gained",
		"highest_damage",
		"sea_creature_kills",
		"deaths_master_professor",
		"kills_water_worm",
		"kills_poisoned_water_worm",
		"deaths_goldor",
		"deaths_storm",
		"deaths_necron",
		"kills_master_sadan_statue",
		"deaths_master_skeletor_prime",
		"deaths_master_sadan_golem",
		"kills_master_mimic",
		"deaths_watcher_giant_diamond",
		"kills_master_crypt_undead_alexander",
		"kills_master_skeleton_lord",
		"kills_master_zombie_commander",
		"kills_master_wither_guard",
		"deaths_master_maxor",
		"kills_master_crypt_undead_marius",
		"deaths_master_super_archer",
		"kills_master_sadan_golem",
		"deaths_master_zombie_commander",
		"kills_master_wither_miner",
		"deaths_master_goldor",
		"kills_dive_ghast",
		"kills_intro_blaze",
		"kills_barbarian",
		"kills_fire_mage",
		"kills_charging_mushroom_cow",
		"kills_vanquisher",
		"kills_baby_magma_slug",
		"deaths_barbarians_guard",
		"kills_flaming_spider",
		"kills_magma_cube_rider",
		"kills_magma_slug",
		"kills_moogma",
		"kills_lava_leech",
		"deaths_barbarian_duke_x",
		"kills_dojo_knockback_zombie",
		"deaths_charging_mushroom_cow",
		"deaths_smoldering_blaze",
		"kills_fire_eel",
		"items_fished_trophy_fish",
		"kills_pyroclastic_worm",
		"kills_lava_flame",
		"kills_pig_rider",
		"kills_wither_spectre",
		"deaths_mage_outlaw",
		"deaths_magma_boss",
		"kills_magma_glare",
		"kills_unstable_magma",
		"kills_mage_skull",
		"kills_mage_outlaw",
		"deaths_ashfang",
		"kills_magma_boss",
		"deaths_magma_glare",
		"deaths_hellwisp",
		"kills_hellwisp",
		"deaths_unstable_magma",
		"kills_bezal",
		"kills_mutated_blaze",
		"deaths_bladesoul",
		"kills_bladesoul",
		"kills_wither_defender_guard",
		"kills_duelist_rollim",
		"deaths_kuudra_knocker",
		"kills_smoldering_blaze",
		"deaths_flare",
		"kills_barbarians_guard",
		"deaths_pyroclastic_worm",
		"deaths_pig_rider",
		"deaths_thunder",
		"kills_thunder",
		"deaths_moogma",
		"kills_livid_clone",
		"kills_livid",
		"kills_kuudra",
		"deaths_goliath_barbarian",
		"kills_ashfang_red_blaze",
		"deaths_kuudra_landmine",
		"deaths_kuudra_follower",
		"kills_barbarian_duke_x",
		"kills_matcho",
		"deaths_ashfang_red_blaze",
		"deaths_ashfang_blue_blaze",
		"kills_ashfang",
		"deaths_dive_ghast",
		"kills_flare",
		"deaths_lord_jawbus",
		"kills_pack_magma_cube",
		"kills_kada_knight",
		"kills_lord_jawbus",
		"deaths_magma_slug",
		"deaths_lava_flame",
		"deaths_master_wither_miner",
		"deaths_baby_magma_slug",
		"deaths_fire_eel",
		"kills_cinder_bat",
		"deaths_old_blaze",
		"kills_sadan",
		"deaths_lava_leech",
		"kills_taurus",
		"kills_bonzo"
	);
	public static final Map<String, String> STATS_CASE_MAP = Maps.of(
		"mythos_burrows_dug_treasure_common",
		"mythos_burrows_dug_treasure_COMMON",
		"mythos_burrows_chains_complete_legendary",
		"mythos_burrows_chains_complete_LEGENDARY",
		"mythos_burrows_dug_treasure_rare",
		"mythos_burrows_dug_treasure_RARE",
		"mythos_burrows_chains_complete_epic",
		"mythos_burrows_chains_complete_EPIC",
		"mythos_burrows_dug_combat_uncommon",
		"mythos_burrows_dug_combat_UNCOMMON",
		"mythos_burrows_dug_combat_epic",
		"mythos_burrows_dug_combat_EPIC",
		"mythos_burrows_dug_combat_rare",
		"mythos_burrows_dug_combat_RARE",
		"mythos_burrows_dug_treasure_legendary",
		"mythos_burrows_dug_treasure_LEGENDARY",
		"mythos_burrows_chains_complete_common",
		"mythos_burrows_chains_complete_COMMON",
		"mythos_burrows_chains_complete_rare",
		"mythos_burrows_chains_complete_RARE",
		"mythos_burrows_dug_next_legendary",
		"mythos_burrows_dug_next_LEGENDARY",
		"mythos_burrows_dug_combat_legendary",
		"mythos_burrows_dug_combat_LEGENDARY",
		"mythos_burrows_dug_next_common",
		"mythos_burrows_dug_next_COMMON",
		"mythos_burrows_chains_complete_uncommon",
		"mythos_burrows_chains_complete_UNCOMMON",
		"mythos_burrows_dug_treasure_epic",
		"mythos_burrows_dug_treasure_EPIC",
		"mythos_burrows_dug_treasure_uncommon",
		"mythos_burrows_dug_treasure_UNCOMMON",
		"mythos_burrows_dug_next_uncommon",
		"mythos_burrows_dug_next_UNCOMMON",
		"mythos_burrows_dug_combat_common",
		"mythos_burrows_dug_combat_COMMON",
		"mythos_burrows_dug_next_rare",
		"mythos_burrows_dug_next_RARE",
		"mythos_burrows_dug_next_epic",
		"mythos_burrows_dug_next_EPIC"
	);
	public String invMissing = "";
	private JsonArray profilesArray;
	private int profileIndex;
	private JsonElement hypixelPlayerJson;
	private boolean valid = false;
	private String uuid;
	private String username;
	private String profileName;
	private String failCause = "Unknown fail cause";
	private final Map<Integer, Double> profileToNetworth = new HashMap<>();

	/* Constructors */
	// Empty player, always invalid
	public Player() {
		failCause = "No Args Constructor";
	}

	public Player(String username) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (response.isNotValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		leaderboardDatabase.insertIntoLeaderboard(this);
	}

	/**
	 * Meant to only be used for leaderboard command
	 */
	public Player(String username, Gamemode gamemode) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid, HYPIXEL_API_KEY, false);
			if (response.isNotValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		leaderboardDatabase.insertIntoLeaderboardSync(this, gamemode);
	}

	public Player(String username, String profileName) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = skyblockProfilesFromUuid(uuid);
			if (response.isNotValid()) {
				failCause = response.failCause();
				return;
			}

			this.profilesArray = response.response().getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : failCause;
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		leaderboardDatabase.insertIntoLeaderboard(this);
	}

	public Player(String uuid, String username, JsonElement profileArray) {
		this(uuid, username, profileArray, false);
	}

	public Player(String uuid, String username, JsonElement profileArray, boolean isCopy) {
		this.uuid = uuid;
		this.username = username;

		try {
			if (profileArray == null) {
				return;
			}

			this.profilesArray = profileArray.getAsJsonArray();
			if (getLatestProfile(profilesArray)) {
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (!isCopy) {
			leaderboardDatabase.insertIntoLeaderboard(this);
		}
	}

	public Player(String uuid, String username, String profileName, JsonElement profileArray) {
		this(uuid, username, profileName, profileArray, false);
	}

	public Player(String uuid, String username, String profileName, JsonElement profileArray, boolean isCopy) {
		this.uuid = uuid;
		this.username = username;

		try {
			if (profileArray == null) {
				return;
			}

			this.profilesArray = profileArray.getAsJsonArray();
			if (profileIdFromName(profileName, profilesArray)) {
				failCause = failCause.equals("Unknown fail cause") ? "Invalid profile name" : "";
				return;
			}
		} catch (Exception e) {
			failCause = e.getMessage();
			return;
		}

		this.valid = true;
		if (!isCopy) {
			leaderboardDatabase.insertIntoLeaderboard(this);
		}
	}

	public Player copy() {
		return new Player(getUuid(), getUsername(), getProfileName(), getProfileArray(), true);
	}

	/* Constructor helper methods */
	public boolean usernameToUuid(String username) {
		UsernameUuidStruct response = ApiHandler.usernameToUuid(username);
		if (response.isNotValid()) {
			failCause = response.failCause();
			return true;
		}

		this.username = response.username();
		this.uuid = response.uuid();
		return false;
	}

	public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
		try {
			for (int i = 0; i < profilesArray.size(); i++) {
				String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
				if (currentProfileName.equalsIgnoreCase(profileName)) {
					this.profileName = currentProfileName;
					this.profileIndex = i;
					return false;
				}
			}
		} catch (Exception ignored) {}
		return true;
	}

	public boolean getLatestProfile(JsonArray profilesArray) {
		try {
			Instant lastProfileSave = Instant.EPOCH;
			for (int i = 0; i < profilesArray.size(); i++) {
				Instant lastSaveLoop;
				try {
					lastSaveLoop =
						Instant.ofEpochMilli(higherDepth(profilesArray.get(i), "members." + this.uuid + ".last_save").getAsLong());
				} catch (Exception e) {
					continue;
				}

				if (lastSaveLoop.isAfter(lastProfileSave)) {
					this.profileIndex = i;
					lastProfileSave = lastSaveLoop;
					this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
				}
			}
			return false;
		} catch (Exception ignored) {}
		return true;
	}

	/* Getters */
	public JsonElement profileJson() {
		return higherDepth(profilesArray.get(profileIndex), "members." + this.uuid);
	}

	public String getUsername() {
		return username;
	}

	public String getUsernameFixed() {
		return fixUsername(username);
	}

	public String getProfileName() {
		return profileName;
	}

	public String getUuid() {
		return uuid;
	}

	public JsonElement getOuterProfileJson() {
		return profilesArray.get(profileIndex);
	}

	public JsonArray getProfileArray() {
		return profilesArray;
	}

	public boolean isValid() {
		return valid;
	}

	public String getFailCause() {
		return failCause;
	}

	public JsonElement getHypixelPlayerJson() {
		if (hypixelPlayerJson == null) {
			hypixelPlayerJson = playerFromUuid(uuid).response();
		}

		return hypixelPlayerJson;
	}

	/* Links */
	public String skyblockStatsLink() {
		return Utils.skyblockStatsLink(username, profileName);
	}

	public String getThumbnailUrl() {
		return "https://cravatar.eu/helmavatar/" + uuid + "/64.png";
	}

	/* Bank and purse */
	/**
	 * @return Bank balance or -1 if bank API disabled
	 */
	public double getBankBalance() {
		return higherDepth(getOuterProfileJson(), "banking.balance", -1.0);
	}

	public double getPurseCoins() {
		return higherDepth(profileJson(), "coin_purse", 0.0);
	}

	public JsonArray getBankHistory() {
		try {
			return higherDepth(getOuterProfileJson(), "banking.transactions").getAsJsonArray();
		} catch (Exception e) {
			return null;
		}
	}

	/* Skills */
	public int getTotalSkillsXp() {
		int totalSkillXp = 0;
		for (String skill : SKILL_NAMES) {
			SkillsStruct skillInfo = getSkill(skill);
			if (skillInfo != null) {
				totalSkillXp += skillInfo.totalExp();
			} else {
				return -1;
			}
		}
		return totalSkillXp;
	}

	public int getFarmingCapUpgrade() {
		return higherDepth(profileJson(), "jacob2.perks.farming_level_cap", 0);
	}

	public int getSkillMaxLevel(String skillName, WeightType weightType) {
		if (weightType == WeightType.LILY) {
			return 60;
		}

		int maxLevel = higherDepth(getLevelingJson(), "leveling_caps." + skillName, 0);

		if (skillName.equals("farming")) {
			maxLevel = weightType == WeightType.SENITHER ? 60 : maxLevel + getFarmingCapUpgrade();
		}

		return maxLevel;
	}

	public double getSkillXp(String skillName) {
		try {
			return skillName.equals("catacombs") ? getCatacombs().totalExp() : getSkill(skillName).totalExp();
		} catch (Exception e) {
			return -1;
		}
	}

	public SkillsStruct getSkill(String skillName) {
		return getSkill(skillName, WeightType.NONE);
	}

	public SkillsStruct getSkill(String skillName, WeightType weightType) {
		try {
			return skillInfoFromExp(
				higherDepth(profileJson(), "experience_skill_" + (skillName.equals("social") ? "social2" : skillName)).getAsLong(),
				skillName,
				weightType
			);
		} catch (Exception e) {
			return null;
		}
	}

	public double getSkillAverage() {
		return getSkillAverage("", 0);
	}

	public double getSkillAverage(String skillName, int overrideAmount) {
		double skillAverage = 0;
		for (String skill : SKILL_NAMES) {
			try {
				if (skill.equals(skillName)) {
					skillAverage += overrideAmount;
				} else {
					SkillsStruct skillsStruct = getSkill(skill);
					skillAverage += skillsStruct.getProgressLevel();
				}
			} catch (Exception e) {
				return -1;
			}
		}
		return skillAverage / SKILL_NAMES.size();
	}

	public SkillsStruct skillInfoFromExp(long skillExp, String skill) {
		return skillInfoFromExp(skillExp, skill, WeightType.NONE);
	}

	public SkillsStruct skillInfoFromExp(long skillExp, String skill, WeightType weightType) {
		return levelingInfoFromExp(skillExp, skill, getSkillMaxLevel(skill, weightType));
	}

	public SkillsStruct skillInfoFromLevel(int targetLevel, String skill) {
		return skillInfoFromLevel(targetLevel, skill, WeightType.NONE);
	}

	public SkillsStruct skillInfoFromLevel(int targetLevel, String skill, WeightType weightType) {
		JsonArray skillsTable =
			switch (skill) {
				case "catacombs", "social", "HOTM", "bestiary.ISLAND", "bestiary.MOB", "bestiary.BOSS" -> higherDepth(
					getLevelingJson(),
					skill
				)
					.getAsJsonArray();
				case "runecrafting" -> higherDepth(getLevelingJson(), "runecrafting_xp").getAsJsonArray();
				default -> higherDepth(getLevelingJson(), "leveling_xp").getAsJsonArray();
			};

		int maxLevel = getSkillMaxLevel(skill, weightType);

		long xpTotal = 0L;
		int level = 1;
		for (int i = 0; i < maxLevel; i++) {
			xpTotal += skillsTable.get(i).getAsLong();

			if (level >= targetLevel) {
				xpTotal -= skillsTable.get(i).getAsLong();
				break;
			} else {
				level = (i + 1);
			}
		}

		long xpForNext = 0;
		if (level < maxLevel) {
			xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());
		}

		return new SkillsStruct(skill, targetLevel, maxLevel, xpTotal, 0, xpForNext, 0);
	}

	public SkillsStruct getHOTM() {
		long xp = higherDepth(profileJson(), "mining_core.experience", -1L);
		return xp == -1 ? null : skillInfoFromExp(xp, "HOTM");
	}

	/* Slayer */
	public int getTotalSlayer() {
		return getTotalSlayer("", 0);
	}

	public int getTotalSlayer(String type, int overrideAmount) {
		return (
			(type.equals("sven") ? overrideAmount : getSlayer("sven")) +
			(type.equals("rev") ? overrideAmount : getSlayer("rev")) +
			(type.equals("tara") ? overrideAmount : getSlayer("tara")) +
			(type.equals("enderman") ? overrideAmount : getSlayer("enderman")) +
			(type.equals("blaze") ? overrideAmount : getSlayer("blaze"))
		);
	}

	public int getSlayerBossKills(String slayerName, int tier) {
		return higherDepth(profileJson(), "slayer_bosses." + slayerName + ".boss_kills_tier_" + tier, 0);
	}

	/**
	 * @param slayerName sven, rev, tara, enderman
	 */
	public int getSlayer(String slayerName) {
		JsonElement profileSlayer = higherDepth(profileJson(), "slayer_bosses");
		return switch (slayerName) {
			case "sven" -> higherDepth(profileSlayer, "wolf.xp", 0);
			case "rev" -> higherDepth(profileSlayer, "zombie.xp", 0);
			case "tara" -> higherDepth(profileSlayer, "spider.xp", 0);
			case "enderman" -> higherDepth(profileSlayer, "enderman.xp", 0);
			case "blaze" -> higherDepth(profileSlayer, "blaze.xp", 0);
			default -> 0;
		};
	}

	public int getSlayerLevel(String slayerName) {
		return getSlayerLevel(slayerName, getSlayer(slayerName));
	}

	public int getSlayerLevel(String slayerName, int xp) {
		JsonArray levelArray = higherDepth(
			getLevelingJson(),
			"slayer_xp." +
			switch (slayerName) {
				case "sven" -> "wolf";
				case "rev" -> "zombie";
				case "tara" -> "spider";
				default -> slayerName;
			}
		)
			.getAsJsonArray();
		int level = 0;
		for (int i = 0; i < levelArray.size(); i++) {
			if (xp >= levelArray.get(i).getAsInt()) {
				level = i + 1;
			} else {
				break;
			}
		}
		return level;
	}

	/* Dungeons */
	public String getSelectedDungeonClass() {
		try {
			return higherDepth(profileJson(), "dungeons.selected_dungeon_class").getAsString();
		} catch (Exception e) {
			return "none";
		}
	}

	public int getHighestPlayedDungeonFloor() {
		int master = higherDepth(profileJson(), "dungeons.dungeon_types.master_catacombs.highest_tier_completed", -1);
		if (master != -1) {
			return master + 7;
		}

		return higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.highest_tier_completed", -1);
	}

	public Set<String> getItemsPlayerHas(List<String> items, InvItem... extras) {
		Map<Integer, InvItem> invItemMap = getInventoryMap();
		if (invItemMap == null) {
			return null;
		}

		Collection<InvItem> itemsMap = new ArrayList<>(invItemMap.values());
		itemsMap.addAll(new ArrayList<>(getEnderChestMap().values()));
		itemsMap.addAll(new ArrayList<>(getStorageMap().values()));
		Collections.addAll(itemsMap, extras);
		Set<String> itemsPlayerHas = new HashSet<>();

		for (InvItem item : itemsMap) {
			if (item != null) {
				if (!item.getBackpackItems().isEmpty()) {
					for (InvItem backpackItem : item.getBackpackItems()) {
						if (backpackItem != null && items.contains(backpackItem.getId())) {
							itemsPlayerHas.add(backpackItem.getId());
						}
					}
				} else {
					if (items.contains(item.getId())) {
						itemsPlayerHas.add(item.getId());
					}
				}
			}
		}

		return itemsPlayerHas;
	}

	public int getDungeonSecrets() {
		return higherDepth(getHypixelPlayerJson(), "achievements.skyblock_treasure_hunter", 0);
	}

	public SkillsStruct getDungeonClass(String className) {
		return skillInfoFromExp(higherDepth(profileJson(), "dungeons.player_classes." + className + ".experience", 0L), "catacombs");
	}

	public SkillsStruct getCatacombs() {
		return skillInfoFromExp(higherDepth(profileJson(), "dungeons.dungeon_types.catacombs.experience", 0L), "catacombs");
	}

	/* InvItem maps */
	public Map<Integer, InvItem> getInventoryMap() {
		return getInventoryMap(false);
	}

	public Map<Integer, InvItem> getInventoryMap(boolean sort) {
		try {
			String contents = higherDepth(profileJson(), "inv_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			Map<Integer, InvItem> invMap = getGenericInventoryMap(parsedContents);
			if (sort) {
				Map<Integer, InvItem> sortedMap = new TreeMap<>();
				for (Map.Entry<Integer, InvItem> entry : invMap.entrySet()) {
					if (entry.getKey() >= 9 && entry.getKey() <= 17) {
						sortedMap.put(entry.getKey() + 18, entry.getValue());
					} else if (entry.getKey() >= 27) {
						sortedMap.put(entry.getKey() - 18, entry.getValue());
					} else {
						sortedMap.put(entry.getKey(), entry.getValue());
					}
				}
				return sortedMap;
			} else {
				return invMap;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getPersonalVaultMap() {
		try {
			String contents = higherDepth(profileJson(), "personal_vault_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getStorageMap() {
		try {
			JsonElement backpackContents = higherDepth(profileJson(), "backpack_contents");
			List<String> backpackCount = getJsonKeys(backpackContents);
			Map<Integer, InvItem> storageMap = new HashMap<>();
			int counter = 1;
			for (String bp : backpackCount) {
				Collection<InvItem> curBpMap = getGenericInventoryMap(
					NBTReader.readBase64(higherDepth(backpackContents, bp + ".data").getAsString())
				)
					.values();
				for (InvItem itemSlot : curBpMap) {
					storageMap.put(counter, itemSlot);
					counter++;
				}
			}

			return storageMap;
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getTalismanBagMap() {
		try {
			String contents = higherDepth(profileJson(), "talisman_bag.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getEquipmentMap() {
		try {
			String contents = higherDepth(profileJson(), "equippment_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getArmorMap() {
		try {
			String contents = higherDepth(profileJson(), "inv_armor.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			Map<Integer, InvItem> oldMap = getGenericInventoryMap(parsedContents);
			Map<Integer, InvItem> fixedMap = new HashMap<>();
			fixedMap.put(0, oldMap.getOrDefault(3, null));
			fixedMap.put(1, oldMap.getOrDefault(2, null));
			fixedMap.put(2, oldMap.getOrDefault(1, null));
			fixedMap.put(3, oldMap.getOrDefault(0, null));
			return fixedMap;
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, InvItem> getWardrobeMap() {
		try {
			String contents = higherDepth(profileJson(), "wardrobe_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public List<InvItem> getPetsMap() {
		JsonArray petsArr;
		try {
			petsArr = getPets();
		} catch (Exception e) {
			return new ArrayList<>();
		}

		List<InvItem> petsNameFormatted = new ArrayList<>();

		for (JsonElement pet : petsArr) {
			try {
				InvItem invItemStruct = new InvItem();
				invItemStruct.setName(
					"[Lvl " +
					petLevelFromXp(
						higherDepth(pet, "exp", 0L),
						higherDepth(pet, "tier").getAsString().toLowerCase(),
						higherDepth(pet, "type").getAsString()
					) +
					"] " +
					capitalizeString(higherDepth(pet, "type").getAsString().toUpperCase().replace("_", " "))
				);
				invItemStruct.setId("PET");
				if (higherDepth(pet, "skin", null) != null) {
					invItemStruct.addExtraValue("PET_SKIN_" + higherDepth(pet, "skin").getAsString());
				}
				invItemStruct.setRarity(higherDepth(pet, "tier").getAsString());
				if (higherDepth(pet, "heldItem", null) != null) {
					invItemStruct.addExtraValue(higherDepth(pet, "heldItem").getAsString());
				}
				petsNameFormatted.add(invItemStruct);
			} catch (Exception ignored) {}
		}

		return petsNameFormatted;
	}

	public Map<Integer, InvItem> getEnderChestMap() {
		try {
			String contents = higherDepth(profileJson(), "ender_chest_contents.data").getAsString();
			NBTCompound parsedContents = NBTReader.readBase64(contents);
			return getGenericInventoryMap(parsedContents);
		} catch (Exception ignored) {}
		return null;
	}

	public Map<String, Integer> getPlayerSacks() {
		try {
			JsonObject sacksJson = higherDepth(profileJson(), "sacks_counts").getAsJsonObject();
			Map<String, Integer> sacksMap = new HashMap<>();
			for (Map.Entry<String, JsonElement> sacksEntry : sacksJson.entrySet()) {
				sacksMap.put(sacksEntry.getKey(), sacksEntry.getValue().getAsInt());
			}

			return sacksMap;
		} catch (Exception e) {
			return null;
		}
	}

	/* Emoji viewer arrays & other inventory */
	public List<String[]> getTalismanBag() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "talisman_bag.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");

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
					enderChestPages.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
					outputStringPart1 = new StringBuilder();
					outputStringPart2 = new StringBuilder();
					page += 45;
				}
			}
			return enderChestPages;
		} catch (Exception ignored) {}
		return null;
	}

	public List<String[]> getEnderChest() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "ender_chest_contents.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");

			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
				if (displayName != null) {
					String id = displayName.getString("id", "empty").toLowerCase();
					if (id.equals("pet")) {
						JsonElement petInfo = JsonParser.parseString(
							invFrames.getCompound(i).getCompound("tag.ExtraAttributes").getString("petInfo", "{}")
						);
						String newId =
							higherDepth(petInfo, "type", "") + RARITY_TO_NUMBER_MAP.getOrDefault(higherDepth(petInfo, "tier", ""), "");
						if (!newId.isEmpty()) {
							id = newId.toLowerCase();
						}
					} else if (id.equals("enchanted_book")) {
						NBTCompound enchantedBooks = invFrames.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
						if (enchantedBooks.size() == 1) {
							Map.Entry<String, Object> enchant = enchantedBooks.entrySet().stream().findFirst().get();
							id = enchant.getKey() + ";" + enchant.getValue();
						}
					}
					invFramesMap.put(i + 1, id);
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
					enderChestPages.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
					outputStringPart1 = new StringBuilder();
					outputStringPart2 = new StringBuilder();
					page += 45;
				}
			}
			return enderChestPages;
		} catch (Exception ignored) {}
		return null;
	}

	public List<String[]> getStorage() {
		try {
			List<String[]> out = new ArrayList<>();
			for (JsonElement page : higherDepth(profileJson(), "backpack_contents")
				.getAsJsonObject()
				.entrySet()
				.stream()
				.sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())))
				.map(Map.Entry::getValue)
				.toList()) {
				NBTCompound decodedInventoryContents = NBTReader.readBase64(higherDepth(page, "data").getAsString());

				NBTList invFrames = decodedInventoryContents.getList("i");
				Map<Integer, String> invFramesMap = new TreeMap<>();
				for (int i = 0; i < invFrames.size(); i++) {
					NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
					if (displayName != null) {
						String id = displayName.getString("id", "empty").toLowerCase();
						if (id.equals("pet")) {
							JsonElement petInfo = JsonParser.parseString(
								invFrames.getCompound(i).getCompound("tag.ExtraAttributes").getString("petInfo", "{}")
							);
							String newId =
								higherDepth(petInfo, "type", "") + RARITY_TO_NUMBER_MAP.getOrDefault(higherDepth(petInfo, "tier", ""), "");
							if (!newId.isEmpty()) {
								id = newId.toLowerCase();
							}
						} else if (id.equals("enchanted_book")) {
							NBTCompound enchantedBooks = invFrames.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
							if (enchantedBooks.size() == 1) {
								Map.Entry<String, Object> enchant = enchantedBooks.entrySet().stream().findFirst().get();
								id = enchant.getKey() + ";" + enchant.getValue();
							}
						}
						invFramesMap.put(i + 1, id);
					} else {
						invFramesMap.put(i + 1, "empty");
					}
				}
				if (invFrames.size() < 27) {
					int curSize = invFrames.size();
					for (int i = 0; i < 27 - curSize; i++) {
						invFramesMap.put(i + 1 + curSize, "blank");
					}
				}

				StringBuilder outputStringPart1 = new StringBuilder();
				StringBuilder outputStringPart2 = new StringBuilder();
				StringBuilder curNine = new StringBuilder();
				for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
					if (i.getKey() <= 18) {
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
				}

				out.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
			}
			return out;
		} catch (Exception ignored) {}
		return null;
	}

	public String[] getInventory() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "inv_contents.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");
			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
				if (displayName != null) {
					String id = displayName.getString("id", "empty").toLowerCase();
					if (id.equals("pet")) {
						JsonElement petInfo = JsonParser.parseString(
							invFrames.getCompound(i).getCompound("tag.ExtraAttributes").getString("petInfo", "{}")
						);
						String newId =
							higherDepth(petInfo, "type", "") + RARITY_TO_NUMBER_MAP.getOrDefault(higherDepth(petInfo, "tier", ""), "");
						if (!newId.isEmpty()) {
							id = newId.toLowerCase();
						}
					} else if (id.equals("enchanted_book")) {
						NBTCompound enchantedBooks = invFrames.getCompound(i).getCompound("tag.ExtraAttributes.enchantments");
						if (enchantedBooks.size() == 1) {
							Map.Entry<String, Object> enchant = enchantedBooks.entrySet().stream().findFirst().get();
							id = enchant.getKey() + ";" + enchant.getValue();
						}
					}
					invFramesMap.put(i + 1, id);
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
			return new String[] { outputStringPart2.toString(), outputStringPart1.toString() };
		} catch (Exception ignored) {}
		return null;
	}

	public Map<Integer, ArmorStruct> getWardrobeList() {
		try {
			String encodedWardrobeContents = higherDepth(profileJson(), "wardrobe_contents.data").getAsString();
			int equippedSlot = higherDepth(profileJson(), "wardrobe_equipped_slot").getAsInt();
			NBTCompound decodedWardrobeContents = NBTReader.readBase64(encodedWardrobeContents);

			NBTList wardrobeFrames = decodedWardrobeContents.getList("i");
			Map<Integer, String> wardrobeFramesMap = new HashMap<>();
			for (int i = 0; i < wardrobeFrames.size(); i++) {
				NBTCompound displayName = wardrobeFrames.getCompound(i).getCompound("tag.display");
				if (displayName != null) {
					wardrobeFramesMap.put(i, parseMcCodes(displayName.getString("Name", "Empty")));
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
				armorStructMap.replace((equippedSlot - 1), getArmor().makeBold());
			}

			return armorStructMap;
		} catch (Exception e) {
			return null;
		}
	}

	public List<String[]> getWardrobe() {
		try {
			int equippedWardrobeSlot = higherDepth(profileJson(), "wardrobe_equipped_slot").getAsInt();
			Map<Integer, InvItem> equippedArmor = equippedWardrobeSlot != -1 ? getArmorMap() : null;

			String encodedInventoryContents = higherDepth(profileJson(), "wardrobe_contents.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList invFrames = decodedInventoryContents.getList("i");
			Map<Integer, String> invFramesMap = new TreeMap<>();
			for (int i = 0; i < invFrames.size(); i++) {
				NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");

				if (displayName != null) {
					invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
				} else if (
					(equippedArmor != null) &&
					(equippedWardrobeSlot <= 9) &&
					((((i + 1) - equippedWardrobeSlot) % 9) == 0) &&
					((i + 1) <= 36) &&
					(equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9))) != null
				) {
					invFramesMap.put(i + 1, equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9)).getId().toLowerCase());
				} else if (
					(equippedArmor != null) &&
					(equippedWardrobeSlot > 9) &&
					((((i + 1) - equippedWardrobeSlot) % 9) == 0) &&
					((i + 1) > 36) &&
					(equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) - 3)) != null
				) {
					invFramesMap.put(i + 1, equippedArmor.get((((i + 1) - equippedWardrobeSlot) / 9) - 3).getId().toLowerCase());
				} else {
					invFramesMap.put(i + 1, "empty");
				}
			}

			if (invFramesMap.size() % 36 != 0) {
				int toAdd = 36 - (invFramesMap.size() % 36);
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
				if ((i.getKey() - page) <= 18) {
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

				if (i.getKey() != 0 && i.getKey() % 36 == 0) {
					enderChestPages.add(new String[] { outputStringPart1.toString(), outputStringPart2.toString() });
					outputStringPart1 = new StringBuilder();
					outputStringPart2 = new StringBuilder();
					page += 36;
				}
			}
			return enderChestPages;
		} catch (Exception ignored) {}
		return null;
	}

	public ArmorStruct getArmor() {
		try {
			String encodedInventoryContents = higherDepth(profileJson(), "inv_armor.data").getAsString();
			NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

			NBTList talismanFrames = decodedInventoryContents.getList("i");

			Map<Integer, String> armorFramesMap = new HashMap<>();
			for (int i = 0; i < talismanFrames.size(); i++) {
				NBTCompound displayName = talismanFrames.getCompound(i).getCompound("tag.display");
				if (displayName != null) {
					armorFramesMap.put(i, parseMcCodes(displayName.getString("Name", "Empty")));
				} else {
					armorFramesMap.put(i, "Empty");
				}
			}
			return new ArmorStruct(armorFramesMap.get(3), armorFramesMap.get(2), armorFramesMap.get(1), armorFramesMap.get(0));
		} catch (Exception e) {
			return null;
		}
	}

	public JsonArray getPets() {
		return higherDepth(profileJson(), "pets").getAsJsonArray();
	}

	/* Miscellaneous */
	public String[] getAllProfileNames(Gamemode gamemode) {
		List<String> profileNameList = new ArrayList<>();
		if (this.profilesArray == null) {
			this.profilesArray = skyblockProfilesFromUuid(uuid).response().getAsJsonArray();
		}

		for (JsonElement profile : profilesArray) {
			try {
				if (gamemode.isGamemode(higherDepth(profile, "game_mode", "regular"))) {
					profileNameList.add(higherDepth(profile, "cute_name").getAsString().toLowerCase());
				}
			} catch (Exception ignored) {}
		}

		return profileNameList.toArray(new String[0]);
	}

	public int getFairySouls() {
		return higherDepth(profileJson(), "fairy_souls_collected", 0);
	}

	public String itemToEmoji(String itemName) {
		itemName = itemName.toUpperCase();

		try {
			return getEmojiMap().get(itemName).getAsString();
		} catch (Exception ignored) {}

		if (!invMissing.contains(itemName)) {
			invMissing += "\n• " + itemName;
		}
		return "❓";
	}

	public double getLilyWeight() {
		return new LilyWeight(this, true).getTotalWeight().getRaw();
	}

	public double getWeight() {
		return new SenitherWeight(this, true).getTotalWeight().getRaw();
	}

	public double getWeight(String... weightTypes) {
		SenitherWeight weight = new SenitherWeight(this);
		for (String weightType : weightTypes) {
			if (SLAYER_NAMES.contains(weightType)) {
				weight.getSlayerWeight().getSlayerWeight(weightType);
			} else if (SKILL_NAMES.contains(weightType)) {
				weight.getSkillsWeight().getSkillsWeight(weightType);
			} else if (DUNGEON_CLASS_NAMES.contains(weightType)) {
				weight.getDungeonsWeight().getClassWeight(weightType);
			} else if (weightType.equals("catacombs")) {
				weight.getDungeonsWeight().getDungeonWeight();
			} else {
				throw new IllegalArgumentException("Invalid weight type: " + weightType);
			}
		}

		return weight.getTotalWeight().getRaw();
	}

	public EmbedBuilder defaultPlayerEmbed() {
		return defaultPlayerEmbed("");
	}

	public EmbedBuilder defaultPlayerEmbed(String extra) {
		return defaultEmbed(fixUsername(getUsername()) + getSymbol(" ") + extra, skyblockStatsLink()).setThumbnail(getThumbnailUrl());
	}

	public CustomPaginator.Builder defaultPlayerPaginator(User... users) {
		return defaultPlayerPaginator(PaginatorExtras.PaginatorType.DEFAULT, users);
	}

	public CustomPaginator.Builder defaultPlayerPaginator(PaginatorExtras.PaginatorType type, User... users) {
		return defaultPaginator(users)
			.setColumns(1)
			.setItemsPerPage(1)
			.setPaginatorExtras(
				new PaginatorExtras(type)
					.setEveryPageTitle(fixUsername(getUsername()) + getSymbol(" "))
					.setEveryPageThumbnail(getThumbnailUrl())
					.setEveryPageTitleUrl(skyblockStatsLink())
			);
	}

	public EmbedBuilder getFailEmbed() {
		return invalidEmbed(failCause);
	}

	public int getNumberMinionSlots() {
		try {
			List<String> profileMembers = getJsonKeys(higherDepth(getOuterProfileJson(), "members"));
			Set<String> uniqueCraftedMinions = new HashSet<>();

			for (String member : profileMembers) {
				try {
					JsonArray craftedMinions = higherDepth(getOuterProfileJson(), "members." + member + ".crafted_generators")
						.getAsJsonArray();
					for (JsonElement minion : craftedMinions) {
						uniqueCraftedMinions.add(minion.getAsString());
					}
				} catch (Exception ignored) {}
			}

			int prevMax = 0;
			for (int i = 0; i < CRAFTED_MINIONS_TO_SLOTS.size(); i++) {
				if (uniqueCraftedMinions.size() >= CRAFTED_MINIONS_TO_SLOTS.get(i)) {
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

	public int getPetScore() {
		JsonArray playerPets = getPets();
		Map<String, Integer> petsMap = new HashMap<>();
		for (JsonElement pet : playerPets) {
			String petName = higherDepth(pet, "type").getAsString();
			int rarity =
				switch (higherDepth(pet, "tier").getAsString().toLowerCase()) {
					case "common" -> 1;
					case "uncommon" -> 2;
					case "rare" -> 3;
					case "epic" -> 4;
					case "legendary" -> 5;
					default -> 0;
				};
			if (petsMap.containsKey(petName)) {
				if (petsMap.get(petName) < rarity) {
					petsMap.replace(petName, rarity);
				}
			} else {
				petsMap.put(petName, rarity);
			}
		}

		int petScore = 0;
		for (int i : petsMap.values()) {
			petScore += i;
		}

		return petScore;
	}

	public String getSymbol(String... prefix) {
		return getGamemode().getSymbol(prefix);
	}

	public boolean isGamemode(Gamemode gamemode) {
		return gamemode.isGamemode(getGamemode());
	}

	public Gamemode getGamemode() {
		return Gamemode.of(higherDepth(getOuterProfileJson(), "game_mode", "regular"));
	}

	public double getHighestAmount(String type) {
		return getHighestAmount(type, Gamemode.ALL);
	}

	public double getHighestAmount(String type, Gamemode gamemode) {
		double highestAmount = -1.0;
		int beforeProfileIndex = this.profileIndex;
		this.profileIndex = 0;
		for (JsonElement ignored : profilesArray) {
			if (isGamemode(gamemode)) {
				switch (type) {
					case "slayer":
						highestAmount = Math.max(highestAmount, getTotalSlayer());
					case "skills":
						highestAmount = Math.max(highestAmount, getSkillAverage());
						break;
					case "catacombs":
						highestAmount = Math.max(highestAmount, getCatacombs().getProgressLevel());
						break;
					case "catacombs_xp":
						highestAmount = Math.max(highestAmount, getCatacombs().totalExp());
						break;
					case "healer", "mage", "berserk", "archer", "tank":
						highestAmount = Math.max(highestAmount, getDungeonClass(type).getProgressLevel());
						break;
					case "weight":
						highestAmount = Math.max(highestAmount, getWeight());
						break;
					case "sven":
					case "rev":
					case "tara":
					case "enderman":
					case "blaze":
						highestAmount = Math.max(highestAmount, getSlayer(type));
						break;
					case "alchemy":
					case "combat":
					case "fishing":
					case "farming":
					case "foraging":
					case "carpentry":
					case "mining":
					case "taming":
					case "enchanting":
					case "social":
						highestAmount = Math.max(highestAmount, getSkill(type) != null ? getSkill(type).getProgressLevel() : -1);
						break;
					case "alchemy_xp":
					case "combat_xp":
					case "fishing_xp":
					case "farming_xp":
					case "foraging_xp":
					case "carpentry_xp":
					case "mining_xp":
					case "taming_xp":
					case "enchanting_xp":
					case "social_xp":
						highestAmount =
							Math.max(
								highestAmount,
								getSkill(type.split("_xp")[0]) != null ? getSkill(type.split("_xp")[0]).totalExp() : -1
							);
						break;
					case "bank":
						highestAmount = Math.max(highestAmount, getBankBalance());
						break;
					case "purse":
						highestAmount = Math.max(highestAmount, getPurseCoins());
						break;
					case "coins":
						highestAmount = Math.max(highestAmount, Math.max(0, getBankBalance()) + getPurseCoins());
						break;
					case "pet_score":
						highestAmount = Math.max(highestAmount, getPetScore());
						break;
					case "networth":
						highestAmount = Math.max(highestAmount, getNetworth());
						break;
					case "fairy_souls":
						highestAmount = Math.max(highestAmount, getFairySouls());
						break;
					case "slot_collector":
						highestAmount = Math.max(highestAmount, getNumberMinionSlots());
						break;
					case "dungeon_secrets":
						highestAmount = Math.max(highestAmount, getDungeonSecrets());
						break;
					case "accessory_count":
						highestAmount = Math.max(highestAmount, getAccessoryCount());
						break;
					case "total_slayer":
						highestAmount = Math.max(highestAmount, getTotalSlayer());
						break;
					case "slayer_nine":
						highestAmount = Math.max(highestAmount, getNumLvlNineSlayers());
						break;
					case "maxed_collections":
						highestAmount = Math.max(highestAmount, getNumMaxedCollections());
						break;
					case "mage_rep":
						highestAmount = Math.max(highestAmount, getMageRep());
						break;
					case "barbarian_rep":
						highestAmount = Math.max(highestAmount, getBarbarianRep());
						break;
					case "ironman":
					case "stranded":
						highestAmount = Math.max(highestAmount, isGamemode(Gamemode.of(type)) ? 1 : -1);
						break;
					case "lily_weight":
						highestAmount = Math.max(highestAmount, getLilyWeight());
						break;
					default:
						if (COLLECTION_NAME_TO_ID.containsKey(type)) {
							highestAmount = Math.max(highestAmount, getCollection(COLLECTION_NAME_TO_ID.get(type)));
							break;
						} else if (STATS_LIST.contains(type)) {
							highestAmount = Math.max(highestAmount, getStat(type));
							break;
						} else {
							this.profileIndex = beforeProfileIndex;
							return -1;
						}
				}
			}

			this.profileIndex++;
		}

		this.profileIndex = beforeProfileIndex;
		return highestAmount;
	}

	public int getNumLvlNineSlayers() {
		int lvlNineSlayers = getSlayer("sven") >= 1000000 ? 1 : 0;
		lvlNineSlayers = getSlayer("rev") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		lvlNineSlayers = getSlayer("tara") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		lvlNineSlayers = getSlayer("enderman") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		lvlNineSlayers = getSlayer("blaze") >= 1000000 ? lvlNineSlayers + 1 : lvlNineSlayers;
		return lvlNineSlayers;
	}

	public long getCollection(String id) {
		return higherDepth(profileJson(), "collection." + id, -1);
	}

	public long getCombinedCollection(String id) {
		long amount = 0;
		for (Map.Entry<String, JsonElement> member : higherDepth(getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			amount += higherDepth(member.getValue(), "collection." + id, 0L);
		}
		return amount;
	}

	public double getStat(String stat) {
		return higherDepth(profileJson(), "stats." + STATS_CASE_MAP.getOrDefault(stat, stat), -1.0);
	}

	public int getNumMaxedCollections() {
		Map<String, Long> collections = new HashMap<>();
		for (Map.Entry<String, JsonElement> member : higherDepth(getOuterProfileJson(), "members").getAsJsonObject().entrySet()) {
			try {
				for (Map.Entry<String, JsonElement> collection : higherDepth(member.getValue(), "collection")
					.getAsJsonObject()
					.entrySet()) {
					collections.compute(collection.getKey(), (k, v) -> (v == null ? 0 : v) + collection.getValue().getAsLong());
				}
			} catch (Exception ignored) {}
		}
		int numMaxedColl = 0;
		for (Map.Entry<String, Long> collection : collections.entrySet()) {
			long maxAmount = higherDepth(getCollectionsJson(), collection.getKey() + ".tiers.[-1]", -1L);
			if (maxAmount != -1 && collection.getValue() >= maxAmount) {
				numMaxedColl++;
			}
		}

		return numMaxedColl;
	}

	public int getAccessoryCount() {
		return getItemsPlayerHas(new ArrayList<>(ALL_TALISMANS), getTalismanBagMap().values().toArray(new InvItem[0])).size();
	}

	public double getNetworth() {
		if (profileToNetworth.containsKey(profileIndex)) {
			return profileToNetworth.get(profileIndex);
		}

		double networth = NetworthExecute.getTotalNetworth(this);
		profileToNetworth.put(profileIndex, networth);
		return networth;
	}

	public int getMageRep() {
		return higherDepth(profileJson(), "nether_island_player_data.mages_reputation", 0);
	}

	public int getBarbarianRep() {
		return higherDepth(profileJson(), "nether_island_player_data.barbarians_reputation", 0);
	}

	@Override
	public String toString() {
		return (
			"Player{" +
			"validPlayer=" +
			valid +
			", playerUuid='" +
			uuid +
			'\'' +
			", playerUsername='" +
			username +
			'\'' +
			", profileName='" +
			profileName +
			'\'' +
			'}'
		);
	}

	public double getBestiaryLevel() {
		int total = 0;
		for (Map.Entry<String, List<String>> location : locations.entrySet()) {
			for (String mob : location.getValue()) {
				int kills = higherDepth(profileJson(), "bestiary.kills_" + mob, 0);
				String type = "MOB";
				if (location.getKey().equals("Private Island")) {
					type = "ISLAND";
				} else if (bosses.contains(mob)) {
					type = "BOSS";
				}
				total +=
					levelingInfoFromExp(kills, "bestiary." + type, higherDepth(getLevelingJson(), "bestiary.caps." + type).getAsInt())
						.currentLevel();
			}
		}
		return total / 10.0;
	}

	public enum WeightType {
		NONE,
		SENITHER,
		LILY;

		public static WeightType of(String name) {
			return valueOf(name.toUpperCase());
		}
	}

	public enum Gamemode {
		ALL,
		REGULAR,
		STRANDED,
		IRONMAN,
		IRONMAN_STRANDED;

		public static Gamemode of(String gamemode) {
			return valueOf(
				switch (gamemode = gamemode.toUpperCase()) {
					case "ISLAND" -> "STRANDED";
					case "BINGO" -> "REGULAR";
					case "" -> "ALL";
					default -> gamemode;
				}
			);
		}

		public String toCacheType() {
			return (
				switch (this) {
					case IRONMAN, STRANDED -> name().toLowerCase();
					default -> "all";
				} +
				"_lb"
			);
		}

		public boolean isGamemode(Object gamemode) {
			if (gamemode instanceof Gamemode mode) {
				return (this == IRONMAN_STRANDED) ? ((gamemode == IRONMAN) || (gamemode == STRANDED)) : ((this == ALL) || (this == mode));
			}

			return isGamemode(of((String) gamemode));
		}

		public String getSymbol(String... prefix) {
			return (
				(prefix.length >= 1 ? prefix[0] : "") +
				switch (this) {
					case IRONMAN -> "\u267B️";
					case STRANDED -> "\uD83C\uDFDD";
					default -> "";
				}
			).stripTrailing();
		}
	}

	public boolean isInventoryApiEnabled() {
		return higherDepth(profileJson(), "inv_contents.data", null) != null;
	}

	public boolean isBankApiEnabled() {
		return getBankBalance() != -1;
	}

	public boolean isCollectionsApiEnabled() {
		try {
			return higherDepth(profileJson(), "collection").getAsJsonObject() != null;
		} catch (Exception ignored) {}
		return false;
	}

	public boolean isVaultApiEnabled() {
		return getPersonalVaultMap() != null;
	}

	public boolean isSkillsApiEnabled() {
		return getSkill("combat") != null;
	}
}
