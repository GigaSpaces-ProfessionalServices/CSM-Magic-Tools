package com.gigaspaces.Leumi.dto;

public class MSSQLConfig {

    public MSSQLConfig(String server, Boolean integratedSecurity, String authenticationScheme, String databaseName) {
        this.server = server;
        this.integratedSecurity = integratedSecurity;
        this.authenticationScheme = authenticationScheme;
        this.dataBaseName = databaseName;
    }

    private String server;
    private Boolean integratedSecurity;
    private String authenticationScheme;
    private String dataBaseName;

    public String getServer() { return server; }

    public Boolean getIntegratedSecurity() {
        return integratedSecurity;
    }

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public String getDatabaseName() {
        return dataBaseName;
    }
  }
