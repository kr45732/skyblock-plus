package com.skyblockplus.dungeons;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class EssenceCommand extends Command {

  public EssenceCommand() {
    this.name = "essence";
    this.cooldown = globalCooldown;
  }

  @Override
  protected void execute(CommandEvent event) {
    new Thread(
      () -> {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event
          .getChannel()
          .sendMessage(eb.build())
          .complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        logCommand(event.getGuild(), event.getAuthor(), content);

        JsonElement essenceCostsJson = getEssenceCostsJson();

        if (args.length >= 3 && args[1].equals("upgrade")) {
          String itemName = content
            .split(" ", 3)[2].replace(" ", "_")
            .toUpperCase();

          JsonElement itemJson = higherDepth(essenceCostsJson, itemName);
          if (itemJson != null) {
            jda.addEventListener(
              new EssenceWaiter(
                itemName,
                itemJson,
                ebMessage,
                event.getAuthor()
              )
            );
          } else {
            eb = defaultEmbed("Invalid item name");
            ebMessage.editMessage(eb.build()).queue();
          }
          return;
        } else if (
          args.length >= 3 &&
          (args[1].equals("info") || args[1].equals("information"))
        ) {
          String itemName = content.split(" ", 3)[2];

          eb = getEssenceInformation(itemName, essenceCostsJson);
          if (eb == null) {
            eb = defaultEmbed("Invalid item name");
          }
          ebMessage.editMessage(eb.build()).queue();
          return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
      }
    )
      .start();
  }

  private EmbedBuilder getEssenceInformation(
    String itemName,
    JsonElement essenceCostsJson
  ) {
    String preFormattedItem = itemName
      .replace("'s", "")
      .replace(" ", "_")
      .toUpperCase();
    switch (preFormattedItem) {
      case "NECRON_HELMET":
        preFormattedItem = "POWER_WITHER_HELMET";
        break;
      case "NECRON_CHESTPLATE":
        preFormattedItem = "POWER_WITHER_CHESTPLATE";
        break;
      case "NECRON_LEGGINGS":
        preFormattedItem = "POWER_WITHER_LEGGINGS";
        break;
      case "NECRON_BOOTS":
        preFormattedItem = "POWER_WITHER_BOOTS";
        break;
      case "STORM_HELMET":
        preFormattedItem = "WISE_WITHER_HELMET";
        break;
      case "STORM_CHESTPLATE":
        preFormattedItem = "WISE_WITHER_CHESTPLATE";
        break;
      case "STORM_LEGGINGS":
        preFormattedItem = "WISE_WITHER_LEGGINGS";
        break;
      case "STORM_BOOTS":
        preFormattedItem = "WISE_WITHER_BOOTS";
        break;
      case "MAXOR_HELMET":
        preFormattedItem = "SPEED_WITHER_HELMET";
        break;
      case "MAXOR_CHESTPLATE":
        preFormattedItem = "SPEED_WITHER_CHESTPLATE";
        break;
      case "MAXOR_LEGGINGS":
        preFormattedItem = "SPEED_WITHER_LEGGINGS";
        break;
      case "MAXOR_BOOTS":
        preFormattedItem = "SPEED_WITHER_BOOTS";
        break;
      case "GOLDOR_HELMET":
        preFormattedItem = "TANK_WITHER_HELMET";
        break;
      case "GOLDOR_CHESTPLATE":
        preFormattedItem = "TANK_WITHER_CHESTPLATE";
        break;
      case "GOLDOR_LEGGINGS":
        preFormattedItem = "TANK_WITHER_LEGGINGS";
        break;
      case "GOLDOR_BOOTS":
        preFormattedItem = "TANK_WITHER_BOOTS";
        break;
      case "BONEMERANG":
        preFormattedItem = "BONE_BOOMERANG";
        break;
      case "SPIRIT_SCEPTRE":
        preFormattedItem = "BAT_WAND";
        break;
    }

    JsonElement itemJson = higherDepth(essenceCostsJson, preFormattedItem);
    EmbedBuilder eb = defaultEmbed("Essence information for " + itemName);
    if (itemJson != null) {
      String essenceType = higherDepth(itemJson, "type")
        .getAsString()
        .toLowerCase(Locale.ROOT);
      for (String level : getJsonKeys(itemJson)) {
        switch (level) {
          case "type":
            eb.setDescription(
              "**Essence Type**: " + capitalizeString(essenceType) + " essence"
            );
            break;
          case "dungeonize":
            eb.addField(
              "Dungeonize item",
              higherDepth(itemJson, level).getAsString() +
              " " +
              essenceType +
              " essence",
              false
            );
            break;
          case "1":
            eb.addField(
              level + " star",
              higherDepth(itemJson, level).getAsString() +
              " " +
              essenceType +
              " essence",
              false
            );
            break;
          default:
            eb.addField(
              level + " stars",
              higherDepth(itemJson, level).getAsString() +
              " " +
              essenceType +
              " essence",
              false
            );
            break;
        }
      }
      eb.setThumbnail("https://sky.lea.moe/item.gif/" + preFormattedItem);
      return eb;
    }
    return null;
  }
}
