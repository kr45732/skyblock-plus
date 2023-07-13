/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.database.LeaderboardDatabase.formattedTypesSubList;
import static com.skyblockplus.utils.database.LeaderboardDatabase.getType;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.Utils.GLOBAL_COOLDOWN;
import static com.skyblockplus.utils.utils.Utils.errorEmbed;

import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.utils.StringUtils;
import groovy.lang.Tuple2;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.springframework.stereotype.Component;

@Component
public class CompareSlashCommand extends SlashCommand {

	public CompareSlashCommand() {
		this.name = "compare";
		this.cooldown = GLOBAL_COOLDOWN + 3;
	}

	public static EmbedBuilder getCompare(
		String xStatIn,
		String yStatIn,
		String username,
		Player.Gamemode gamemode,
		SlashCommandEvent event
	) {
		String xStat = getType(xStatIn);
		String yStat = getType(yStatIn);

		if (xStat.equals(yStat)) {
			return errorEmbed("The X and Y axis must be different statistics");
		}

		Player.Profile player = null;
		if (username != null) {
			player = Player.create(username);
			if (!player.isValid()) {
				return player.getErrorEmbed();
			}
		}

		Tuple2<List<Float>, List<Float>> data = leaderboardDatabase.getBiStatistics(gamemode, xStat, yStat);
		if (data == null) {
			return errorEmbed("Error fetching data");
		}

		XYChart chart = new XYChartBuilder()
			.xAxisTitle(capitalizeString(xStat.replace("_", " ")))
			.yAxisTitle(capitalizeString(yStat.replace("_", " ")))
			.theme(Styler.ChartTheme.GGPlot2)
			.build();
		XYStyler styler = chart.getStyler();
		styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter).setPlotGridLinesVisible(false).setLegendVisible(false);
		styler.setxAxisTickLabelsFormattingFunction(StringUtils::simplifyNumber);
		styler.setyAxisTickLabelsFormattingFunction(StringUtils::simplifyNumber);

		chart.addSeries("data", data.getV1(), data.getV2());
		if (player != null) {
			double playerX = player.getHighestAmount(xStat, gamemode);
			double playerY = player.getHighestAmount(yStat, gamemode);

			if (playerX != -1 && playerY != -1) {
				chart.addSeries("player", List.of(playerX), List.of(playerY)).setMarkerColor(Color.BLACK);
			}
		}

		File file = new File("src/main/java/com/skyblockplus/json/renders/out.png");
		try {
			BitmapEncoder.saveBitmap(chart, file.getPath(), BitmapEncoder.BitmapFormat.PNG);
		} catch (IOException e) {
			e.printStackTrace();
			return errorEmbed("Error rendering chart");
		}

		event.getHook().editOriginal(new MessageEditBuilder().setFiles(FileUpload.fromData(file)).build()).queue();
		return null;
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(
			getCompare(
				event.getOptionStr("x"),
				event.getOptionStr("y"),
				event.getOptionStr("player"),
				Player.Gamemode.of(event.getOptionStr("gamemode", "all")),
				event
			)
		);
	}

	@Override
	public SlashCommandData getCommandData() {
		return Commands
			.slash(name, "Compare two statistics graphically across all players")
			.addOption(OptionType.STRING, "x", "X Axis Statistic", true, true)
			.addOption(OptionType.STRING, "y", "Y Axis Statistic", true, true)
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOptions(
				new OptionData(OptionType.STRING, "gamemode", "Gamemode type")
					.addChoice("All", "all")
					.addChoice("Ironman", "ironman")
					.addChoice("Stranded", "stranded")
			);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		} else if (event.getFocusedOption().getName().equals("x") || event.getFocusedOption().getName().equals("y")) {
			event.replyClosestMatch(event.getFocusedOption().getValue(), formattedTypesSubList);
		}
	}
}
