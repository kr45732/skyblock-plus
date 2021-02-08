package com.skyblockplus.api.discordserversettings;

import org.hibernate.annotations.Type;

import javax.persistence.*;

@Entity
@Table
public class SettingsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="id", updatable = false, nullable = false)
    private Long id;
    private String serverName;

    @Column
    @Type(type = "AutomatedApplicationType")
    private AutomatedApplication automatedApplication;

    public SettingsModel() {
    }

    public SettingsModel(Long id, String serverName, AutomatedApplication automatedApplication) {
        this.id = id;
        this.serverName = serverName;
        this.automatedApplication = automatedApplication;
    }

    public SettingsModel(String serverName, AutomatedApplication automatedApplication) {
        this.serverName = serverName;
        this.automatedApplication = automatedApplication;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public AutomatedApplication getAutomatedApplication() {
        return automatedApplication;
    }

    public void setAutomatedApplication(AutomatedApplication automatedApplication) {
        this.automatedApplication = automatedApplication;
    }
}
