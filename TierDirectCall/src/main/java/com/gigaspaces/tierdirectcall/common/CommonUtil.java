package com.gigaspaces.tierdirectcall.common;

import com.gigaspaces.internal.server.space.tiered_storage.TieredStorageTableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CommonUtil {
    private static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static Map<String, TieredStorageTableConfig> setTableConfigCriteria(String tierCriteriaFilename) {
        Map<String, String[]> typesWithCriteriaArrayMap = getTierCriteriaConfig(tierCriteriaFilename);
        Map<String, TieredStorageTableConfig> tieredStorageTableConfigMap = new HashMap<>();
        for (String typeName : typesWithCriteriaArrayMap.keySet()) {
            String[] criteriaArray = typesWithCriteriaArrayMap.get(typeName);
            TieredStorageTableConfig tableConfig = new TieredStorageTableConfig();
            if (criteriaArray != null) {
                if (criteriaArray[0].equalsIgnoreCase("C")) {
                    logger.info("Catagory :" + criteriaArray[0] + " DataType :" + criteriaArray[1] + " Property :" + criteriaArray[2]);
                    tableConfig.setName(criteriaArray[1])
                            .setCriteria(criteriaArray[2]);
                }
                if (criteriaArray[0].equalsIgnoreCase("T")) {
                    logger.info("Time Criteria:  " + criteriaArray[1] + " :: " + criteriaArray[2] + " : " + criteriaArray[3]);
                    Duration duration = Duration.parse(criteriaArray[3]);
                    tableConfig.setName(criteriaArray[1])
                            .setTimeColumn(criteriaArray[2]).setPeriod(duration);
                }
                if (criteriaArray[0].equalsIgnoreCase("A")) {
                    logger.info(criteriaArray[1] + " ALL ");
                    tableConfig.setName(criteriaArray[1])
                            .setCriteria("all");
                }
                if (criteriaArray[0].equalsIgnoreCase("R")) {
                    logger.info(criteriaArray[1] + "Transient :: ");
                    tableConfig.setName(criteriaArray[1])
                            .setTransient(true);
                }
            }
            tieredStorageTableConfigMap.put(typeName, tableConfig);
        }
        return tieredStorageTableConfigMap;
    }

    public static Map<String, String[]> getTierCriteriaConfig(String strTierCriteriaFile) {
        logger.info("Entering into -> getTierCriteriaConfig");
        logger.info("strTierCriteriaFile -> " + strTierCriteriaFile);
        Map<String, String[]> typesWithCriteria = new HashMap<>();
        try {
            if (strTierCriteriaFile != null && !"".equals(strTierCriteriaFile.trim())) {
                File tierCriteriaFile = new File(strTierCriteriaFile);
                if (tierCriteriaFile.exists()) {
                    logger.info("Tier Criteria config file exists");
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(tierCriteriaFile));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        logger.info("Tier criteria file line -> " + line);
                        String[] lineContents = line.split("\t");
                        if (lineContents != null && lineContents.length > 2) {
                            String typeName = lineContents[1];
                            logger.info("typeName -> " + typeName);
                            String criteria = lineContents[2];
                            logger.info("criteria -> " + criteria);

                            if (typeName != null) {
                                if (criteria != null && !"".equals(criteria.trim())) {
                                    typesWithCriteria.put(typeName, lineContents);
                                }
                            }
                        } else if (lineContents != null && lineContents.length == 2 && lineContents[0].equals("A")) {
                            String typeName = lineContents[1];
                            typesWithCriteria.put(typeName, lineContents);
                        }
                    }
                } else {
                    logger.info("Tier Criteria file '" + strTierCriteriaFile + "' does not exists");
                }
            } else {
                logger.info("Tier Criteria file path is not configured");
            }
            logger.info("Exiting from -> getTierCriteriaConfig");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getTierCriteriaConfig -> " + e, e);
        }
        return typesWithCriteria;
    }
}
