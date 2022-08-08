/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gs;

import org.hibernate.SessionFactory;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.config.annotation.EmbeddedSpaceBeansConfig;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;

import javax.sql.DataSource;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageConfig;
import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.security.cert.TrustAnchor;
import java.time.Duration;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;


public class CustomSpaceConfig extends EmbeddedSpaceBeansConfig {

	@Value("${db.driver}")
	private String dbDriver;
	@Value("${db.url}")
	private String dbUrl;
	@Value("${db.user:#{null}}")
	private String dbUser;
	@Value("${db.password:#{null}}")
	private String dbPassword;
	@Value("${hibernate.dialect}")
	private String hibernateDialect;
	@Value("${hibernate.limitResults:-1}")
	private int limitResults;
	@Value("${tieredCriteriaConfig.filePath}")
	private String tieredConfigFilePath;

	private String isTieredConfigFileExist;

	public String getIsTieredConfigFileExist() {
		return isTieredConfigFileExist;
	}

	public void setIsTieredConfigFileExist(String isTieredConfigFileExist) {
		this.isTieredConfigFileExist = isTieredConfigFileExist;
	}

	@Override
    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
		super.configure(factoryBean);
	/*
	factoryBean.setSchema("persistent");
		factoryBean.setMirrored(true);
       	factoryBean.setSpaceDataSource(new DefaultHibernateSpaceDataSourceConfigurer()
		   	 .sessionFactory(initSessionFactory())
		    .clusterInfo(clusterInfo)
		    .limitResults(limitResults)
		    .create());

		Properties properties = new Properties();
        properties.setProperty("space-config.engine.cache_policy", "1");
        properties.setProperty("space-config.external-data-source.usage", "read-only");
        properties.setProperty("cluster-config.cache-loader.external-data-source", "true");
        properties.setProperty("cluster-config.cache-loader.central-data-source", "true");
        factoryBean.setProperties(properties);
		*/

		TieredStorageConfig tieredStorageConfig = new TieredStorageConfig();
		Map<String, TieredStorageTableConfig> tables = new HashMap<>();
		boolean isTieredConfigFileExist=checkIsFileExist(tieredConfigFilePath);
		System.out.println("isTieredConfigFileExist "+isTieredConfigFileExist);
		if(isTieredConfigFileExist){
			System.out.println(" File Exist ::::: Starting Tiered StorageIMPL");
			String lineJustFetched = null;
			String[] wordsArray;
			try {
				BufferedReader bufferedReader = new CustomSpaceConfig2().getFileBufferedReader(tieredConfigFilePath);
				while (true) {
					lineJustFetched = bufferedReader.readLine();
					if(lineJustFetched==null)
						break;
					else{
						wordsArray = lineJustFetched.split("	");
						if(wordsArray[0].equalsIgnoreCase("C")){
							System.out.println("Catagory :" + wordsArray[0] + " DataType :" + wordsArray[1] + " Property :" + wordsArray[2]);
							//tables.put("+className+".class.getName(), new TieredStorageTableConfig().setName("+className+".class.getName()).setCriteria(\""+criteria+"\")); \n";
							tables.put(wordsArray[1],new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria(wordsArray[2]));
						}
						if(wordsArray[0].equalsIgnoreCase("T")){
							System.out.println("Time :");
							System.out.println(wordsArray[1]+" :: "+wordsArray[2]+" : "+wordsArray[3]);
							//tables.put("+className+".class.getName(), new TieredStorageTableConfig().setName("+className+".class.getName()).setTimeColumn(\""+property+"\").setPeriod(Duration.ofDays("+criteria.replace("d","")+"))); \n";
							//Period period = Period.parse("P1D");
							//Duration duration = Duration.parse(Duration.);
							if(wordsArray[3].contains("D")) {
								long durationDays = Long.parseLong(wordsArray[3].replace("PT", "").replace("D", ""));
								tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(Duration.ofDays(durationDays)));
							}else if(wordsArray[3].contains("H")) {
								long duration = Long.parseLong(wordsArray[3].replace("PT", "").replace("H", ""));
								tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(Duration.ofHours(duration)));
							}else if(wordsArray[3].contains("S")) {
								long duration = Long.parseLong(wordsArray[3].replace("PT", "").replace("S", ""));
								tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(Duration.ofSeconds(duration)));
							}else if(wordsArray[3].contains("M")) {
								long duration = Long.parseLong(wordsArray[3].replace("PT", "").replace("M", ""));
								tables.put(wordsArray[1], new TieredStorageTableConfig().setName(wordsArray[1]).setTimeColumn(wordsArray[2]).setPeriod(Duration.ofMinutes(duration)));
							}

							//tables.put(Purchase.class.getName(), new TieredStorageTableConfig().setName(Purchase.class.getName()).setTimeColumn("orderTime").setPeriod(Duration.ofDays(durationDays)));
						}
						if(wordsArray[0].equalsIgnoreCase("A")){
							System.out.println(wordsArray[1]+"ALL ");
							tables.put(wordsArray[1],new TieredStorageTableConfig().setName(wordsArray[1]).setCriteria("all"));
						}
						if (wordsArray[0].equalsIgnoreCase("R")){
							System.out.println(wordsArray[1]+"Transient :: ");
							//+ "		tables.put(Data2.class.getName(), new TieredStorageTableConfig().setName(Data2.class.getName()).setTransient(true));\n"
							tables.put(wordsArray[1],new TieredStorageTableConfig().setName(wordsArray[1]).setTransient(true));
						}
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		System.out.println("MAP :"+tables);
		//TieredStorageImpl Ends...
		tieredStorageConfig.setTables(tables);
		factoryBean.setTieredStorageConfig(tieredStorageConfig);


		/*
		SpaceTypeDescriptor typeDescriptor = new SpaceTypeDescriptorBuilder("Data5")
				.idProperty("id")
				.addFixedProperty("name", String.class)
				.addFixedProperty("price", Float.class)
				.addFixedProperty("id", String.class).create();
		*/

	}

	private SessionFactory initSessionFactory() {
		return new LocalSessionFactoryBuilder(initDataSource())
		    .scanPackages("com.gs")
            .setProperty("hibernate.dialect", hibernateDialect)
            .setProperty("hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider")
            .setProperty("hibernate.jdbc.use_scrollable_resultset", "true")
		    .buildSessionFactory();
	}
	
	private DataSource initDataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbUrl);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		return dataSource;
	}

	private boolean checkIsFileExist(String tieredConfigFilePath) {
		BufferedReader bufferedReader=null;
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream in = classLoader.getResourceAsStream(tieredConfigFilePath);
			if (in != null) {
				return true;
			}
			if(in==null) {
				bufferedReader = new BufferedReader(new FileReader(tieredConfigFilePath));
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public BufferedReader getFileBufferedReader(String dataFile) throws Exception{
		BufferedReader bufferedReader=null;
    	try {
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream in = classLoader.getResourceAsStream(dataFile);
			if (in != null) {
				System.out.println("!=null");
				setIsTieredConfigFileExist("true");
				System.out.println("isExit:"+getIsTieredConfigFileExist());
				bufferedReader = new BufferedReader(new FileReader(classLoader.getResource(dataFile).getFile()));
			}
			if(in==null)
				bufferedReader = new BufferedReader(new FileReader(dataFile));
		}catch(Exception e){
			e.printStackTrace();
		}
		return bufferedReader;
	}
}
