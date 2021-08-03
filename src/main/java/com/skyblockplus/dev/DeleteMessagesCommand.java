package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.concurrent.TimeUnit;

public class DeleteMessagesCommand extends Command {

	public DeleteMessagesCommand() {
		this.name = "d-purge";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					try {
						int messageCount = Math.min(Integer.parseInt(args[1]) + 1, 100);
						event.getChannel().purgeMessages(event.getChannel().getHistory().retrievePast(messageCount).complete());

						event
							.getChannel()
							.sendMessageEmbeds(defaultEmbed("Purged messages").build())
							.complete()
							.delete()
							.queueAfter(3, TimeUnit.SECONDS);
						return;
					} catch (Exception ignored) {}
				}

				event.getChannel().sendMessageEmbeds(errorEmbed(name).build()).queue();
			}
		}
			.submit();
	}
}
