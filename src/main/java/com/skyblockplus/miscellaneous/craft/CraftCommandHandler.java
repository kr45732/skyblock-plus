/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
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

package com.skyblockplus.miscellaneous.craft;

import static com.skyblockplus.utils.ApiHandler.getQueryApiUrl;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.HttpUtils.getJson;
import static com.skyblockplus.utils.utils.HypixelUtils.isCrimsonArmor;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.utils.StringUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.apache.groovy.util.Maps;
import org.apache.http.client.utils.URIBuilder;

public class CraftCommandHandler {

	public static final List<String> ignoredCategories = List.of(
		"TRAVEL_SCROLL",
		"SHEARS",
		"NONE",
		"DUNGEON_PASS",
		"ARROW_POISON",
		"PORTAL",
		"PET_ITEM",
		"REFORGE_STONE",
		"COSMETIC",
		"BAIT",
		"ARROW"
	);
	private static final List<String> woodSingularityItems = List.of(
		"WOOD_SWORD",
		"ASPECT_OF_THE_JERRY",
		"SCORPION_FOIL",
		"TACTICIAN_SWORD",
		"SWORD_OF_REVELATIONS",
		"SWORD_OF_BAD_HEALTH",
		"GREAT_SPOOK_SWORD"
	);
	private static final List<String> weaponCategories = List.of("SWORD", "LONGSWORD", "BOW", "FISHING_ROD", "FISHING_WEAPON");
	private static final List<String> armorCategories = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
	private static final List<String> dyeMaterials = List.of("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS");
	private static final List<String> necronBladeScrollItems = List.of("NECRON_BLADE", "HYPERION", "VALKYRIE", "ASTRAEA", "SCYLLA");
	private static final List<String> accessoryEnrichments = List.of(
		"TALISMAN_ENRICHMENT_ATTACK_SPEED",
		"TALISMAN_ENRICHMENT_CRITICAL_CHANCE",
		"TALISMAN_ENRICHMENT_CRITICAL_DAMAGE",
		"TALISMAN_ENRICHMENT_DEFENSE",
		"TALISMAN_ENRICHMENT_FEROCITY",
		"TALISMAN_ENRICHMENT_HEALTH",
		"TALISMAN_ENRICHMENT_INTELLIGENCE",
		"TALISMAN_ENRICHMENT_MAGIC_FIND",
		"TALISMAN_ENRICHMENT_SEA_CREATURE_CHANCE",
		"TALISMAN_ENRICHMENT_STRENGTH",
		"TALISMAN_ENRICHMENT_WALK_SPEED"
	);
	private static final List<String> dyes = List.of(
		"DYE_AQUAMARINE",
		"DYE_BINGO_BLUE",
		"DYE_BONE",
		"DYE_BRICK_RED",
		"DYE_BYZANTIUM",
		"DYE_CARMINE",
		"DYE_CELESTE",
		"DYE_DARK_PURPLE",
		"DYE_EMERALD",
		"DYE_FLAME",
		"DYE_HOLLY",
		"DYE_MANGO",
		"DYE_NADESHIKO",
		"DYE_NECRON",
		"DYE_NYANZA",
		"DYE_PURE_BLACK",
		"DYE_PURE_WHITE",
		"DYE_WILD_STRAWBERRY"
	);
	private static final List<String> drillUpgradeModules = List.of(
		"GOBLIN_OMELETTE",
		"GOBLIN_OMELETTE_BLUE_CHEESE",
		"GOBLIN_OMELETTE_PESTO",
		"GOBLIN_OMELETTE_SPICY",
		"GOBLIN_OMELETTE_SUNNY_SIDE"
	);
	private static final List<String> drillFuelTanks = List.of(
		"MITHRIL_FUEL_TANK",
		"TITANIUM_FUEL_TANK",
		"GEMSTONE_FUEL_TANK",
		"PERFECTLY_CUT_FUEL_TANK"
	);
	private static final List<String> drillEngines = List.of(
		"MITHRIL_DRILL_ENGINE",
		"TITANIUM_DRILL_ENGINE",
		"RUBY_POLISHED_DRILL_ENGINE",
		"SAPPHIRE_POLISHED_DRILL_ENGINE",
		"AMBER_POLISHED_DRILL_ENGINE"
	);
	private static final List<String> powerScrolls = List.of(
		"AMBER_POWER_SCROLL",
		"AMETHYST_POWER_SCROLL",
		"JASPER_POWER_SCROLL",
		"RUBY_POWER_SCROLL",
		"SAPPHIRE_POWER_SCROLL",
		"OPAL_POWER_SCROLL"
	);
	private static final List<String> gemstoneTiers = List.of("ROUGH", "FLAWED", "FINE", "FLAWLESS", "PERFECT");
	private static final Map<String, List<String>> slotTypeToGemstones = Maps.of(
		"AMBER",
		List.of("AMBER"),
		"TOPAZ",
		List.of("TOPAZ"),
		"SAPPHIRE",
		List.of("SAPPHIRE"),
		"AMETHYST",
		List.of("AMETHYST"),
		"JASPER",
		List.of("JASPER"),
		"RUBY",
		List.of("RUBY"),
		"JADE",
		List.of("JADE"),
		"OPAL",
		List.of("OPAL"),
		"COMBAT",
		List.of("SAPPHIRE", "AMETHYST", "JASPER", "RUBY"),
		"OFFENSIVE",
		List.of("SAPPHIRE", "JASPER"),
		"DEFENSIVE",
		List.of("AMETHYST", "RUBY", "OPAL"),
		"MINING",
		List.of("JADE", "AMBER", "TOPAZ"),
		"UNIVERSAL",
		List.of("AMBER", "TOPAZ", "SAPPHIRE", "AMETHYST", "JASPER", "RUBY", "JADE", "OPAL")
	);
	// --
	private final SlashCommandEvent slashCommandEvent;
	private Message message;
	private final String category;
	private final NetworthExecute calculator;
	private final String itemId;
	// Added values
	private int recombobulatorCount = 0;
	private final Set<String> enchants = new HashSet<>();
	private int hpbCount = 0;
	private int fpbCount = 0;
	private int stars = 0;
	private String reforge = null;
	private String rune = null;
	private final Map<String, String> gemstones = new HashMap<>();
	private String drillUpgradeModule = null;
	private String drillFuelTank = null;
	private String drillEngine = null;
	private String dye = null;
	private String accessoryEnrichment = null;
	private int manaDisintegratorCount = 0;
	private int ffdCount = 0;
	private final Set<String> necronBladeScrolls = new HashSet<>();
	private String skin = null;
	private String powerScroll = null;
	private int woodSingularityCount = 0;
	private int artOfWarCount = 0;
	private int artOfPeaceCount = 0;
	private boolean etherwarpApplied = false;
	private int transmissionTunerCount = 0;

