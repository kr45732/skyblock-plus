package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.defaultEmbed;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;

public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
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

	//		public static void main(String[] args) throws FileNotFoundException {
	//			File dir = new File("src/main/java/com/skyblockplus/json/items");
	//			File[] directoryListing = dir.listFiles();
	//			JsonObject arrayJson = new JsonObject();
	//			Map<String, String> rarityMapRev = new HashMap<>();
	//			rarityMapRev.put("5", "Mythic");
	//			rarityMapRev.put("4", "Legendary");
	//			rarityMapRev.put("3", "Epic");
	//			rarityMapRev.put("2", "Rare");
	//			rarityMapRev.put("1", "Uncommon");
	//			rarityMapRev.put("0", "Common");
	//
	//			for (File child : directoryListing) {
	//				try {
	//					JsonElement json = JsonParser.parseReader(new FileReader(child));
	//					String itemName = parseMcCodes(higherDepth(json, "displayname").getAsString()).replace("�", "");
	//					String internalName = higherDepth(json, "internalname").getAsString();
	//					if (itemName.contains("(")) {
	//						continue;
	//					}
	//
	//					if (itemName.startsWith("[Lvl")) {
	//						itemName = rarityMapRev.get(internalName.split(";")[1]) + " " + itemName.split("] ")[1];
	//					}
	//					if (itemName.equals("Enchanted Book")) {
	//						itemName = parseMcCodes(higherDepth(json, "lore").getAsJsonArray().get(0).getAsString());
	//					}
	//					if (itemName.contains("⚚")) {
	//						itemName = itemName.replace("⚚ ", "STARRED ");
	//					}
	//					if (itemName.contains("Melody\\u0027s Hair")) {
	//						itemName = "MELODY_HAIR";
	//					}
	//					itemName = itemName.replace("™", "").replace("\u0027s", "").toUpperCase().replace("\u0027", "").replace(" ", "_");
	//					if (itemName.contains("MELODY_HAIR")) {
	//						itemName = "MELODY_HAIR";
	//					}
	//
	//					if (internalName.contains("-")) {
	//						internalName = internalName.replace("-", ":");
	//					}
	//
	//					JsonArray temp = new JsonArray();
	//					if (arrayJson.has(itemName)) {
	//						temp = arrayJson.get(itemName).getAsJsonArray();
	//					}
	//
	//					temp.add(internalName);
	//					arrayJson.add(itemName, temp);
	//				} catch (Exception e) {
	//					e.printStackTrace();
	//				}
	//			}
	//			System.out.println(makeHastePost(new GsonBuilder().setPrettyPrinting().create().toJson(arrayJson)) + ".json");
	//		}
}
