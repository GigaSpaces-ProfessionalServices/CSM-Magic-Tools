mdmUrl="gsprod-di1.tau.ac.il:6081"
managerUrl="gsprod-di1.tau.ac.il:6080"
flinkUrl="http://gsprod-di1.tau.ac.il:8081"
diSsubscriptionManager="http://gsprod-iidr1.tau.ac.il:6082"
diProcessorJar="/home/gsods/di-processor/latest-di-processor/lib/job-2.3.9.jar"
bootstrapServers="gsprod-di1.tau.ac.il:9092,gsprod-di2.tau.ac.il:9092,gsprod-di3.tau.ac.il:9092"
kafkaGroupId="diprocessor"
spaceLookupGroups="xap-16.4.0"
spaceLookupLocators="gsprod-manager1.tau.ac.il"

echo;echo
read -p "exec 1 Configure Flink"

# 1 Configure Flink
mycurl=$( echo -e "curl -s --location \"${mdmUrl}/api/v1/global-config/flink\" --header 'Content-Type: application/json' --data '{ \"restEndpoint\": \"${flinkUrl}\", \"diProcessorJar\": \"${diProcessorJar}\" }' |jq" )
eval $mycurl
echo;echo
read -p "exec 2 Configure Kafka"

# 2 Configure Kafka

mycurl=$( echo -e "curl -s --location '${mdmUrl}/api/v1/global-config/kafka' --header 'Content-Type: application/json' --data '{\"bootstrapServers\":[\"${bootstrapServers}\"],\"groupId\":\"${kafkaGroupId}\"}'")
eval $mycurl |jq

echo;echo
read -p "exec 3 Configure Space Common"

# 3 Configure Space Common

mycurl=$(echo -e "curl -s --location '${mdmUrl}/api/v1/global-config/space' --header 'Content-Type: application/json' --data '{\"lookupGroups\": \"${spaceLookupGroups}\",\"lookupLocators\":\"${spaceLookupLocators}\"}'")
eval $mycurl |jq

echo;echo
read -p "exec 4 Configure DI Subscription Manager"

# 4 Configure DI Subscription Mananger

curl -s -X 'POST' \
  'http://'$mdmUrl'/api/v1/global-config/subscription-managers' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "iidr": {
        "url": "'$diSsubscriptionManager'",
        "features": {
            "supportsTransaction": true
        }
    }
}' |jq

echo;echo
read -p "exec 5 Configure IIDR extraction"


# 5 Configure IIDR extraction

mycurl=$(echo -e "curl -s --location '${mdmUrl}/api/v1/global-config/iidr-extraction' --header 'Content-Type: application/json' --data '{\"cdcOperations\": {
      \"insert\": {
        \"conditions\": null,
        \"defaultOperation\": true
},
      \"update\": {
        \"conditions\": [
          {
            \"value\": \"UP\",
            \"selector\": \"$.A_ENTTYP.string\"
          }
        ],
        \"defaultOperation\": false
      },
      \"delete\": {
        \"conditions\": [
          {
            \"value\": \"DL\",
            \"selector\": \"$.A_ENTTYP.string\"
          }
        ],
        \"defaultOperation\": false
      }
    },
    \"tableNameExtractionJsonPath\": \"$.A_OBJECT.string\",
    \"schemaNameExtractionJsonPath\": \"$.A_LIBRARY.string\",
    \"dataFormat\": \"JSON\"
  }'")

eval $mycurl |jq

echo;echo
read -p "exec 6 Get All Global Configuration"

# 6 Get All Global Configuration

curl -s --location "${mdmUrl}/api/v1/global-config/" \
--header 'Accept: */*'|jq

echo;echo
read -p "exec 7 Get About MDM"

# 7 Get About MDM

curl -s --location "${mdmUrl}/api/v1/about" \
--header 'Accept: */*'


echo;echo
read -p "exec 8 Get About Manager"

# 8 Get About Manager

curl -s --location "${managerUrl}/api/v1/about" \
--header 'Accept: */*'


echo;echo
read -p "exec 9 Update di-processor jar"

# 9 Update di-processor jar

curl -s --location "${mdmUrl}/api/v1/global-config/flink/di-processor-jar" \
--header 'Content-Type: text/plain' \
--data "${diProcessorJar}"


echo;echo
read -p "exec 10 Get di-processor jar"

# 9 Get di-processor jar

curl -s --location "${mdmUrl}/api/v1/global-config/flink/di-processor-jar"

echo

