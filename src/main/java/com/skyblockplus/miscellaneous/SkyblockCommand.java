package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.ArmorStruct;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class SkyblockCommand extends Command {

	public SkyblockCommand() {
		this.name = "skyblock";
		this.cooldown = globalCooldown;
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getSkyblockStats(username, args.length == 3 ? args[2] : null, event));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}

	private EmbedBuilder getSkyblockStats(String username, String profileName, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			JsonElement profileJson = player.profileJson();
			JsonElement statsJson = higherDepth(player.profileJson(), "stats");
			JsonElement jacobJson = higherDepth(player.profileJson(), "jacob2");

			// General
			String generalPageString = "";
			generalPageString +=
				"**First Joined:** <t:" +
				Instant.ofEpochMilli(higherDepth(profileJson, "first_join").getAsLong()).getEpochSecond() +
				":D>\n";
			generalPageString += "**Purse:** " + simplifyNumber(player.getPurseCoins()) + "\n";
			generalPageString +=
				"**Bank Account:** " +
				(player.getBankBalance() == -1.0 ? "Banking API disabled" : simplifyNumber(player.getBankBalance())) +
				"\n";
			generalPageString += "**Skill Average:** " + roundAndFormat(player.getSkillAverage()) + "\n";
			generalPageString += "**Fairy Souls:** " + player.getFairySouls() + " / 220" + "\n";

			// Armor
			String armorPageString = "";
			ArmorStruct invArmor = player.getInventoryArmor();
			armorPageString += "**Equipped Helmet:** " + invArmor.getHelmet() + "\n";
			armorPageString += "**Equipped Chestplate:** " + invArmor.getChestplate() + "\n";
			armorPageString += "**Equipped Leggings:** " + invArmor.getLeggings() + "\n";
			armorPageString += "**Equipped Boots:** " + invArmor.getBoots() + "\n";
			armorPageString += "**Wardrobe:** Use `" + getGuildPrefix(event.getGuild().getId()) + "wardrobe` to view wardrobe armor" + "\n";

			// Skills
			String skillsPageString = "";
			skillsPageString += "__**Farming**__" + "\n";
			skillsPageString += "• **Contests Attended:** " + getJsonKeys(higherDepth(jacobJson, "contests")).size() + "\n";
			skillsPageString +=
				"• **Unique Golds:** " +
				(higherDepth(jacobJson, "unique_golds2") != null ? higherDepth(jacobJson, "unique_golds2").getAsJsonArray().size() : 0) +
				"\n";
			skillsPageString += "• **Current Golds:** " + higherDepth(jacobJson, "medals_inv.gold", 0) + "\n";
			skillsPageString += "• **Current Silvers:** " + higherDepth(jacobJson, "medals_inv.silver", 0) + "\n";
			skillsPageString += "• **Current Bronzes:** " + higherDepth(jacobJson, "medals_inv.bronze", 0) + "\n";
			skillsPageString += "\n";

			skillsPageString += "__**Fishing**__" + "\n";
			skillsPageString += "• **Items Fished:** " + formatNumber(higherDepth(statsJson, "items_fished", 0)) + "\n";
			skillsPageString += "• **Treasures Fished:** " + formatNumber(higherDepth(statsJson, "items_fished_treasure", 0)) + "\n";
			skillsPageString +=
				"• **Large Treasures Fished:** " + formatNumber(higherDepth(statsJson, "items_fished_large_treasure", 0)) + "\n";
			skillsPageString += "• **Fished With Shredder:** " + (higherDepth(statsJson, "shredder_fished", 0)) + "\n";

			// Dungeons
			String dungeonsPageString = "";
			dungeonsPageString += "__**Classes**__" + "\n";
			dungeonsPageString += "• **Healer:** " + roundAndFormat(player.getDungeonClassLevel("healer")) + "\n";
			dungeonsPageString += "• **Mage:** " + roundAndFormat(player.getDungeonClassLevel("mage")) + "\n";
			dungeonsPageString += "• **Berserk:** " + roundAndFormat(player.getDungeonClassLevel("berserk")) + "\n";
			dungeonsPageString += "• **Archer:** " + roundAndFormat(player.getDungeonClassLevel("archer")) + "\n";
			dungeonsPageString += "• **Tank:** " + roundAndFormat(player.getDungeonClassLevel("tank")) + "\n";
			dungeonsPageString += "\n";

			dungeonsPageString += "__**Catacombs**__" + "\n";
			dungeonsPageString += "• **Catacombs:** " + roundAndFormat(player.getCatacombsLevel()) + "\n";

			String[] pageTitles = { "General", "Armor", "Skills", "Dungeons" };
			CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor())
				.setColumns(1)
				.setItemsPerPage(1)
				.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
			paginateBuilder.addItems(generalPageString, armorPageString, skillsPageString, dungeonsPageString);
			paginateBuilder.build().paginate(event.getChannel(), 0);

			return null;
		}
		return invalidEmbed(player.getFailCause());
	}
}
