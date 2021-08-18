package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;

public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[]{"ph"};
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				eb = defaultEmbed("Debug");
				eb.addField("Total", "" + (Runtime.getRuntime().totalMemory() / 1000000.0) + " MB", false);
				eb.addField("Free", "" + (Runtime.getRuntime().freeMemory() / 1000000.0) + " MB", false);
				eb.addField(
						"Used",
						"" + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) + " MB",
						false
				);
				eb.addField("Max", "" + (Runtime.getRuntime().maxMemory() / 1000000.0) + " MB", false);

				if (args.length == 2 && args[1].equals("gc")) {
					System.gc();
					eb.addField("GC RUN", "GC RUN", false);
				}

				embed(eb);
			}
		}
				.submit();
	}

	//	public static void main(String[] args) {
	//		for(String i:getEmojiMap().keySet()){
	//			if(getJson("https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/items/" + i.toUpperCase() + ".json") == null){
	//				System.out.println("BAD: " + i);
	//			}
	//		}
	//
	//		/*
	//		* BAD: euclid_wheat_hoe_tier_2
	//		* BAD: titanium_drill_dr_x555
	//		*/
	//
	//	}
}
