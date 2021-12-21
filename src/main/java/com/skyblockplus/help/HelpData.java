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

package com.skyblockplus.help;

import static com.skyblockplus.utils.Utils.capitalizeString;
import static com.skyblockplus.utils.Utils.defaultEmbed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;

public class HelpData {

	private final String name;
	private final String description;
	private final String usage;
	private final List<String> aliases = new ArrayList<>();
	private final List<String> examples = new ArrayList<>();
	private final List<HelpData> subcommands = new ArrayList<>();
	private boolean ignoreSuperCommand;
	private transient HelpData superCommand;
	private String secondDescription;
	private String secondUsage;
	private String prefix;
	private String category;

	public HelpData(String name, String description, String usage, boolean ignoreSuperCommand) {
		this(name, description, usage);
		this.ignoreSuperCommand = ignoreSuperCommand;
	}

	public HelpData(String name, String description) {
		this(name, description, name);
	}

	public HelpData(String name, String description, String usage) {
		this.name = name;
		this.description = description;
		this.usage = usage;
	}

	public String getName() {
		if (superCommand != null) {
			return superCommand.getName() + " " + name;
		}

		return name;
	}

	public HelpData addSecondData(String description, String usage) {
		secondDescription = description;
		secondUsage = usage;
		return this;
	}

	public EmbedBuilder getHelp(String subcommandName, String prefix) {
		if (subcommandName != null) {
			HelpData subcommand = subcommands.stream().filter(cmd -> cmd.matchTo(subcommandName.split(" ")[0])).findFirst().orElse(null);
			if (subcommand != null) {
				return subcommand.getHelp(subcommandName.split(" ", 2).length == 2 ? subcommandName.split(" ", 2)[1] : null, prefix);
			}
			return defaultEmbed("Invalid Command");
		}

		this.prefix = prefix;

		EmbedBuilder eb = defaultEmbed(getCategory() + " | " + capitalizeString(getName()));
		eb.addField("Description", getDescription(), false);
		eb.addField("Usage", getUsageFormatted(), false);
		if (aliases.size() > 0) {
			eb.addField(aliases.size() == 1 ? "Alias" : "Aliases", getAliases(), false);
		}
		if (examples.size() > 0) {
			eb.addField(examples.size() == 1 ? "Example" : "Examples", getExamples(), false);
		}
		if (subcommands.size() > 0) {
			eb.addField(subcommands.size() == 1 ? "Subcommand" : "Subcommands", getSubcommands(), false);
		}

		return eb;
	}

	private String getDescription() {
		return description + (secondDescription != null ? "\n\n" + secondDescription : "");
	}

	private String getCategory() {
		return getCategory(this);
	}

	private String getCategory(HelpData data) {
		if (data.category != null) {
			return data.category;
		} else if (data.superCommand != null) {
			return getCategory(data.superCommand);
		}
		return null;
	}

	public String getUsage() {
		if (superCommand != null && !ignoreSuperCommand) {
			return superCommand.getUsage() + " " + usage;
		}

		return usage;
	}

	public String getSecondUsage(HelpData command) {
		if (command.superCommand != null) {
			return command.superCommand.getUsage() + " " + command.secondUsage;
		}

		return command.secondUsage;
	}

	public String getUsageFormatted() {
		return getUsageFormatted(this);
	}

	public String getUsageFormatted(HelpData command) {
		if (command.subcommands.size() == 0) {
			return (
				"`" +
				prefix +
				command.getUsage() +
				"`" +
				(command.secondUsage != null ? "\n`" + prefix + getSecondUsage(command) + "`" : "")
			);
		}

		return (
			"`" +
			prefix +
			command.getUsage() +
			" <subcommand>`" +
			(command.secondUsage != null ? "\n`" + prefix + getSecondUsage(command) + "`" : "")
		);
	}

	public String getSubcommands() {
		StringBuilder subcommandsStr = new StringBuilder();
		for (int i = 0; i < subcommands.size(); i++) {
			if (i != 0) {
				subcommandsStr.append("\n");
			}

			subcommandsStr.append(getUsageFormatted(subcommands.get(i)));
		}

		return subcommandsStr.toString();
	}

	public HelpData addAliases(String... aliases) {
		this.aliases.addAll(Arrays.asList(aliases));
		return this;
	}

	public HelpData addExamples(String... examples) {
		this.examples.addAll(Arrays.asList(examples));
		return this;
	}

	public HelpData addSubcommands(HelpData... subcommands) {
		for (HelpData subcommand : subcommands) {
			this.subcommands.add(subcommand.setSuperCommand(this));
		}
		return this;
	}

	public String getAliases() {
		StringBuilder aliasesStr = new StringBuilder();
		for (int i = 0; i < aliases.size(); i++) {
			if (i != 0) {
				aliasesStr.append(", ");
			}

			aliasesStr.append(aliases.get(i));
		}

		return aliasesStr.toString();
	}

	public String getExamples() {
		StringBuilder examplesStr = new StringBuilder();
		for (int i = 0; i < examples.size(); i++) {
			if (i != 0) {
				examplesStr.append("\n");
			}

			examplesStr
				.append("`")
				.append(prefix)
				.append(superCommand != null && !ignoreSuperCommand ? superCommand.getUsage() + " " : "")
				.append(examples.get(i))
				.append("`");
		}

		return examplesStr.toString();
	}

	public HelpData setSuperCommand(HelpData superCommand) {
		this.superCommand = superCommand;
		return this;
	}

	public HelpData setCategory(String category) {
		this.category = category;
		return this;
	}

	public HelpData setPrefix(String prefix) {
		this.prefix = prefix;
		for (HelpData subcommand : subcommands) {
			subcommand.setPrefix(prefix);
		}
		return this;
	}

	public boolean matchTo(String cmd) {
		if (name.equalsIgnoreCase(cmd)) {
			return true;
		}

		for (String alias : aliases) {
			if (alias.equalsIgnoreCase(cmd)) {
				return true;
			}
		}

		return false;
	}
}
