/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

import static com.skyblockplus.utils.utils.Utils.updateGuildExecutor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.entities.MemberImpl;
import org.apache.commons.collections4.SetUtils;

public final class ModifyMemberRecord {

	private final Set<Role> add = new HashSet<>();
	private final Set<Role> remove = new HashSet<>();
	private String nickname;

	public ModifyMemberRecord update(Member selfMember, List<Role> add, List<Role> remove) {
		if (selfMember.hasPermission(Permission.MANAGE_ROLES)) {
			for (Role role : add) {
				if (role != null && selfMember.canInteract(role)) {
					this.add.add(role);
				}
			}
			for (Role role : remove) {
				if (role != null && selfMember.canInteract(role)) {
					this.remove.add(role);
				}
			}
		}
		return this;
	}

	public ModifyMemberRecord update(Member selfMember, List<Role> add, List<Role> remove, String nickname) {
		update(selfMember, add, remove);
		this.nickname = nickname.trim();
		return this;
	}

	/**
	 * @return true if roles or nickname modification was queued
	 */
	public boolean queue(Member member) {
		boolean queued = false;

		if (!add.isEmpty() || !remove.isEmpty()) {
			if (!remove.isEmpty()) {
				// Theoretically duplicate roles should only be the same automatic guild member/ranks roles
				// If it's in both lists, it means the player is in one of the guilds, so then keep the role
				remove.removeAll(SetUtils.intersection(add, remove));
			}

			Set<Role> currentRoles = ((MemberImpl) member).getRoleSet();
			Set<Role> updatedRoles = new HashSet<>(currentRoles);
			updatedRoles.addAll(add);
			updatedRoles.removeAll(remove);

			if (!SetUtils.isEqualSet(currentRoles, updatedRoles)) {
				updateGuildExecutor.submit(() -> {
					try {
						member.getGuild().modifyMemberRoles(member, updatedRoles).complete();
					} catch (Exception ignored) {}
				});
				queued = true;
			}
		}

		if (nickname != null && !nickname.equals(member.getNickname())) {
			updateGuildExecutor.submit(() -> {
				try {
					member.modifyNickname(nickname).complete();
				} catch (Exception ignored) {}
			});
			queued = true;
		}

		return queued;
	}
}
