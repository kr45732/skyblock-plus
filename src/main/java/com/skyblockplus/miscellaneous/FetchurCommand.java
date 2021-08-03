package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.fetchurItems;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.time.LocalDate;
import java.time.ZoneId;
import net.dv8tion.jda.api.EmbedBuilder;

public class FetchurCommand extends Command {

	public FetchurCommand() {
		this.name = "fetchur";
		this.cooldown = globalCooldown;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				embed(getFetchurItem());
			}
		}
			.submit();
	}

	public EmbedBuilder getFetchurItem() {
		String[] fetchurItem = fetchurItems
			.get((LocalDate.now(ZoneId.of("America/New_York")).getDayOfMonth() - 1) % fetchurItems.size())
			.split("\\|");
		return defaultEmbed("Fetchur item")
			.setDescription(fetchurItem[0])
			.setThumbnail("https://sky.shiiyu.moe/item.gif/" + fetchurItem[1]);
	}
}
