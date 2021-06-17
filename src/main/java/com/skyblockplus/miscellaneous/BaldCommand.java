package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class BaldCommand extends Command {

	private final List<String> allowedGuilds = Arrays.asList("782154976243089429", "796790757947867156", "766365919231344661");

	public BaldCommand() {
		this.name = "bald";
		this.cooldown = globalCooldown;
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				if (!allowedGuilds.contains(event.getGuild().getId())) {
					return;
				}

				EmbedBuilder eb = defaultEmbed("Checking if bald...");
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

				String[] args = event.getMessage().getContentRaw().split(" ");
				if (args.length != 2) {
					eb = defaultEmbed("Invalid usage. Try `" + BOT_PREFIX + "bald @mention`");
					ebMessage.editMessage(eb.build()).queue();
					return;
				}

				try {
					String id = args[1].replaceAll("[<@!>]", "");
					User user = jda.retrieveUserById(id).complete();
					if (user != null && !user.isBot()) {
						eb = defaultEmbed("Baldness Checker");
						if (user.getId().equals("385939031596466176") || user.getId().equals("225045405526654977")) {
							eb.setDescription(user.getName() + " is not bald!");
							eb.setImage(user.getAvatarUrl());
							eb.setColor(Color.GREEN.darker());
							ebMessage.editMessage(eb.build()).queue();
							event.getMessage().addReaction(":green_check_custom:799774962394988574").queue();
							return;
						} else if (
							user.getId().equals("273873363926253568") ||
							user.getId().equals("726329299895975948") ||
							user.getId().equals("370888656803594240")
						) {
							if (new Random().nextDouble() >= 0.99) {
								eb.setDescription(user.getName() + " is not bald!");
								eb.setImage(user.getAvatarUrl());
								eb.setColor(Color.GREEN.darker());
								ebMessage.editMessage(eb.build()).queue();
								event.getMessage().addReaction(":green_check_custom:799774962394988574").queue();
							} else {
								eb.setDescription("**WARNING** - " + user.getName() + " is bald!!!");
								eb.setImage(user.getAvatarUrl());
								ebMessage.editMessage(eb.build()).queue();
								event.getMessage().addReaction("⚠️").queue();
							}
							return;
						} else {
							if (new Random().nextDouble() >= 0.25) {
								eb.setDescription(user.getName() + " is not bald!");
								eb.setImage(user.getAvatarUrl());
								eb.setColor(Color.GREEN.darker());
								ebMessage.editMessage(eb.build()).queue();
								event.getMessage().addReaction(":green_check_custom:799774962394988574").queue();
							} else {
								eb.setDescription("**WARNING** - " + user.getName() + " is bald!!!");
								eb.setImage(user.getAvatarUrl());
								ebMessage.editMessage(eb.build()).queue();
								event.getMessage().addReaction("⚠️").queue();
							}
							return;
						}
					}
				} catch (Exception ignored) {}
				ebMessage.editMessage(defaultEmbed("Invalid usage. Try `" + BOT_PREFIX + "bald @mention`").build()).queue();
			}
		)
			.start();
	}
}