	public CraftCommandHandler(String itemId, SlashCommandEvent slashCommandEvent) {
		// TODO: Pets (pet skin & held item), attributes, silex

		this.itemId = itemId;
		this.slashCommandEvent = slashCommandEvent;
		this.calculator = new NetworthExecute().initPrices();

		JsonElement itemInfo = getSkyblockItemsJson().get(itemId);
		this.category = higherDepth(itemInfo, "category", null);
		if (category == null || ignoredCategories.contains(category)) {
			slashCommandEvent.embed(errorEmbed("This item is not supported"));
			return;
		}

		StringSelectMenu.Builder selectMenu = StringSelectMenu
			.create("craft_command_main")
			.addOption("Toggle Recombobulator", "toggle_recombobulator");

		if (higherDepth(getEnchantsJson(), "enchants." + category) != null) {
			selectMenu.addOption("Enchants", "enchants");
		}

		if (weaponCategories.contains(category) || armorCategories.contains(category)) {
			selectMenu.addOption("Potato Books", "potato_books");
		}

		if (higherDepth(getEssenceCostsJson(), itemId) != null) {
			selectMenu.addOption("Stars", "stars");
		}

		if (CATEGORY_TO_REFORGES.containsKey(category)) {
			selectMenu.addOption("Reforge", "reforge");
		}

		if (weaponCategories.contains(category) || category.equals("CHESTPLATE")) {
			selectMenu.addOption("Rune", "rune");
		}

		if (higherDepth(itemInfo, "gemstone_slots") != null) {
			selectMenu.addOption("Gemstones", "gemstones");
		}

		if (category.equals("DRILL")) {
			selectMenu.addOption("Drill Upgrades", "drill_upgrades");
		}

		if (dyeMaterials.contains(higherDepth(itemInfo, "material", ""))) {
			selectMenu.addOption("Dye", "dye");
		}

		if (category.equals("ACCESSORY")) {
			selectMenu.addOption("Accessory Enrichment", "accessory_enrichment");
		}

		if (category.equals("DEPLOYABLE") || category.equals("WAND")) {
			selectMenu.addOption("Mana Disintegrator", "mana_disintegrator");
		}

		if (category.equals("HOE") || category.equals("AXE")) {
			selectMenu.addOption("Farming For Dummies", "farming_for_dummies");
		}

		if (necronBladeScrollItems.contains(itemId)) {
			selectMenu.addOption("Necron's Blade Scrolls", "necron_blade_scrolls");
		}

		if (higherDepth(getInternalJsonMappings(), itemId + ".skins.[0]") != null) {
			selectMenu.addOption("Skin", "skin");
		}

		if (higherDepth(getInternalJsonMappings(), itemId + ".scrollable", false)) {
			selectMenu.addOption("Power Scroll", "power_scroll");
		}

		if (woodSingularityItems.contains(itemId)) {
			selectMenu.addOption("Toggle Wood Singularity", "toggle_wood_singularity");
		}

		if (weaponCategories.contains(category)) {
			selectMenu.addOption("Toggle Art Of War", "toggle_art_of_war");
		}

		if (armorCategories.contains(category)) {
			selectMenu.addOption("Toggle Art Of Peace", "toggle_art_of_peace");
		}

		if (itemId.equals("ASPECT_OF_THE_END") || itemId.equals("ASPECT_OF_THE_VOID") || itemId.equals("SINSEEKER_SCYTHE")) {
			selectMenu.addOption("Transmission Tuners", "transmission_tuners");
		}

		if (itemId.equals("ASPECT_OF_THE_END") || itemId.equals("ASPECT_OF_THE_VOID")) {
			selectMenu.addOption("Toggle Etherwarp", "toggle_etherwarp");
		}

		slashCommandEvent
			.getHook()
			.editOriginalEmbeds(getMainMessage().build())
			.setComponents(
				ActionRow.of(selectMenu.build()),
				ActionRow.of(Button.primary("craft_command_search_0", "Search Auction House For Matches"))
			)
			.queue(
				m -> {
					message = m;
					m
						.editMessageComponents(
							ActionRow.of(selectMenu.build()),
							ActionRow.of(Button.primary("craft_command_search_" + message.getId(), "Search Auction House For Matches"))
						)
						.queue();
					waitForEvent();
				},
				ignore
			);
	}

