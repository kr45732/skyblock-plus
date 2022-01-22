/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
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

package com.skyblockplus.features.apply.log;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.MiscUtil;

public class ApplyLog {

	public static LogMessage toLog(Message message) {
		if(message.getMember() == null){
			try{message.getGuild().retrieveMember(message.getAuthor()).complete();}catch (Exception ignored){}
		}
		LogMessage.Builder builder = LogMessage
			.builder()
			.id(message.getId())
			.user(
				LogUser
					.builder()
					.name(message.getMember().getEffectiveName())
					.avatar(message.getMember().getEffectiveAvatarUrl())
					.isBot(message.getAuthor().isBot())
					.isVerified(message.getAuthor().getFlags().contains(User.UserFlag.VERIFIED_BOT))
					.color(toHex(message.getMember().getColorRaw()))
					.build()
			);

		String text = message.getContentRaw();
		for (Member mentionedMember : message.getMentionedMembers()) {
			text =
				text
					.replace(
						"<@" + mentionedMember.getId() + ">",
						"<discord-mention>" + mentionedMember.getEffectiveName() + "</discord-mention>"
					)
					.replace(
						"<@!" + mentionedMember.getId() + ">",
						"<discord-mention>" + mentionedMember.getEffectiveName() + "</discord-mention>"
					);
		}
		for (TextChannel mentionedChannel : message.getMentionedChannels()) {
			text =
				text.replace(
					mentionedChannel.getAsMention(),
					"<discord-mention type=\"channel\">" + mentionedChannel.getName() + "</discord-mention>"
				);
		}
		for (Role mentionedRole : message.getMentionedRoles()) {
			text =
				text.replace(
					mentionedRole.getAsMention(),
					"<discord-mention type=\"role\" color=\"" +
					toHex(mentionedRole.getColorRaw()) +
					"\">" +
					mentionedRole.getName() +
					"</discord-mention>"
				);
		}
		for (Emote emote : message.getEmotes()) {
			text = text.replace(emote.getAsMention(), "<img src=\"" + emote.getImageUrl() + "\" width=\"16\" height=\"16\"/>");
		}
		builder.text(parseMarkdown(text));

		builder.timestamp(toTimestamp(message.getTimeCreated()));

		List<LogEmbed> embeds = new ArrayList<>();
		for (MessageEmbed embed : message.getEmbeds()) {
			String description = fillNull(embed.getDescription());
			for (PartialEmote emote : getEmotes(description)) {
				description =
					description.replace(emote.getAsMention(), "<img src=\"" + emote.getImageUrl() + "\" width=\"16\" height=\"16\"/>");
			}
			LogEmbed.Builder embedBuilder = LogEmbed
				.builder()
				.title(fillNull(embed.getTitle()))
				.url(fillNull(embed.getUrl()))
				.description(parseHyperLink(parseMarkdown(description)))
				.fields(embed.getFields())
				.color(toHex(embed.getColorRaw()));

			if (embed.getFooter() != null) {
				embedBuilder.footer(fillNull(embed.getFooter().getText()));
			}
			if (embed.getThumbnail() != null) {
				embedBuilder.thumbnail(fillNull(embed.getThumbnail().getUrl()));
			}
			if (embed.getTimestamp() != null) {
				embedBuilder.timestamp(toTimestamp(embed.getTimestamp()));
			}
			embeds.add(embedBuilder.build());
		}
		builder.embeds(embeds);
		return builder.build();
	}

	public static List<PartialEmote> getEmotes(String content) {
		return processMentions(content, Message.MentionType.EMOTE, new ArrayList<>(), ApplyLog::matchEmote);
	}

	public static PartialEmote matchEmote(Matcher m) {
		long emoteId = MiscUtil.parseSnowflake(m.group(2));
		String name = m.group(1);
		boolean animated = m.group(0).startsWith("<a:");
		return new PartialEmote(emoteId, name, animated);
	}

	public static <T, C extends Collection<T>> C processMentions(
		String content,
		Message.MentionType type,
		C collection,
		Function<Matcher, T> map
	) {
		Matcher matcher = type.getPattern().matcher(content);
		while (matcher.find()) {
			try {
				T elem = map.apply(matcher);
				if (elem == null || collection.contains(elem)) {
					continue;
				}
				collection.add(elem);
			} catch (NumberFormatException ignored) {}
		}
		return collection;
	}

	public static String fillNull(String str) {
		return str == null ? "" : str;
	}

	public static String toHex(int color) {
		return "#" + Integer.toHexString(color);
	}

	public static String toTimestamp(OffsetDateTime timestamp) {
		return DateTimeFormatter.ofPattern("MM/dd/yy").format(timestamp);
	}

	public static String parseMarkdown(String str) {
		str = parseMarkdown(str, "\\*\\*", "b");
		str = parseMarkdown(str, "__", "u");
		str = parseMarkdown(str, "\\*", "i");
		str = parseMarkdown(str, "_", "i");
		str = parseMarkdown(str, "~~", "strike");
		str = parseMarkdown(str, "\\|\\|", "span class=\"spoiler\"", "span");
		str = parseMarkdown(str, "`", "tt class=\"code\"", "tt");
		return str;
	}

	public static String parseMarkdown(String str, String regex, String tag) {
		return parseMarkdown(str, regex, tag, tag);
	}

	public static String parseMarkdown(String str, String regex, String startTag, String endTag) {
		return Pattern
			.compile("(?<!\\\\)" + regex + "(.*)" + "(?<!\\\\)" + regex)
			.matcher(str)
			.replaceAll(match -> "<" + startTag + ">" + match.group(1) + "</" + endTag + ">");
	}

	public static String parseHyperLink(String str) {
		Pattern pattern = Pattern.compile("\\[(.*)]\\((.*)\\)");
		Matcher matcher = pattern.matcher(str);
		return matcher.replaceAll(match -> "<a href=\"" + match.group(2) + "\">" + match.group(1) + "</a>");
	}
}
