package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.Permission;

public class CategoriesCommand extends Command {

	public CategoriesCommand() {
		this.name = "categories";
		this.cooldown = globalCooldown;
		this.userPermissions = new Permission[] { Permission.ADMINISTRATOR };
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				StringBuilder ebString = new StringBuilder();
				for (net.dv8tion.jda.api.entities.Category category : event.getGuild().getCategories()) {
					ebString.append("\n• ").append(category.getName()).append(" ⇢ `").append(category.getId()).append("`");
				}

				embed(defaultEmbed("Guild Categories").setDescription(ebString.length() == 0 ? "None" : ebString.toString()));
			}
		}
			.submit();
	}
}
