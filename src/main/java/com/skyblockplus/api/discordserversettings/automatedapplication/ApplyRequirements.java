package com.skyblockplus.api.discordserversettings.automatedapplication;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@Embeddable
public class ApplyRequirements {
    private String slayerReq = "";
    private String skillsReq = "";
    private String catacombsReq = "";
    private String weightReq = "";

    public ApplyRequirements() {
    }
}
