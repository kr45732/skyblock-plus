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

package com.skyblockplus.utils.structs;

import com.skyblockplus.utils.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HypixelGuildCache {

    private final List<String> normalCache;
    private final List<String> ironmanCache;
    private Instant lastUpdated;

    public HypixelGuildCache() {
        this.normalCache = new ArrayList<>();
        this.ironmanCache = new ArrayList<>();
        this.lastUpdated = Instant.now();
    }

    public static boolean isValidType(String type) {
        return typeToIndex(type.toLowerCase()) >= 2;
    }

    public static int typeToIndex(String type) {
        return switch (type) {
            case "username" -> 0;
            case "uuid" -> 1;
            case "slayer" -> 2;
            case "skills" -> 3;
            case "catacombs" -> 4;
            case "weight" -> 5;
            case "sven" -> 6;
            case "rev" -> 7;
            case "tara" -> 8;
            case "enderman" -> 9;
            case "alchemy" -> 10;
            case "combat" -> 11;
            case "fishing" -> 12;
            case "farming" -> 13;
            case "foraging" -> 14;
            case "carpentry" -> 15;
            case "mining" -> 16;
            case "taming" -> 17;
            case "enchanting" -> 18;
            default -> -1;
        };
    }

    public static String getStringFromCache(String cache, String type) {
        return cache.split("=:=")[typeToIndex(type)];
    }

    public static double getDoubleFromCache(String cache, String type) {
        return Double.parseDouble(getStringFromCache(cache, type));
    }

    public void addPlayer(Player player) {
        normalCache.add(memberCacheFromPlayer(player, false));
        ironmanCache.add(memberCacheFromPlayer(player, true));
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public HypixelGuildCache setLastUpdated() {
        lastUpdated = Instant.now();
        return this;
    }

    public List<String> getCache() {
        return getCache(false);
    }

    public List<String> getCache(boolean ironmanOnly) {
        return ironmanOnly ? ironmanCache : normalCache;
    }

    private String memberCacheFromPlayer(Player player, boolean ironmanOnly) {
        return (
                player.getUsername() +
                        "=:=" +
                        player.getUuid() +
                        "=:=" +
                        player.getHighestAmount("slayer", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("skills", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("catacombs", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("weight", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("sven", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("rev", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("tara", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("enderman", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("alchemy", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("combat", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("fishing", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("farming", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("foraging", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("carpentry", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("mining", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("taming", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("enchanting", ironmanOnly) +
                        "=:=" +
                        player.getHighestAmount("enchanting", ironmanOnly)
        );
    }
}
