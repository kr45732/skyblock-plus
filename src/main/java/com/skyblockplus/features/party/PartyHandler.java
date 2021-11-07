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

package com.skyblockplus.features.party;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Constants.DUNGEON_CLASS_NAMES;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.utils.command.PaginatorEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

public class PartyHandler {

	private final PaginatorEvent paginatorEvent;
	private String menuId;
	private final Message message;
	private int classIndex = 0;
	private final String username;
	private String floor;
	private final List<String> classes = new ArrayList<>(Arrays.asList("any", "any", "any", "any"));

	public PartyHandler(String username, PaginatorEvent paginatorEvent) {
		this.username = username;
		this.paginatorEvent = paginatorEvent;

		SelectionMenu menu = getMainSelectionMenu();
		this.menuId = menu.getId();
		this.message =
			paginatorEvent
				.getChannel()
				.sendMessageEmbeds(
					defaultEmbed("Party Finder Creator").setDescription("Choose an option from the menu below to get started!").build()
				)
				.setActionRow(menu)
				.complete();
		scheduleWaiter();
	}

	private boolean condition(SelectionMenuEvent event) {
		return (
			event.getUser().getId().equals(paginatorEvent.getUser().getId()) &&
			event.getChannel().getId().equals(paginatorEvent.getChannel().getId()) &&
			event.getComponentId().equals(menuId)
		);
	}

	private void action(SelectionMenuEvent event) {
		event.deferEdit().complete();

		switch (menuId) {
			case "party_finder_create_main":
				switch (event.getSelectedOptions().get(0).getValue()) {
					case "floor":
						SelectionMenu floorMenu = SelectionMenu
							.create("party_finder_create_floor")
							.addOption("Entrance", "entrance")
							.addOption("Floor 1", "floor_1")
							.addOption("Floor 2", "floor_2")
							.addOption("Floor 3", "floor_3")
							.addOption("Floor 4", "floor_4")
							.addOption("Floor 5", "floor_5")
							.addOption("Floor 6", "floor_6")
							.addOption("Floor 7", "floor_7")
							.addOption("Master floor 1", "master_floor_1")
							.addOption("Master floor 2", "master_floor_2")
							.addOption("Master floor 3", "master_floor_3")
							.addOption("Master floor 4", "master_floor_4")
							.addOption("Master floor 5", "master_floor_5")
							.addOption("Master floor 6", "master_floor_6")
							.build();
						menuId = floorMenu.getId();
						event
							.getHook()
							.editOriginalEmbeds(getCreationEmbed().setDescription("Choose a floor from the menu below").build())
							.setActionRow(floorMenu)
							.queue();
						break;
					case "class":
						classes.clear();
						classIndex = 0;
						SelectionMenu classMenu = getClassSelectionMenu("healer");
						menuId = classMenu.getId();
						event
							.getHook()
							.editOriginalEmbeds(getCreationEmbed().setDescription("Choose the number of healers").build())
							.setActionRow(classMenu)
							.queue();
						break;
					case "create":
						if (floor == null) {
							event
								.getHook()
								.editOriginalEmbeds(
									getCreationEmbed().setDescription("You must set the floor number before creating the party").build()
								)
								.queue();
							break;
						}
						guildMap
							.get(event.getGuild().getId())
							.partyList.add(new Party(username, event.getUser().getId(), floor, classes, event.getChannel().getId()));
						event
							.getHook()
							.editOriginalEmbeds(
								defaultEmbed("Party Finder Creator")
									.setDescription(
										"Successfully created the party which can be joined using `/party join " + username + "`"
									)
									.build()
							)
							.setActionRows()
							.queue();
						return;
					case "cancel":
						event
							.getHook()
							.editOriginalEmbeds(
								defaultEmbed("Party Finder Creator").setDescription("Canceled the creation process").build()
							)
							.setActionRows()
							.queue();
						return;
				}
				break;
			case "party_finder_create_floor":
				floor = event.getSelectedOptions().get(0).getValue();
				SelectionMenu mainMenu = getMainSelectionMenu();
				menuId = mainMenu.getId();
				event.getHook().editOriginalEmbeds(getCreationEmbed().build()).setActionRow(mainMenu).queue();
				break;
			default:
				if (menuId.startsWith("party_finder_create_class_")) {
					String className = menuId.split("party_finder_create_class_")[1];
					for (int i = 0; i < Integer.parseInt(event.getSelectedOptions().get(0).getValue()); i++) {
						classes.add(className);
					}

					if (classIndex + 1 == DUNGEON_CLASS_NAMES.size()) {
						int classesSize = classes.size();
						for (int i = 0; i < 4 - classesSize; i++) {
							classes.add("any");
						}
						SelectionMenu defaultMenu = getMainSelectionMenu();
						menuId = defaultMenu.getId();
						event.getHook().editOriginalEmbeds(getCreationEmbed().build()).setActionRow(defaultMenu).queue();
						break;
					}

					String nextClassName = DUNGEON_CLASS_NAMES.get(++classIndex);
					SelectionMenu nextClassMenu = getClassSelectionMenu(nextClassName);

					if (nextClassMenu == null) {
						SelectionMenu defaultMenu = getMainSelectionMenu();
						menuId = defaultMenu.getId();
						event.getHook().editOriginalEmbeds(getCreationEmbed().build()).setActionRow(defaultMenu).queue();
						break;
					}

					menuId = nextClassMenu.getId();
					event
						.getHook()
						.editOriginalEmbeds(getCreationEmbed().setDescription("Choose the number of " + nextClassName + "s").build())
						.setActionRow(nextClassMenu)
						.queue();
				}
				break;
		}

		scheduleWaiter();
	}

	private void scheduleWaiter() {
		waiter.waitForEvent(
			SelectionMenuEvent.class,
			this::condition,
			this::action,
			3,
			TimeUnit.MINUTES,
			() -> message.editMessageEmbeds(invalidEmbed("Party creation timeout").build()).setActionRows().queue()
		);
	}

	private SelectionMenu getMainSelectionMenu() {
		return SelectionMenu
			.create("party_finder_create_main")
			.addOption("Floor", "floor", "Change the requested floor number")
			.addOption("Classes", "class", "Change the requested classes")
			.addOption("Create", "create", "Create the party")
			.addOption("Cancel", "cancel", "Cancel the party creation")
			.build();
	}

	private EmbedBuilder getCreationEmbed() {
		EmbedBuilder eb = defaultEmbed("Party Finder Creator");
		eb.addField("Floor", capitalizeString(floor != null ? floor.replace("_", " ") : "Not set"), false);
		eb.addField("Requested classes", String.join(", ", classes), false);
		return eb;
	}

	private SelectionMenu getClassSelectionMenu(String className) {
		SelectionMenu.Builder classMenu = SelectionMenu.create("party_finder_create_class_" + className);
		switch (classes.size()) {
			case 0:
				classMenu.addOption("Zero", "0");
				classMenu.addOption("One", "1");
				classMenu.addOption("Two", "2");
				classMenu.addOption("Three", "3");
				classMenu.addOption("Four", "4");
				break;
			case 1:
				classMenu.addOption("Zero", "0");
				classMenu.addOption("One", "1");
				classMenu.addOption("Two", "2");
				classMenu.addOption("Three", "3");
				break;
			case 2:
				classMenu.addOption("Zero", "0");
				classMenu.addOption("One", "1");
				classMenu.addOption("Two", "2");
				break;
			case 3:
				classMenu.addOption("Zero", "0");
				classMenu.addOption("One", "1");
				break;
			case 4:
				return null;
		}

		return classMenu.build();
	}
}