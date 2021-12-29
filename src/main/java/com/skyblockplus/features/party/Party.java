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

package com.skyblockplus.features.party;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Party {

    private final List<PartyMember> partyMembers = new ArrayList<>();
    private final List<String> missingClasses = new ArrayList<>();
    private String partyLeaderUsername;
    private String partyLeaderId;
    private String messageChannelId;
    private String floor;
    private List<String> requestedClasses;

    public Party(String partyLeaderUsername, String partyLeaderId, String floor, List<String> requestedClasses, String messageChannelId) {
        this.partyLeaderUsername = partyLeaderUsername;
        this.partyLeaderId = partyLeaderId;
        this.floor = floor;
        this.requestedClasses = requestedClasses;
        this.missingClasses.addAll(requestedClasses);
        this.messageChannelId = messageChannelId;
    }

    public void joinParty(String username, String discordId, String className, boolean isAny) {
        missingClasses.remove(isAny ? "any" : className);
        partyMembers.add(new PartyMember(username, discordId, className, isAny));
    }

    public boolean leaveParty(String discordId) {
        for (PartyMember partyMember : partyMembers) {
            if (partyMember.getDiscordId().equals(discordId)) {
                missingClasses.add(partyMember.isAny() ? "any" : partyMember.getClassName());
                partyMembers.remove(partyMember);
                return true;
            }
        }
        return false;
    }

    public String kickFromParty(String username) {
        for (PartyMember partyMember : partyMembers) {
            if (partyMember.getUsername().equalsIgnoreCase(username)) {
                missingClasses.add(partyMember.isAny() ? "any" : partyMember.getClassName());
                partyMembers.remove(partyMember);
                return partyMember.getUsername();
            }
        }
        return null;
    }

    public int getFloorInt() {
        if (floor.equals("entrance")) {
            return 0;
        } else if (floor.startsWith("master_floor_")) {
            return 7 + Integer.parseInt(floor.split("master_floor_")[1]);
        } else {
            return Integer.parseInt(floor.split("floor_")[1]);
        }
    }

    @Data
    @AllArgsConstructor
    public static class PartyMember {

        private String username;
        private String discordId;
        private String className;
        private boolean isAny;
    }
}
