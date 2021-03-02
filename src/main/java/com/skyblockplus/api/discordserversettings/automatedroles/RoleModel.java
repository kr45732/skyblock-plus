package com.skyblockplus.api.discordserversettings.automatedroles;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Embeddable
public class RoleModel {

    private String enable = "false";

    @ElementCollection
    private List<RoleObject> levels = new ArrayList<>();

    private String stackable = "false";

    public RoleModel() {
    }
}