	private boolean condition(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			if (event.isFromGuild() && event.getUser().getId().equals(slashCommandEvent.getUser().getId())) {
				if (event.getMessageId().equals(message.getId())) {
					return true;
				}

				if (event.getComponentId().startsWith("craft_command_")) {
					String[] split = event.getComponentId().split("_");
					return split[split.length - 1].equals(message.getId());
				}
			}
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			if (event.isFromGuild() && event.getUser().getId().equals(slashCommandEvent.getUser().getId())) {
				if (event.getModalId().startsWith("craft_command_")) {
					String[] split = event.getModalId().split("_");
					return split[split.length - 1].equals(message.getId());
				}
			}
		} else if (genericEvent instanceof ButtonInteractionEvent event) {
			if (event.isFromGuild() && event.getUser().getId().equals(slashCommandEvent.getUser().getId())) {
				if (event.getComponentId().startsWith("craft_command_search_")) {
					String[] split = event.getComponentId().split("_");
					return split[split.length - 1].equals(message.getId());
				}
			}
		}
		return false;
	}

	private void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			onStringSelectInteraction(event);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			onModalEvent(event);
		} else if (genericEvent instanceof ButtonInteractionEvent event) {
			AbstractMap.SimpleEntry<String, Integer> ahSearchUrl = getAhSearchUrl();
			if (ahSearchUrl.getValue() == 0) {
				event.replyEmbeds(errorEmbed("At least one upgrade must be added").build()).setEphemeral(true).queue();
			} else {
				event
					.deferReply()
					.queue(e -> {
						JsonElement items = getJson(ahSearchUrl.getKey());
						if (items == null || !items.isJsonArray()) {
							e.editOriginalEmbeds(errorEmbed("Error fetching").build()).queue();
							return;
						}
						new CraftCommandPaginator(items.getAsJsonArray(), ahSearchUrl.getValue(), event);
					});
			}
		}

		waitForEvent();
	}

	private void onStringSelectInteraction(StringSelectInteractionEvent event) {
		if (event.getComponentId().equals("craft_command_main")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "toggle_recombobulator" -> {
					recombobulatorCount = recombobulatorCount == 0 ? 1 : 0;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Recombobulated: " + (recombobulatorCount == 1)).build())
						.setEphemeral(true)
						.queue();
				}
				case "enchants" -> event
					.replyEmbeds(
						defaultEmbed("Craft Helper").setDescription("Select an option from the menu below to modify enchants").build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_enchants_main_" + message.getId())
							.addOption("Add Enchant", "add_enchant")
							.addOption("Remove Enchant", "remove_enchant")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "gemstones" -> event
					.replyEmbeds(
						defaultEmbed("Craft Helper").setDescription("Select an option from the menu below to modify gemstones").build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_gemstones_main_" + message.getId())
							.addOption("Add Gemstone", "add_gemstone")
							.addOption("Remove Gemstone", "remove_gemstone")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "drill_upgrades" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a drill upgrade from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_main_" + message.getId())
							.addOption("Drill Upgrade Module", "drill_upgrade_module")
							.addOption("Drill Fuel Tank", "drill_fuel_tank")
							.addOption("Drill Engine", "drill_engine")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "potato_books" -> event
					.replyModal(
						Modal
							.create("craft_command_pbs_" + message.getId(), "Craft Helper - Potato Books")
							.addActionRow(
								TextInput
									.create("hpb", "Hot Potato Books Count", TextInputStyle.SHORT)
									.setValue("" + hpbCount)
									.setRequired(false)
									.build()
							)
							.addActionRow(
								TextInput
									.create("fpb", "Fuming Potato Books Count", TextInputStyle.SHORT)
									.setValue("" + fpbCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
				case "dye" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a dye from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_dye_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								dyes.stream().map(o -> SelectOption.of(idToName(o), o)).collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "accessory_enrichment" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select an accessory enrichment from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_accessory_enrichments_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								accessoryEnrichments
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "mana_disintegrator" -> event
					.replyModal(
						Modal
							.create("craft_command_mana_disintegrator_" + message.getId(), "Craft Helper - Mana Disintegrator")
							.addActionRow(
								TextInput
									.create("value", "Mana Disintegrator Count", TextInputStyle.SHORT)
									.setValue("" + manaDisintegratorCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
				case "farming_for_dummies" -> event
					.replyModal(
						Modal
							.create("craft_command_ffd_" + message.getId(), "Craft Helper - Farming For Dummies")
							.addActionRow(
								TextInput
									.create("value", "Farming For Dummies Count", TextInputStyle.SHORT)
									.setValue("" + ffdCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
				case "necron_blade_scrolls" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select necron's blade scrolls from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_necron_blade_scrolls_" + message.getId())
							.addOption("None", "none")
							.addOption("Implosion", "IMPLOSION_SCROLL")
							.addOption("Shadow Warp", "SHADOW_WARP_SCROLL")
							.addOption("Wither Shield", "WITHER_SHIELD_SCROLL")
							.setMaxValues(3)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "skin" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a skin from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_skin_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								streamJsonArray(higherDepth(getInternalJsonMappings(), itemId + ".skins"))
									.map(o -> SelectOption.of(idToName(o.getAsString()), o.getAsString()))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "power_scroll" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a scroll from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_power_scroll_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								powerScrolls
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "stars" -> event
					.replyModal(
						Modal
							.create("craft_command_stars_" + message.getId(), "Craft Helper - Stars")
							.addActionRow(TextInput.create("value", "Stars", TextInputStyle.SHORT).setValue("" + stars).build())
							.build()
					)
					.queue();
				case "reforge" -> event
					.replyModal(
						Modal
							.create("craft_command_reforge_" + message.getId(), "Craft Helper - Reforge")
							.addActionRow(TextInput.create("value", "Reforge", TextInputStyle.SHORT).setValue(reforge).build())
							.build()
					)
					.queue();
				case "rune" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a rune from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_rune_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								networthRunes
									.stream()
									.filter(o -> category.equals("CHESTPLATE") ^ o.startsWith("MUSIC_RUNE"))
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "toggle_wood_singularity" -> {
					woodSingularityCount = woodSingularityCount == 0 ? 1 : 0;
					event
						.replyEmbeds(
							defaultEmbed("Craft Helper").setDescription("Wood Singularity Applied: " + (woodSingularityCount == 1)).build()
						)
						.setEphemeral(true)
						.queue();
				}
				case "toggle_art_of_war" -> {
					artOfWarCount = artOfWarCount == 0 ? 1 : 0;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Art Of War Applied: " + (artOfWarCount == 1)).build())
						.setEphemeral(true)
						.queue();
				}
				case "toggle_art_of_peace" -> {
					artOfPeaceCount = artOfPeaceCount == 0 ? 1 : 0;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Art Of Peace Applied: " + artOfPeaceCount).build())
						.setEphemeral(true)
						.queue();
				}
				case "toggle_etherwarp" -> {
					etherwarpApplied = !etherwarpApplied;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Etherwarp Applied: " + etherwarpApplied).build())
						.setEphemeral(true)
						.queue();
				}
				case "transmission_tuners" -> event
					.replyModal(
						Modal
							.create("craft_command_transmission_tuners_" + message.getId(), "Craft Helper - Transmission Tuners")
							.addActionRow(
								TextInput
									.create("value", "Transmission Tuners Count", TextInputStyle.SHORT)
									.setValue("" + transmissionTunerCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
			}
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_enchants_main_")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "add_enchant" -> event
					.replyModal(
						Modal
							.create("craft_command_enchants_add_" + message.getId(), "Craft Helper - Add Enchant")
							.addActionRow(TextInput.create("value", "Enchant Name & Level", TextInputStyle.SHORT).build())
							.build()
					)
					.queue();
				case "remove_enchant" -> {
					if (enchants.isEmpty()) {
						event.editMessageEmbeds(errorEmbed("No enchants added").build()).queue();
					} else {
						event
							.replyModal(
								Modal
									.create("craft_command_enchants_remove_" + message.getId(), "Craft Helper - Remove Enchant")
									.addActionRow(TextInput.create("value", "Enchant Name & Level", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
					}
				}
			}
		} else if (event.getComponentId().startsWith("craft_command_gemstones_main_")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "add_gemstone" -> event
					.editMessageEmbeds(
						defaultEmbed("Craft Helper").setDescription("Select a gemstone slot to add from the menu below").build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_gemstones_add_" + message.getId())
							.addOptions(
								streamJsonArray(higherDepth(getSkyblockItemsJson().get(itemId), "gemstone_slots"))
									.map(e -> higherDepth(e, "formatted_slot_type").getAsString())
									.filter(e -> !gemstones.containsKey(e))
									.map(e -> SelectOption.of(capitalizeString(e.replace("_", "  ")) + " Slot", e))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
				case "remove_gemstone" -> {
					if (gemstones.isEmpty()) {
						event.editMessageEmbeds(errorEmbed("No gemstones added").build()).queue();
					} else {
						event
							.editMessageEmbeds(
								defaultEmbed("Craft Helper").setDescription("Select a gemstone slot to remove from the menu below").build()
							)
							.setActionRow(
								StringSelectMenu
									.create("craft_command_gemstones_remove_" + message.getId())
									.addOptions(
										gemstones
											.entrySet()
											.stream()
											.map(e ->
												SelectOption.of(
													capitalizeString(e.getKey().replace("_", " ")) + " Slot - " + idToName(e.getValue()),
													e.getKey()
												)
											)
											.collect(Collectors.toCollection(ArrayList::new))
									)
									.build()
							)
							.queue();
					}
				}
			}
		} else if (event.getComponentId().startsWith("craft_command_gemstones_add_")) {
			SelectOption gemstoneSlot = event.getSelectedOptions().get(0);
			List<String> gemstoneVarieties = slotTypeToGemstones.get(gemstoneSlot.getValue().split("_")[0]);
			if (gemstoneVarieties.size() > 1) {
				event
					.editMessageEmbeds(
						defaultEmbed("Craft Helper")
							.setDescription(
								"Select a gemstone variety for the " + gemstoneSlot.getLabel().toLowerCase() + " from the menu below"
							)
							.build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_gemstones_variety_add_" + gemstoneSlot.getValue() + "_" + message.getId())
							.addOptions(
								slotTypeToGemstones
									.get(gemstoneSlot.getValue().split("_")[0])
									.stream()
									.map(e -> SelectOption.of(capitalizeString(e) + " Variety", e))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
			} else {
				String gemstoneVariety = gemstoneVarieties.get(0);
				event
					.editMessageEmbeds(
						defaultEmbed("Craft Helper")
							.setDescription(
								"Select a gemstone tier for the " + gemstoneSlot.getLabel().toLowerCase() + " from the menu below"
							)
							.build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_gemstones_tier_add_" + gemstoneSlot.getValue() + "_" + message.getId())
							.addOptions(
								gemstoneTiers
									.stream()
									.map(e -> e + "_" + gemstoneVariety + "_GEM")
									.map(e -> SelectOption.of(idToName(e), e))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
			}
		} else if (event.getComponentId().startsWith("craft_command_gemstones_variety_add_")) {
			String[] gemstoneSlotSplit = event.getComponentId().split("craft_command_gemstones_variety_add_")[1].split("_");
			// 						slot type					slot num
			String gemstoneSlot = gemstoneSlotSplit[0] + "_" + gemstoneSlotSplit[1];
			String gemstoneVariety = event.getSelectedOptions().get(0).getValue();
			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription(
							"Select a gemstone tier for the " + gemstoneSlot.replace("_", " ").toLowerCase() + " slot from the menu below"
						)
						.build()
				)
				.setActionRow(
					StringSelectMenu
						.create("craft_command_gemstones_tier_add_" + gemstoneSlot + "_" + message.getId())
						.addOptions(
							gemstoneTiers
								.stream()
								.map(e -> e + "_" + gemstoneVariety + "_GEM")
								.map(e -> SelectOption.of(idToName(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getComponentId().startsWith("craft_command_gemstones_tier_add_")) {
			String[] gemstoneSlotSplit = event.getComponentId().split("craft_command_gemstones_tier_add_")[1].split("_");
			String gemstoneSlot = gemstoneSlotSplit[0] + "_" + gemstoneSlotSplit[1];
			SelectOption gemstone = event.getSelectedOptions().get(0);
			gemstones.put(gemstoneSlot, gemstone.getValue());

			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription("Added " + gemstoneSlot.replace("_", " ").toLowerCase() + " slot: " + gemstone.getLabel())
						.build()
				)
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_gemstones_remove_")) {
			SelectOption gemstoneSlot = event.getSelectedOptions().get(0);
			gemstones.remove(gemstoneSlot.getValue());

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Removed gemstone slot: " + gemstoneSlot.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_main_")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "drill_upgrade_module" -> event
					.editMessageEmbeds(
						defaultEmbed("Craft Helper").setDescription("Select a drill upgrade module from the menu below").build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_upgrade_module_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								drillUpgradeModules
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
				case "drill_fuel_tank" -> event
					.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Select a drill fuel tank from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_fuel_tank_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								drillFuelTanks
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
				case "drill_engine" -> event
					.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Select a drill engine from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_engine_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								drillEngines
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
			}
		} else if (event.getComponentId().startsWith("craft_command_enchants_add_choose_")) {
			String enchant = event.getSelectedOptions().get(0).getValue();
			enchants.add(enchant);

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Added enchant: " + idToName(enchant)).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_enchants_remove_choose_")) {
			String enchant = event.getSelectedOptions().get(0).getValue();
			enchants.remove(enchant);

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Removed enchant: " + idToName(enchant)).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_reforge_choose_")) {
			reforge = event.getSelectedOptions().get(0).getValue();

			event.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Reforge: " + reforge).build()).setComponents().queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_rune_")) {
			SelectOption runeValue = event.getSelectedOptions().get(0);
			rune = runeValue.getValue().equals("none") ? null : runeValue.getValue();

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Rune: " + runeValue.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_necron_blade_scrolls_")) {
			necronBladeScrolls.clear();
			List<SelectOption> selectedScrolls = event.getSelectedOptions();
			for (SelectOption selectedScroll : selectedScrolls) {
				if (selectedScroll.getValue().equals("none")) {
					event
						.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Removed all necron's blade scrolls").build())
						.setComponents()
						.queue();
					updateMainMessage();
					return;
				} else {
					necronBladeScrolls.add(selectedScroll.getValue());
				}
			}

			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription(
							"Added necron's blade scrolls: " +
							necronBladeScrolls.stream().map(StringUtils::idToName).collect(Collectors.joining(", "))
						)
						.build()
				)
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_skin_")) {
			SelectOption skinValue = event.getSelectedOptions().get(0);
			skin = skinValue.getValue().equals("none") ? null : skinValue.getValue();

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Skin: " + skinValue.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_power_scroll_")) {
			SelectOption powerScrollValue = event.getSelectedOptions().get(0);
			powerScroll = powerScrollValue.getValue().equals("none") ? null : powerScrollValue.getValue();

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Power Scroll: " + powerScrollValue.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_upgrade_module_")) {
			SelectOption drillUpgradeModuleValue = event.getSelectedOptions().get(0);
			drillUpgradeModule = drillUpgradeModuleValue.getValue().equals("none") ? null : drillUpgradeModuleValue.getValue();

			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper").setDescription("Drill upgrade module: " + drillUpgradeModuleValue.getLabel()).build()
				)
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_fuel_tank_")) {
			SelectOption drillFuelTankValue = event.getSelectedOptions().get(0);
			drillFuelTank = drillFuelTankValue.getValue().equals("none") ? null : drillFuelTankValue.getValue();

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Drill fuel tank: " + drillFuelTankValue.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_engine_")) {
			SelectOption drillEngineValue = event.getSelectedOptions().get(0);
			drillEngine = drillEngineValue.getValue().equals("none") ? null : drillEngineValue.getValue();

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Drill engine: " + drillEngineValue.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_dye_")) {
			SelectOption dyeValue = event.getSelectedOptions().get(0);
			dye = dyeValue.getValue().equals("none") ? null : dyeValue.getValue();

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Dye: " + dyeValue.getLabel()).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_accessory_enrichments_")) {
			SelectOption accessoryEnrichmentValue = event.getSelectedOptions().get(0);
			accessoryEnrichment = accessoryEnrichmentValue.getValue().equals("none") ? null : accessoryEnrichmentValue.getValue();

			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper").setDescription("Accessory enrichment: " + accessoryEnrichmentValue.getLabel()).build()
				)
				.setComponents()
				.queue();
			updateMainMessage();
		}
	}

	private void onModalEvent(ModalInteractionEvent event) {
		if (event.getModalId().startsWith("craft_command_enchants_add_")) {
			List<String> usedEnchants = enchants.stream().map(e -> e.split(";")[0]).collect(Collectors.toCollection(ArrayList::new));
			Set<String> validEnchants = new HashSet<>();
			for (JsonElement enchant : higherDepth(getEnchantsJson(), "enchants." + category).getAsJsonArray()) {
				String enchantStr = enchant.getAsString().toUpperCase();

				if (usedEnchants.contains(enchantStr)) {
					continue;
				}

				for (int i = 1; i <= 10; i++) {
					String enchantNameLevel = enchantStr + ";" + i;
					if (higherDepth(getInternalJsonMappings(), enchantNameLevel) != null) {
						validEnchants.add(enchantNameLevel);
					}
				}
			}

			List<String> closestMatches = getClosestMatchesFromIds(event.getValues().get(0).getAsString(), validEnchants, 10);
			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Select an enchant to add from the menu below").build())
				.setActionRow(
					StringSelectMenu
						.create("craft_command_enchants_add_choose_" + message.getId())
						.addOptions(
							closestMatches
								.stream()
								.map(e -> SelectOption.of(idToName(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getModalId().startsWith("craft_command_enchants_remove_")) {
			List<String> closestMatches = getClosestMatchesFromIds(event.getValues().get(0).getAsString(), enchants, 10);
			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Select an enchant to remove from the menu below").build())
				.setActionRow(
					StringSelectMenu
						.create("craft_command_enchants_remove_choose_" + message.getId())
						.addOptions(
							closestMatches
								.stream()
								.map(e -> SelectOption.of(idToName(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getModalId().startsWith("craft_command_pbs_")) {
			String hpbValue = event.getValue("hpb").getAsString();
			int hpbValueInt = 0;
			if (!hpbValue.isEmpty()) {
				try {
					hpbValueInt = Integer.parseInt(hpbValue);
				} catch (Exception e) {
					hpbValueInt = -1;
				}

				if (hpbValueInt < 0 || hpbValueInt > 10) {
					event.replyEmbeds(errorEmbed("Hot potato book count must be between 0 and 10").build()).setEphemeral(true).queue();
					return;
				}
			}

			String fpbValue = event.getValue("fpb").getAsString();
			int fpbValueInt = 0;
			if (!fpbValue.isEmpty()) {
				try {
					fpbValueInt = Integer.parseInt(fpbValue);
				} catch (Exception e) {
					fpbValueInt = -1;
				}

				if (fpbValueInt < 0 || fpbValueInt > 5) {
					event.replyEmbeds(errorEmbed("Fuming potato book count must be between 0 and 5").build()).setEphemeral(true).queue();
					return;
				}
			}

			if (fpbValueInt > 0 && hpbValueInt != 10) {
				event
					.replyEmbeds(errorEmbed("Fuming potato books can only be added if hot potato books are maxed out").build())
					.setEphemeral(true)
					.queue();
				return;
			}

			hpbCount = hpbValueInt;
			fpbCount = fpbValueInt;

			event
				.replyEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription("Hot potato book count: " + hpbCount + "\nFuming potato book count: " + fpbCount)
						.build()
				)
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_stars_")) {
			String starsValue = event.getValues().get(0).getAsString();
			int starsValueInt = -1;
			try {
				starsValueInt = Integer.parseInt(starsValue);
			} catch (Exception ignored) {}

			JsonElement essenceInfo = higherDepth(getEssenceCostsJson(), itemId);

			int maxStars = 5;
			for (int i = 15; i >= 1; i--) {
				if (higherDepth(essenceInfo, "" + i) != null) {
					maxStars = i;
					break;
				}
			}

			if (higherDepth(getSkyblockItemsJson().get(itemId), "dungeon_item", false)) {
				maxStars += 5;
			}

			if (starsValueInt < 0 || starsValueInt > maxStars) {
				event.replyEmbeds(errorEmbed("Stars must be between 0 and " + maxStars).build()).setEphemeral(true).queue();
				return;
			}

			stars = starsValueInt;

			event.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Stars: " + stars).build()).setEphemeral(true).queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_reforge_")) {
			List<String> closestMatches = getClosestMatches(
				event.getValues().get(0).getAsString().toLowerCase(),
				CATEGORY_TO_REFORGES.get(category),
				10
			);
			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a reforge from the menu below").build())
				.setActionRow(
					StringSelectMenu
						.create("craft_command_reforge_choose_" + message.getId())
						.addOptions(
							closestMatches
								.stream()
								.map(e -> SelectOption.of(capitalizeString(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.setEphemeral(true)
				.queue();
		} else if (event.getModalId().startsWith("craft_command_mana_disintegrator_")) {
			String manaDisintegratorValue = event.getValues().get(0).getAsString();
			int manaDisintegratorValueInt = 0;
			if (!manaDisintegratorValue.isEmpty()) {
				try {
					manaDisintegratorValueInt = Integer.parseInt(manaDisintegratorValue);
				} catch (Exception e) {
					manaDisintegratorValueInt = -1;
				}

				if (manaDisintegratorValueInt < 0 || manaDisintegratorValueInt > 10) {
					event.replyEmbeds(errorEmbed("Mana disintegrator count must be between 0 and 10").build()).setEphemeral(true).queue();
					return;
				}
			}
			manaDisintegratorCount = manaDisintegratorValueInt;

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Mana disintegrator count: " + manaDisintegratorCount).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_ffd_")) {
			String ffdValue = event.getValues().get(0).getAsString();
			int ffdValueInt = 0;
			if (!ffdValue.isEmpty()) {
				try {
					ffdValueInt = Integer.parseInt(ffdValue);
				} catch (Exception e) {
					ffdValueInt = -1;
				}

				if (ffdValueInt < 0 || ffdValueInt > 5) {
					event.replyEmbeds(errorEmbed("Farming for dummies count must be between 0 and 5").build()).setEphemeral(true).queue();
					return;
				}
			}
			ffdCount = ffdValueInt;

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Farming for dummies count: " + ffdCount).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_transmission_tuners_")) {
			String transmissionTunersValue = event.getValues().get(0).getAsString();
			int transmissionTunersValueInt = 0;
			if (!transmissionTunersValue.isEmpty()) {
				try {
					transmissionTunersValueInt = Integer.parseInt(transmissionTunersValue);
				} catch (Exception e) {
					transmissionTunersValueInt = -1;
				}

				if (transmissionTunersValueInt < 0 || transmissionTunersValueInt > 4) {
					event.replyEmbeds(errorEmbed("Transmission tuners count must be between 0 and 4").build()).setEphemeral(true).queue();
					return;
				}
			}
			transmissionTunerCount = transmissionTunersValueInt;

			event
				.replyEmbeds(
					defaultEmbed("Craft Helper").setDescription("Transmission tuners dummies count: " + transmissionTunerCount).build()
				)
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		}
	}

	private void updateMainMessage() {
		message.editMessageEmbeds(getMainMessage().build()).queue();
	}

	private EmbedBuilder getMainMessage() {
		EmbedBuilder eb = defaultEmbed("Craft Helper For " + idToName(itemId)).setThumbnail(getItemThumbnail(itemId));
		double totalPrice = 0;

		double basePrice = calculator.getLowestPrice(itemId);
		if (isCrimsonArmor(itemId, true)) {
			List<String> prestigeOrder = List.of("HOT", "BURNING", "FIERY", "INFERNAL");
			for (int j = 0; j <= prestigeOrder.indexOf(itemId.split("_")[0]); j++) {
				for (Map.Entry<String, JsonElement> entry : ARMOR_PRESTIGE_COST.getAsJsonObject(prestigeOrder.get(j)).entrySet()) {
					basePrice +=
						entry.getValue().getAsInt() *
						calculator.getLowestPrice(entry.getKey().equals("CRIMSON_ESSENCE") ? "ESSENCE_CRIMSON" : entry.getKey());
				}
			}
		}
		totalPrice += basePrice;
		eb.appendDescription("\n" + getEmoji(itemId) + " Base Price: " + roundAndFormat(basePrice));

		totalPrice += getPriceFormatted("RECOMBOBULATOR_3000", recombobulatorCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("HOT_POTATO_BOOK", hpbCount, eb.getDescriptionBuilder());
		totalPrice += getPriceFormatted("FUMING_POTATO_BOOK", fpbCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted(drillUpgradeModule, eb.getDescriptionBuilder());
		totalPrice += getPriceFormatted(drillFuelTank, eb.getDescriptionBuilder());
		totalPrice += getPriceFormatted(drillEngine, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted(rune, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted(dye, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted(accessoryEnrichment, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("MANA_DISINTEGRATOR", manaDisintegratorCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("FARMING_FOR_DUMMIES", ffdCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("TRANSMISSION_TUNER", transmissionTunerCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted(skin, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted(powerScroll, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("WOOD_SINGULARITY", woodSingularityCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("THE_ART_OF_WAR", artOfWarCount, eb.getDescriptionBuilder());

		totalPrice += getPriceFormatted("THE_ART_OF_PEACE", artOfPeaceCount, eb.getDescriptionBuilder());

		if (etherwarpApplied) {
			double etherwarpPrice = calculator.getLowestPrice("ETHERWARP_MERGER");
			etherwarpPrice += calculator.getLowestPrice("ETHERWARP_CONDUIT");
			totalPrice += etherwarpPrice;
			eb.appendDescription("\n" + getEmoji("ETHERWARP_CONDUIT") + " Etherwarp: " + roundAndFormat(etherwarpPrice));
		}

		if (reforge != null) {
			String baseRarity = higherDepth(getInternalJsonMappings(), itemId + ".base_rarity").getAsString();
			if (recombobulatorCount == 1) {
				baseRarity = NUMBER_TO_RARITY_MAP.get("" + (Integer.parseInt(RARITY_TO_NUMBER_MAP.get(baseRarity).replace(";", "")) + 1));
			}

			Map.Entry<String, JsonElement> reforgeStone = getReforgeStonesJson()
				.entrySet()
				.stream()
				.filter(e -> reforge.equalsIgnoreCase(higherDepth(e.getValue(), "reforgeName").getAsString()))
				.findAny()
				.get();

			double reforgePrice = calculator.getLowestPriceModifier(reforge, baseRarity);
			totalPrice += reforgePrice;
			eb.appendDescription(
				"\n" + getEmoji(reforgeStone.getKey()) + " " + capitalizeString(reforge) + ": " + roundAndFormat(reforgePrice)
			);
		}

		if (!enchants.isEmpty()) {
			StringBuilder ebStr = new StringBuilder();
			double totalEnchantsPrice = 0;

			for (String enchant : enchants) {
				double enchantPrice = calculator.getLowestPriceEnchant(enchant) * 0.986;
				totalEnchantsPrice += enchantPrice;
				ebStr
					.append("\n")
					.append(getEmoji(enchant))
					.append(" ")
					.append(idToName(enchant))
					.append(": ")
					.append(roundAndFormat(enchantPrice));
			}

			totalPrice += totalEnchantsPrice;
			eb.addField("Enchants (" + enchants.size() + ") | " + simplifyNumber(totalEnchantsPrice), ebStr.toString(), false);
		}

		if (stars > 0) {
			JsonElement essenceInfo = higherDepth(getEssenceCostsJson(), itemId);
			String essenceType = higherDepth(essenceInfo, "type").getAsString();
			double totalEssencePrice = 0;
			int essenceCount = 0;
			int masterStarCount = 0;
			Map<String, Integer> extraItems = new HashMap<>();

			for (int i = 0; i <= stars; i++) {
				essenceCount += higherDepth(essenceInfo, "" + i, 0);

				JsonElement items = higherDepth(essenceInfo, "items." + i);
				if (items != null) {
					for (JsonElement item : items.getAsJsonArray()) {
						String[] itemSplit = item.getAsString().split(":");
						extraItems.compute(itemSplit[0], (k, v) -> (v != null ? v : 0) + Integer.parseInt(itemSplit[1]));
					}
				}

				if (i > 5 && !essenceType.equals("Crimson")) {
					masterStarCount++;
				}
			}

			String essenceTypeFormatted = "ESSENCE_" + essenceType.toUpperCase();
			double essencePrice = essenceCount * calculator.getLowestPrice(essenceTypeFormatted);
			totalEssencePrice += essencePrice;
			StringBuilder ebStr = new StringBuilder(
				getEmoji(essenceTypeFormatted) +
				" " +
				idToName(essenceTypeFormatted) +
				" (" +
				formatNumber(essenceCount) +
				"): " +
				roundAndFormat(essencePrice)
			);

			if (masterStarCount > 0) {
				double masterStarCost = 0;
				for (int i = 1; i <= masterStarCount; i++) {
					masterStarCost +=
						calculator.getLowestPrice(
							switch (i) {
								case 5 -> "FIFTH_MASTER_STAR";
								case 4 -> "FOURTH_MASTER_STAR";
								case 3 -> "THIRD_MASTER_STAR";
								case 2 -> "SECOND_MASTER_STAR";
								case 1 -> "FIRST_MASTER_STAR";
								default -> throw new IllegalStateException("Unexpected value: " + i);
							}
						);
				}
				ebStr
					.append("\n")
					.append(getEmoji("FIRST_MASTER_STAR"))
					.append(" Master Stars (")
					.append(masterStarCount)
					.append("): ")
					.append(formatNumber(masterStarCost));
			}

			for (Map.Entry<String, Integer> extraItem : extraItems.entrySet()) {
				double extraItemPrice = extraItem.getValue() * calculator.getLowestPrice(extraItem.getKey());
				totalEssencePrice += extraItemPrice;
				ebStr
					.append("\n")
					.append(getEmoji(extraItem.getKey()))
					.append(" ")
					.append(idToName(extraItem.getKey()))
					.append(" (")
					.append(formatNumber(extraItem.getValue()))
					.append("): ")
					.append(roundAndFormat(extraItemPrice));
			}

			totalPrice += totalEssencePrice;
			eb.addField("Stars (" + stars + ") | " + simplifyNumber(totalEssencePrice), ebStr.toString(), false);
		}

		if (!gemstones.isEmpty()) {
			StringBuilder ebStr = new StringBuilder();
			double totalGemstonesPrice = 0;
			Map<String, JsonElement> slotTypeToJson = streamJsonArray(higherDepth(getSkyblockItemsJson().get(itemId), "gemstone_slots"))
				.collect(Collectors.toMap(e -> higherDepth(e, "formatted_slot_type").getAsString(), e -> e));

			for (Map.Entry<String, String> gemstone : gemstones.entrySet()) {
				JsonElement gemstoneSlotJson = slotTypeToJson.get(gemstone.getKey());
				if (higherDepth(gemstoneSlotJson, "costs") != null) {
					double gemstoneSlotUnlockPrice = 0;
					for (JsonElement cost : higherDepth(gemstoneSlotJson, "costs").getAsJsonArray()) {
						boolean costIsCoins = higherDepth(cost, "type").getAsString().equals("COINS");
						String costItemId = costIsCoins ? "SKYBLOCK_COIN" : higherDepth(cost, "item_id").getAsString();
						int costCount = (costIsCoins ? higherDepth(cost, "coins") : higherDepth(cost, "amount")).getAsInt();
						gemstoneSlotUnlockPrice += calculator.getLowestPrice(costItemId) * costCount;
					}
					totalGemstonesPrice += gemstoneSlotUnlockPrice;
					ebStr
						.append("\n")
						.append(getEmoji("GEMSTONE_MIXTURE"))
						.append(" ")
						.append(capitalizeString(gemstone.getKey().replace("_", " ")))
						.append(" Slot Unlock Cost: ")
						.append(roundAndFormat(gemstoneSlotUnlockPrice));
				}

				double gemstonePrice = calculator.getLowestPrice(gemstone.getValue());
				totalGemstonesPrice += gemstonePrice;
				ebStr
					.append("\n")
					.append(getEmoji(gemstone.getValue()))
					.append(" ")
					.append(idToName(gemstone.getValue()))
					.append(": ")
					.append(roundAndFormat(gemstonePrice));
			}

			totalPrice += totalGemstonesPrice;
			eb.addField("Gemstones (" + gemstones.size() + ") | " + simplifyNumber(totalGemstonesPrice), ebStr.toString(), false);
		}

		if (!necronBladeScrolls.isEmpty()) {
			StringBuilder ebStr = new StringBuilder();
			double totalNecronBladeScrollsPrice = 0;

			for (String necronBladeScroll : necronBladeScrolls) {
				double necronBladeScrollPrice = calculator.getLowestPrice(necronBladeScroll);
				totalNecronBladeScrollsPrice += necronBladeScrollPrice;
				ebStr
					.append("\n")
					.append(getEmoji(necronBladeScroll))
					.append(" ")
					.append(idToName(necronBladeScroll))
					.append(": ")
					.append(roundAndFormat(necronBladeScrollPrice));
			}

			totalPrice += totalNecronBladeScrollsPrice;
			eb.addField(
				"Necron's Blade Scrolls (" + necronBladeScrolls.size() + ") | " + simplifyNumber(totalNecronBladeScrollsPrice),
				ebStr.toString(),
				false
			);
		}

		eb.getDescriptionBuilder().insert(0, getEmoji("ENCHANTED_GOLD") + " **Total Price:** " + roundAndFormat(totalPrice) + "\n");
		return eb;
	}

	private double getPriceFormatted(String itemId, StringBuilder sb) {
		return getPriceFormatted(itemId, 1, sb);
	}

	private double getPriceFormatted(String itemId, int count, StringBuilder sb) {
		if (itemId == null || count == 0) {
			return 0;
		}

		double price = count * calculator.getLowestPrice(itemId);
		sb
			.append("\n")
			.append(getEmoji(itemId))
			.append(" ")
			.append(idToName(itemId))
			.append(count > 1 ? "s (" + formatNumber(count) + ")" : "")
			.append(": ")
			.append(roundAndFormat(price));
		return price;
	}

	private AbstractMap.SimpleEntry<String, Integer> getAhSearchUrl() {
		int maxScore = 0;
		URIBuilder uriBuilder = getQueryApiUrl("query");
		uriBuilder.addParameter("item_id", itemId);
		uriBuilder.addParameter("sort_by", "query");
		uriBuilder.addParameter("limit", "10");
		if (recombobulatorCount == 1) {
			uriBuilder.addParameter("recombobulated", "true");
			maxScore++;
		}
		if (!enchants.isEmpty()) {
			uriBuilder.addParameter("enchants", String.join(",", enchants));
			maxScore += enchants.size();
		}
		if (hpbCount > 0) {
			uriBuilder.addParameter("potato_book", "" + (hpbCount + fpbCount));
			maxScore++;
		}
		if (stars > 0) {
			uriBuilder.addParameter("stars", "" + stars);
			maxScore++;
		}
		if (reforge != null) {
			uriBuilder.addParameter("reforge", reforge);
			maxScore++;
		}
		if (rune != null) {
			uriBuilder.addParameter("rune", rune);
			maxScore++;
		}
		if (!gemstones.isEmpty()) {
			uriBuilder.addParameter(
				"gemstones",
				gemstones.entrySet().stream().map(e -> e.getKey() + "_" + e.getValue()).collect(Collectors.joining(","))
			);
			maxScore += gemstones.size();
		}
		if (drillUpgradeModule != null) {
			uriBuilder.addParameter("drill_upgrade_module", drillUpgradeModule);
			maxScore++;
		}
		if (drillFuelTank != null) {
			uriBuilder.addParameter("drill_fuel_tank", drillFuelTank);
			maxScore++;
		}
		if (drillEngine != null) {
			uriBuilder.addParameter("drill_engine", drillEngine);
			maxScore++;
		}
		if (dye != null) {
			uriBuilder.addParameter("dye", dye);
			maxScore++;
		}
		if (accessoryEnrichment != null) {
			uriBuilder.addParameter("accessory_enrichment", accessoryEnrichment);
			maxScore++;
		}
		if (manaDisintegratorCount > 0) {
			uriBuilder.addParameter("mana_disintegrator", "" + manaDisintegratorCount);
			maxScore++;
		}
		if (ffdCount > 0) {
			uriBuilder.addParameter("farming_for_dummies", "" + ffdCount);
			maxScore++;
		}
		if (!necronBladeScrolls.isEmpty()) {
			uriBuilder.addParameter("necron_scrolls", String.join(",", necronBladeScrolls));
			maxScore += necronBladeScrolls.size();
		}
		if (skin != null) {
			uriBuilder.addParameter("skin", skin);
			maxScore++;
		}
		if (powerScroll != null) {
			uriBuilder.addParameter("power_scroll", powerScroll);
			maxScore++;
		}
		if (woodSingularityCount == 1) {
			uriBuilder.addParameter("wood_singularity", "true");
			maxScore++;
		}
		if (artOfWarCount == 1) {
			uriBuilder.addParameter("art_of_war", "true");
			maxScore++;
		}
		if (artOfPeaceCount == 1) {
			uriBuilder.addParameter("art_of_peace", "true");
			maxScore++;
		}
		if (etherwarpApplied) {
			uriBuilder.addParameter("etherwarp", "true");
			maxScore++;
		}
		if (transmissionTunerCount == 1) {
			uriBuilder.addParameter("transmission_tuner", "" + transmissionTunerCount);
			maxScore++;
		}

		return new AbstractMap.SimpleEntry<>(uriBuilder.toString(), maxScore);
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			GenericInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> message.editMessageComponents().queue(ignore, ignore)
		);
	}
}
