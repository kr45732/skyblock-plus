package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.parseMcCodes;

import com.google.gson.*;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");
				logCommand(event.getGuild(), event.getAuthor(), content);

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

				ebMessage.editMessage(eb.build()).queue();
			}
		)
			.start();
	}
	//	public static void main(String[] args) {
	//		setApplicationSettings();
	//		File dir = new File("src/main/java/com/skyblockplus/json/NotEnoughUpdates-REPO-master/items");
	//		File[] directoryListing = dir.listFiles();
	//		List<String> enchantsList = getJsonKeys(higherDepth(getEnchantsJson(), "enchants_min_level"));
	//
	//		for (File child : directoryListing) {
	//			try {
	//				for(String enchant:enchantsList){
	//					if(child.getName().startsWith(enchant.toUpperCase())){
	//						JsonObject json = JsonParser.parseReader(new FileReader(child)).getAsJsonObject();
	//						String itemName = parseMcCodes(json.getAsJsonArray("lore").get(0).getAsString());
	//
	//						String newJson =  new GsonBuilder()
	//								.setPrettyPrinting()
	//								.create().toJson(JsonParser.parseString(json.toString().replace("Enchanted Book", itemName)));
	//						System.out.println(newJson);
	//
	//						PrintWriter prw= new PrintWriter(child.getAbsolutePath());
	//						prw.println(newJson);
	//						prw.close();
	//						break;
	//					}
	//				}
	//			}catch (Exception e){
	//				e.printStackTrace();
	//			}
	//		}
	//	}

	//	public static void main(String[] args) throws FileNotFoundException {
	//		File dir = new File("src/main/java/com/skyblockplus/json/items");
	//		File[] directoryListing = dir.listFiles();
	//		JsonObject arrayJson = new JsonObject();
	//		for (File child : directoryListing) {
	//			try {
	//				JsonElement json = JsonParser.parseReader(new FileReader(child));
	//				String itemName = parseMcCodes(higherDepth(json, "displayname").getAsString()).replace("�", "");
	//				String internalName = higherDepth(json, "internalname").getAsString();
	//				if (itemName.contains("(")) {
	//					continue;
	//				}
	//
	//				if (itemName.startsWith("[Lvl")) {
	//					itemName = internalName;
	//				}
	//				if (itemName.equals("Enchanted Book")) {
	//					itemName = parseMcCodes(higherDepth(json, "lore").getAsJsonArray().get(0).getAsString());
	//				}
	//				if (itemName.contains("⚚")) {
	//					itemName = itemName.replace("⚚ ", "STARRED ");
	//				}
	//				if (itemName.contains("Melody\\u0027s Hair")) {
	//					itemName = "MELODY_HAIR";
	//				}
	//				itemName = itemName.replace("™", "").replace("\u0027s", "").toUpperCase().replace("\u0027", "").replace(" ", "_");
	//				if (itemName.contains("MELODY_HAIR")) {
	//					itemName = "MELODY_HAIR";
	//				}
	//
	////				if (itemName.equals(internalName)) {
	////					continue;
	////				}
	//
	//				JsonArray temp = new JsonArray();
	//				if (arrayJson.has(itemName)) {
	//					temp = arrayJson.get(itemName).getAsJsonArray();
	//				}
	//
	//				temp.add(internalName);
	//				arrayJson.add(itemName, temp);
	//			} catch (Exception e) {
	//				e.printStackTrace();
	//			}
	//		}
	//		System.out.println(makeHastePost(new GsonBuilder().setPrettyPrinting().create().toJson(arrayJson)) + ".json");
	//	}

}
