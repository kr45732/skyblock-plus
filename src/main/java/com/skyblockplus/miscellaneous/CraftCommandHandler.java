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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.getClosestMatchesFromIds;
import static com.skyblockplus.utils.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class CraftCommandHandler {

	private static final List<String> ignoredCategories = List.of(
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

	private final SlashCommandEvent slashCommandEvent;
	private Message message;
	private final String category;
	private final NetworthExecute calculator;
	private final String itemId;

	// Added values
	private boolean recombobulated = false;
	private final List<String> enchants = new ArrayList<>();

	public CraftCommandHandler(String itemId, SlashCommandEvent slashCommandEvent) {
		this.itemId = itemId;
		this.slashCommandEvent = slashCommandEvent;
		this.calculator = new NetworthExecute().initPrices();

		JsonElement itemInfo = getSkyblockItemsJson().get(itemId);

		this.category = higherDepth(itemInfo, "category", null);
		if (category == null || ignoredCategories.contains(category)) {
			slashCommandEvent.embed(errorEmbed("This item is not supported"));
			return;
		}

		StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu
			.create("craft_command_main")
			.addOption("Toggle Recombobulator", "toggle_recombobulator");

		if (higherDepth(getEnchantsJson(), "enchants." + category) != null) {
			selectMenuBuilder.addOption("Enchants", "enchants");
		}

		if (weaponCategories.contains(category) || armorCategories.contains(category)) {
			selectMenuBuilder.addOption("Potato Books", "potato_books");
		}

		if (higherDepth(getEssenceCostsJson(), itemId) != null) {
			selectMenuBuilder.addOption("Stars", "stars");
			// master stars
			// essence & essence item upgrades
		}

		if (higherDepth(itemInfo, "gemstone_slots") != null) {
			selectMenuBuilder.addOption("Gemstones", "gemstones");
			// chambers too
		}

		if (category.equals("DRILL")) {
			selectMenuBuilder.addOption("Drill Upgrades", "drill_upgrades");
			// upgrade module
			// fuel tank
			// engine
		}

		if (dyeMaterials.contains(higherDepth(itemInfo, "material", ""))) {
			selectMenuBuilder.addOption("Dye", "dye");
		}

		if (category.equals("ACCESSORY")) {
			selectMenuBuilder.addOption("Talisman Enrichment", "talisman_enrichment");
		}

		if (category.equals("DEPLOYABLE") || category.equals("WAND")) {
			selectMenuBuilder.addOption("Mana Disintegrator", "mana_disintegrator");
		}

		if (category.equals("HOE") || category.equals("AXE")) {
			selectMenuBuilder.addOption("Farming For Dummies", "farming_for_dummies");
		}

		if (itemId.equals("ASPECT_OF_THE_END") || itemId.equals("ASPECT_OF_THE_VOID")) {
			selectMenuBuilder.addOption("Etherwarp Conduit", "etherwarp_conduit");
		}

		if (
			itemId.equals("ASPECT_OF_THE_END") ||
			itemId.equals("ASPECT_OF_THE_VOID") ||
			itemId.equals("SINSEEKER_SCYTHE") ||
			itemId.equals("ETHERWARP_CONDUIT")
		) {
			selectMenuBuilder.addOption("Transmission Tuner", "transmission_tuner");
			if (itemId.equals("ASPECT_OF_THE_END") || itemId.equals("ASPECT_OF_THE_VOID")) {
				selectMenuBuilder.addOption("Etherwarp Merge", "etherwarp_merge");
			}
		}

		if (woodSingularityItems.contains(itemId)) {
			selectMenuBuilder.addOption("Wood Singularity", "wood_singularity");
		}

		if (weaponCategories.contains(category)) {
			selectMenuBuilder.addOption("Art Of War", "art_of_war");
		}

		if (armorCategories.contains(category)) {
			selectMenuBuilder.addOption("Art Of Peace", "art_of_peace");
		}

		// Pet, pet skin & held item - later
		// Reforges, runes, attributes, skins - later

		slashCommandEvent
			.getHook()
			.editOriginalEmbeds(
				defaultEmbed("Craft Helper For " + idToName(itemId))
					.setDescription("Base Price: " + roundAndFormat(calculator.getLowestPrice(itemId)))
					.build()
			)
			.setActionRow(selectMenuBuilder.build())
			.queue(
				m -> {
					message = m;
					waitForEvent();
				},
				ignore
			);
	}

	public boolean condition(GenericInteractionCreateEvent genericEvent) {
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
		}
		return false;
	}

	public void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			onStringSelectInteraction(event);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			onModalEvent(event);
		}

		waitForEvent();
	}

	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		if (event.getComponentId().equals("craft_command_main")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "toggle_recombobulator" -> {
					recombobulated = !recombobulated;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Recombobulated: " + recombobulated).build())
						.setEphemeral(true)
						.queue();
					updateMainMessage();
				}
				case "enchants" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select an option from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_enchants_main_" + message.getId())
							.addOption("Add Enchant", "add_enchant")
							.addOption("Remove Enchant", "remove_enchant")
							.build()
					)
					.setEphemeral(true)
					.queue();
			}
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
		} else if (event.getComponentId().startsWith("craft_command_enchants_add_")) {
			String enchant = event.getSelectedOptions().get(0).getValue();
			enchants.add(enchant);
			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Added enchant: " + idToName(enchant)).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_enchants_remove_")) {
			String enchant = event.getSelectedOptions().get(0).getValue();
			enchants.remove(enchant);
			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Removed enchant: " + idToName(enchant)).build())
				.setComponents()
				.queue();
			updateMainMessage();
		}
	}

	private void onModalEvent(ModalInteractionEvent event) {
		List<String> validEnchants = new ArrayList<>();
		for (JsonElement enchant : higherDepth(getEnchantsJson(), "enchants." + category).getAsJsonArray()) {
			String enchantStr = enchant.getAsString().toUpperCase();

			for (int i = 1; i <= 10; i++) {
				String enchantNameLevel = enchantStr + ";" + i;
				if (higherDepth(getInternalJsonMappings(), enchantNameLevel) != null) {
					validEnchants.add(enchantNameLevel);
				}
			}
		}

		if (event.getModalId().startsWith("craft_command_enchants_add_")) {
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
		}
	}

	private void updateMainMessage() {
		EmbedBuilder eb = defaultEmbed("Craft Helper For " + idToName(itemId));
		double totalPrice = 0;

		double basePrice = calculator.getLowestPrice(itemId);
		totalPrice += basePrice;
		eb.appendDescription("\nBase Price: " + roundAndFormat(basePrice));

		if (recombobulated) {
			double recombobulatorPrice = calculator.getLowestPrice("RECOMBOBULATOR_3000");
			totalPrice += recombobulatorPrice;
			eb.appendDescription("\nRecombobulator: " + roundAndFormat(recombobulatorPrice));
		}

		if (!enchants.isEmpty()) {
			for (String enchant : enchants) {
				double enchantPrice = calculator.getLowestPrice(enchant);
				totalPrice += enchantPrice;
				eb.appendDescription("\n" + idToName(enchant) + ": " + roundAndFormat(enchantPrice));
			}
		}

		eb.getDescriptionBuilder().insert(0, "Total Price: " + roundAndFormat(totalPrice) + "\n");
		message.editMessageEmbeds(eb.build()).queue();
	}

	public void waitForEvent() {
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
