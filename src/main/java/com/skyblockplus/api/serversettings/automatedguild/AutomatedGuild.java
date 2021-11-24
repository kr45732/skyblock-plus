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

package com.skyblockplus.api.serversettings.automatedguild;

import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedGuild {

    private String guildName;
    private String guildId;

    // Apply
    private String applyEnable = "false";
    private String applyMessageChannel = ""; // Message with button will be sent here
    private String applyStaffChannel = ""; // Applications to be reviewed by staff sent here
    private String applyCategory = ""; // Where new applications are created
    private String applyWaitingChannel = ""; // Applications that are waitlisted by staff sent here
    private String applyIronmanOnly = "false";
    @Column(length = 2048)
    private String applyMessage = "";
    @Column(length = 2048)
    private String applyAcceptMessage = "";
    @Column(length = 2048)
    private String applyDenyMessage = "";
    @Column(length = 2048)
    private String applyWaitlistMessage = "";
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<String> applyStaffRoles = new ArrayList<>();
    private String applyPrevMessage = ""; // Used to edit original message rather than sending new when the bot restarts
    @Column(columnDefinition = "TEXT")
    private String applyUsersCache = "";
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ApplyRequirements> applyReqs = new ArrayList<>();

    // Guild Member Roles
    private String guildMemberRoleEnable = "false";
    private String guildMemberRole = "";

    // Guild Rank
    private String guildRanksEnable = "false";
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<RoleObject> guildRanks = new ArrayList<>();

    private String guildCounterEnable = "false";
    private String guildCounterChannel = "";

    public AutomatedGuild() {
    }

    public AutomatedGuild(String guildName, String guildId) {
        this.guildName = guildName;
        this.guildId = guildId;
    }
}
