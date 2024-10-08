package com.gigaspaces.datavalidator.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfluxDbProperties {
	@Value("${influxDBUrl}")
	private String influxDBUrl;
	@Value("${influxDBUsername}")
	private String influxDBUsername;
	@Value("${influxDBPassword}")
	private String influxDBPassword;
	@Value("${influxDBName}")
	private String influxDBName;
	@Value("${envName}")
	private String envName;
	@Value("${host}")
	private String host;

	public String getInfluxDBUrl() {
		return influxDBUrl;
	}

	public String getInfluxDBUsername() {
		return influxDBUsername;
	}

	public String getInfluxDBPassword() {
		return influxDBPassword;
	}

	public String getInfluxDBName() {
		return influxDBName;
	}

	public String getEnvName() {
		return envName;
	}

	public String getHost() {
		return host;
	}
}
