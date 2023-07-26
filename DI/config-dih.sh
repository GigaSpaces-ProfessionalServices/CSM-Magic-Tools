mdmUrl="gstest-di1.tau.ac.il:6081"  #<ip of mdm>:port
managerUrl="gstest-di1.tau.ac.il:6080"   #<ip of di-manager>:port
flinkUrl="http://gstest-di1.tau.ac.il:8081"   #http://<ip of flink>:port
diProcessorJar="/home/gsods/di-manager/latest-di-manager/lib/job-2.1.36.jar"   #/home/gsods/di-manager/latest-di-manager/lib/job-2.1.36.jar
bootstrapServers="gstest-di1.tau.ac.il:9092"   #kafka1:9092,kafka2:9092,kafka3 - to test if works with mutliple ips
kafkaGroupId="diprocessor"    #diprocessor
spaceLookupGroups="xap-16.3.0"
spaceLookupLocators="gstest-manager1.tau.ac.il" #,gstest-manager2.tau.ac.il,gstest-manager3.tau.ac.il"  #mng1,mng2,mng3 - to test if works with mutliple ips

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
read -p "exec 4 Configure IIDR extraction"

# 4 Configure IIDR extraction

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
read -p "exec 5 Get All Global Configuration"

# 5 Get All Global Configuration

curl -s --location "${mdmUrl}/api/v1/global-config/" \
--header 'Accept: */*'|jq

echo;echo
read -p "exec 6 Get About MDM"

# 6 Get About MDM

curl -s --location "${mdmUrl}/api/v1/about" \
--header 'Accept: */*'


echo;echo
read -p "exec 7 Get About Manager"

# 7 Get About Manager

curl -s --location "${managerUrl}/api/v1/about" \
--header 'Accept: */*'


echo;echo
read -p "exec 8 Update di-processor jar"

# 8 Update di-processor jar

curl -s --location "${mdmUrl}/api/v1/global-config/flink/di-processor-jar" \
--header 'Content-Type: text/plain' \
--data "${diProcessorJar}"


echo;echo
read -p "exec 9 Get di-processor jar"

# 9 Get di-processor jar

curl -s --location "${mdmUrl}/api/v1/global-config/flink/di-processor-jar"

echo
