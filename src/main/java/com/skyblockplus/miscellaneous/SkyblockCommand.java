package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.executor;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.ArmorStruct;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class SkyblockCommand extends Command {

	public SkyblockCommand() {
		this.name = "skyblock";
		this.cooldown = globalCooldown;
	}

	@Override
	protected void execute(CommandEvent event) {
		executor.submit(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length == 2 || args.length == 3) {
					if (args.length == 3) {
						eb = getSkyblockStats(args[1], args[2], event);
					} else {
						eb = getSkyblockStats(args[1], null, event);
					}

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorEmbed(this.name).build()).queue();
			}
		);
	}

	private EmbedBuilder getSkyblockStats(String username, String profileName, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			JsonElement profileJson = player.getProfileJson();
			JsonElement statsJson = higherDepth(player.getProfileJson(), "stats");
			JsonElement jacobJson = higherDepth(player.getProfileJson(), "jacob2");
			DateTimeFormatter dateFormatter = DateTimeFormatter
				.ofLocalizedDate(FormatStyle.LONG)
				.withLocale(Locale.US)
				.withZone(ZoneId.systemDefault());

			// General
			String generalPageString = "";
			generalPageString +=
				"**First Joined:** " +
				dateFormatter.format(Instant.ofEpochMilli(higherDepth(profileJson, "first_join").getAsLong())) +
				"\n";
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
			armorPageString += "**Wardrobe:** Use `" + BOT_PREFIX + "wardrobe` to view wardrobe armor" + "\n";

			// Skills
			String skillsPageString = "";
			skillsPageString += "__**Farming**__" + "\n";
			skillsPageString += "• **Contests Attended:** " + getJsonKeys(higherDepth(jacobJson, "contests")).size() + "\n";
			skillsPageString +=
				"• **Unique Golds:** " +
				(higherDepth(jacobJson, "unique_golds2") != null ? higherDepth(jacobJson, "unique_golds2").getAsJsonArray().size() : 0) +
				"\n";
			skillsPageString +=
				"• **Current Golds:** " +
				(higherDepth(jacobJson, "medals_inv.gold") == null ? 0 : higherDepth(jacobJson, "medals_inv.gold").getAsInt()) +
				"\n";
			skillsPageString +=
				"• **Current Silvers:** " +
				(higherDepth(jacobJson, "medals_inv.silver") == null ? 0 : higherDepth(jacobJson, "medals_inv.silver").getAsInt()) +
				"\n";
			skillsPageString +=
				"• **Current Bronzes:** " +
				(higherDepth(jacobJson, "medals_inv.bronze") == null ? 0 : higherDepth(jacobJson, "medals_inv.bronze").getAsInt()) +
				"\n";
			skillsPageString += "\n";

			skillsPageString += "__**Fishing**__" + "\n";
			skillsPageString += "• **Items Fished:** " + formatNumber(higherDepth(statsJson, "items_fished").getAsInt()) + "\n";
			skillsPageString +=
				"• **Treasures Fished:** " + formatNumber(higherDepth(statsJson, "items_fished_treasure").getAsInt()) + "\n";
			skillsPageString +=
				"• **Large Treasures Fished:** " + formatNumber(higherDepth(statsJson, "items_fished_large_treasure").getAsInt()) + "\n";
			skillsPageString +=
				"• **Fished With Shredder:** " +
				(
					higherDepth(statsJson, "shredder_fished") != null
						? formatNumber(higherDepth(statsJson, "shredder_fished").getAsInt())
						: 0
				) +
				"\n";

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
		return defaultEmbed("Invalid username or profile");
	}
}
