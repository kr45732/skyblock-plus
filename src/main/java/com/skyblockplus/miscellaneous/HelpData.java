package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

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
	private HelpData superCommand;
	private boolean showSelf = false;
	private String secondDescription;
	private String secondUsage;

	public HelpData(String name, String description, String usage, boolean showSelf) {
		this(name, description, usage);
		this.showSelf = showSelf;
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

	public EmbedBuilder getHelp(String subcommandName) {
		if (subcommandName != null) {
			HelpData subcommand = subcommands.stream().filter(cmd -> cmd.matchTo(subcommandName)).findFirst().orElse(null);
			if (subcommand != null) {
				return subcommand.getHelp(subcommandName.split(" ", 2).length == 2 ? subcommandName.split(" ", 2)[1] : null);
			}
			return defaultEmbed("Invalid Command");
		}

		EmbedBuilder eb = defaultEmbed(capitalizeString(getName()));
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

	public String getUsage() {
		if (superCommand != null) {
			return superCommand.getUsage() + " " + usage;
		}

		return usage;
	}

	public String getUsage(String usage) {
		if (superCommand != null) {
			return superCommand.getUsage() + " " + usage;
		}

		return usage;
	}

	public String getUsageFormatted() {
		if (subcommands.size() == 0) {
			return "`" + BOT_PREFIX + getUsage() + "`" + (secondUsage != null ? "\n`" + BOT_PREFIX + getUsage(secondUsage) + "`" : "");
		}

		return (showSelf ? "`" + BOT_PREFIX + getUsage() + "\n" : "") + "`" + BOT_PREFIX + getUsage() + " [subcommand]`";
	}

	public String getSubcommands() {
		StringBuilder subcommandsStr = new StringBuilder();
		for (int i = 0; i < subcommands.size(); i++) {
			if (i != 0) {
				subcommandsStr.append("\n");
			}

			subcommandsStr.append("`").append(BOT_PREFIX).append(name).append(" ").append(subcommands.get(i).usage).append("`");
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
				.append(BOT_PREFIX)
				.append(superCommand != null ? superCommand.getUsage() + " " : "")
				.append(examples.get(i))
				.append("`");
		}

		return examplesStr.toString();
	}

	public HelpData setSuperCommand(HelpData superCommand) {
		this.superCommand = superCommand;
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
