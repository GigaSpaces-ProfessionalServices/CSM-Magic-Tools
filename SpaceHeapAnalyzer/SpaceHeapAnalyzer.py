#!/usr/bin/python3

import json
import os
import pandas as pd
import pyfiglet
import shutil
import subprocess
from datetime import datetime
from platform import system

if system() == 'Linux':
    CLRSCR = "clear"
if system() == 'Windows':
    CLRSCR = "cls"


def clearScreen():
    os.system(CLRSCR)
    osPrint(pyfiglet.figlet_format("    Space Heap              Analyer", font='slant'))

def listAllScripts(list_files_path):
    if os.path.exists(list_files_path):
        files = [file for file in os.listdir(list_files_path) if
                 os.path.isfile(os.path.join(list_files_path, file))]
    else:
        files = []
    return files

def createFolder(folderPath):
    if not os.path.exists(folderPath):
        os.makedirs(folderPath)

def osPrint(Statement):
    print(Statement)

def generateHeapHprof(_JmapPath,_PID,_SelectedSpaceName):
    osPrint("Generating Heap Hprof Report for Space Name = " + str(_SelectedSpaceName))
    jmapCmd = "jmap -dump:live,format=b,file="+_JmapPath+"/heap"+_PID+".hprof " + _PID
    subprocess.check_output(jmapCmd, shell=True, universal_newlines=True)
    osPrint("Heap Hprof reports generated successfully for Space Name = " + str(_SelectedSpaceName) + "\n")

def generateJsonFromHprof(_JmapPath,_JavaJarPath,_SpaceHeapAnalyzerJsonPath,_hprofPath,_SelectedSpaceName):
    osPrint("Generating Json from SpaceHeapAnalyzer Java for Space Name = "+ str(_SelectedSpaceName))
    SpaceHeapAnalyzerCmd = "java -jar " + _JavaJarPath + " " + _JmapPath + _hprofPath + " " + _SpaceHeapAnalyzerJsonPath + _hprofPath.split('.')[0] + ".json"
    os.system(SpaceHeapAnalyzerCmd)
    if (os.path.getsize(_SpaceHeapAnalyzerJsonPath + _hprofPath.split('.')[0] + ".json") == 0):
        os.remove(_SpaceHeapAnalyzerJsonPath + _hprofPath.split('.')[0] + ".json")
    osPrint("SpaceHeapAnalyzer Json generated successfully for Space Name = "+ str(_SelectedSpaceName) + "\n")

def generateReportsFromJson(_JmapPath,_SpaceHeapAnalyzerJsonPath,_ReportsPath,_DateTimeString):
    osPrint("Generating Reports from SpaceHeapAnalyzer Json")
    ListAllFiles = listAllScripts(_SpaceHeapAnalyzerJsonPath)
    SpaceBackupJsonList = []
    for i in range(len(ListAllFiles)):
        _filePath = _SpaceHeapAnalyzerJsonPath + "/" + ListAllFiles[i]
        if ".json" in str(ListAllFiles[i]):
            if os.path.getsize(_filePath) != 0:
                _data = json.load(open(_filePath))
                if "_" in str(_data["space"]["instanceId"]):
                    SpaceBackupJsonList.append(_filePath)

    for jsonfile in SpaceBackupJsonList:
        CombineReportJsonList = []
        _SpaceBackupJsonFileName = jsonfile.split('/')[-1].replace(".json","")
        data = json.load(open(jsonfile))

        for _generalFData in data["space"]["types"]:
            GeneralCombineReportJson =  {"InstanceID" : data["space"]["instanceId"] , "SpaceName" : data["space"]["spaceName"] , "TypeName" : _generalFData["typeName"],
                              "Property" : "", "Size" : _generalFData["averageEntrySize"], "Index Size" : "", "NumOfEntries" : _generalFData["numOfEntries"],
                              "NumOfProperties" : _generalFData["propertiesCount"], "TotalSize" : _generalFData["totalSize"],
                              "UidSizeCounter" : _generalFData["uidSizeCounter"], "MetadataSizeCountes" : _generalFData["metadataSizeCounter"],
                              "RepeatedRefs" : "", "Nulls" : ""}
            CombineReportJsonList.append(GeneralCombineReportJson)

        for type in data["space"]["types"]:
            for property in type["properties"]:
                PropertiesCombineReportJson =  {"InstanceID" : data["space"]["instanceId"] , "SpaceName" : data["space"]["spaceName"] , "TypeName" : type["typeName"] ,
                                                "Property" : property["propertyName"] , "Size" : property["size"] , "Index Size" : "" , "NumOfEntries" : "" , "NumOfProperties" : "" ,
                                                "TotalSize" : "" , "UidSizeCounter" : "" , "MetadataSizeCountes" : "" , "RepeatedRefs" : property["repeatedRefs"] ,
                                                "Nulls" : property["nulls"]}
                CombineReportJsonList.append(PropertiesCombineReportJson)

        for type in data["space"]["types"]:
            for Index in type["indexes"]:
                if len(list(data["space"]["types"][0]["indexes"])) > 0:
                    for CombineReportJson in CombineReportJsonList:
                        if CombineReportJson["InstanceID"] == data["space"]["instanceId"]:
                            if CombineReportJson["SpaceName"] == data["space"]["spaceName"]:
                                if CombineReportJson["TypeName"] == type["typeName"]:
                                    if CombineReportJson["Property"] == Index["name"]:
                                        CombineReportJson["Index Size"] = Index["size"]

        CombineReportDataFrame = pd.DataFrame(CombineReportJsonList)
        CombineReportDataFrame.to_csv(_ReportsPath+"Combine_Report_"+_SpaceBackupJsonFileName+"_"+data["space"]["spaceName"]+"_"+data["space"]["instanceId"]+"_"+_DateTimeString+".csv",index=False)


    osPrint("Reports generated successfully")

def removeUnwantedFiles(_JmapPath):
    ListJmapFolder = os.listdir(_JmapPath)
    for _path in ListJmapFolder:
        if os.path.exists(_JmapPath + _path) and ".hprof" in _path.lower():
            if os.path.isfile(_JmapPath + _path):
                os.remove(_JmapPath + _path)
            if os.path.isdir(_JmapPath + _path):
                shutil.rmtree(_JmapPath + _path)

if __name__ == '__main__':
    clearScreen()
    osPrint("Manager IP Example - 'http://192.1.12.32' or 'http://localhost'")
    ManagerIP = input("Enter Manager IP - ")
    ManagerUserName = input("Manager UserName - ")
    ManagerPassword = input("Manager Password - ")
    JmapPath = input("Enter Output folder - ")
    JavaJarPath = input("Enter SpaceHeapAnalyzer Java Jar Path - ")

    if JmapPath[-1] != "/":
        JmapPath = JmapPath + "/"
    SpaceHeapAnalyzerJsonPath = JmapPath + "SpaceHeapAnalyzerJson/"
    ReportsPath = JmapPath + "Report/"
    DateTimeString = datetime.now().strftime("%d-%m-%Y~%H.%M")

    listSpacesCmd = "curl -X GET --header 'Accept: application/json' '" + ManagerIP + ":8090/v2/spaces' -u " + ManagerUserName + ":" + ManagerPassword
    osPrint(listSpacesCmd)
    listSpacesResult = subprocess.check_output(listSpacesCmd, shell=True, universal_newlines=True)
    listSpacesResultList = json.loads(listSpacesResult)
    clearScreen()

    if (len(listSpacesResultList) == 0):
        osPrint("No Space found for Manager IP - " + ManagerIP)
        exit()

    SpaceNameDict = {}
    osPrint("Select Spaces")
    for _listSpacesResultList in range(len(listSpacesResultList)):
        SpaceNameDict[_listSpacesResultList] = listSpacesResultList[_listSpacesResultList]["name"]
        osPrint(str(_listSpacesResultList) + " - " + listSpacesResultList[_listSpacesResultList]["name"])

    SpaceName = input()
    SpaceName = SpaceNameDict[int(SpaceName)]

    FilterSpaceName = []
    for _listSpacesResultList in listSpacesResultList:
        if _listSpacesResultList["name"] == SpaceName:
            for _instancesIds in _listSpacesResultList["instancesIds"]:
                FilterSpaceName.append(_instancesIds)

    PIDList = []
    for _InstanceId in FilterSpaceName:
        _SpaceName = _InstanceId.split("~")[0]
        ListInstanceIdType = "curl -X GET --header 'Accept: application/json' '" + ManagerIP + ":8090/v2/spaces/" + _SpaceName + "/instances/" + _InstanceId + "' -u " + ManagerUserName + ":" + ManagerPassword
        ListInstanceIdTypeResult = subprocess.check_output(ListInstanceIdType, shell=True, universal_newlines=True)
        ListInstanceIdTypeResultList = json.loads(ListInstanceIdTypeResult)
        if ListInstanceIdTypeResultList["mode"] == "BACKUP":
            PIDList.append({_InstanceId :ListInstanceIdTypeResultList["containerId"].split("~")[1]})

    clearScreen()

    if (len(PIDList) == 0):
        osPrint("No Partitions available for Space Name - " + SpaceName)
        exit()

    tempPartitions = {}
    osPrint("For single partition selection just enter specific number")
    osPrint("For Multiple partitions range selection enter '0-2' this with select 0, 1 and 2")
    osPrint("For Multiple partitions selection enter '0,2,4' this with select 0, 2 and 4")
    osPrint("Note if you select the Select ALL menu number in the Multiple partitions selection so report will be generated for ALL partitions")
    osPrint("\n")
    osPrint("Select Partitions")
    for partitions in range(len(PIDList)):
        for key,value in (PIDList[partitions]).items():
            osPrint(str(partitions) + " - " + key)
            tempPartitions[partitions] = key

    osPrint(str(len(PIDList)) + " - " + "Select All")
    tempPartitions[len(PIDList)] = "Select All"

    PartitionsSelection = input()

    clearScreen()

    SelectedPIDList = []
    if (PartitionsSelection.isdigit()):
        if (int(PartitionsSelection) != len(PIDList)+1):
            for partitions in range(len(PIDList)):
                for key,value in (PIDList[partitions]).items():
                  if (str(tempPartitions[int(PartitionsSelection)]) == key):
                      SelectedPIDList.append(value)
        else:
            for partitions in range(len(PIDList)):
                for key,value in (PIDList[partitions]).items():
                    SelectedPIDList.append(value)
    elif isinstance(PartitionsSelection,str) and str(len(PIDList)) in str(PartitionsSelection):
            for partitions in range(len(PIDList)):
                for key,value in (PIDList[partitions]).items():
                    SelectedPIDList.append(value)
    elif isinstance(PartitionsSelection,str) and "-" in PartitionsSelection and "," in PartitionsSelection:
        osPrint("Wrong Selection quiting code.")
        exit()
    elif isinstance(PartitionsSelection,str) and "-" in PartitionsSelection:
        for selectedRange in range(int(str(PartitionsSelection).split("-")[0]),int(str(PartitionsSelection).split("-")[1])+1):
            for partitions in range(len(PIDList)):
                for key,value in (PIDList[partitions]).items():
                    if (str(tempPartitions[selectedRange]) == key):
                        SelectedPIDList.append(value)
    elif isinstance(PartitionsSelection,str) and "," in PartitionsSelection:
        _SelectedPIDList = PartitionsSelection.split(",")
        for _SelectedPID in _SelectedPIDList:
            for partitions in range(len(PIDList)):
                for key,value in (PIDList[partitions]).items():
                    if (str(tempPartitions[int(_SelectedPID)]) == key):
                        SelectedPIDList.append(value)
    else:
        osPrint("Wrong Selection quiting code.")
        exit()

    createFolder(JmapPath)
    createFolder(SpaceHeapAnalyzerJsonPath)
    createFolder(ReportsPath)

    for PID in list(SelectedPIDList):
        if len(SelectedPIDList) > 0:
            for partitions in range(len(PIDList)):
                for key,value in (PIDList[partitions]).items():
                    if (PID == value):
                        SelectedSpaceName = key
            generateHeapHprof(JmapPath, str(PID),SelectedSpaceName)
            generateJsonFromHprof(JmapPath, JavaJarPath, SpaceHeapAnalyzerJsonPath, "heap" + str(PID) + ".hprof", SelectedSpaceName)
            removeUnwantedFiles(JmapPath)

    generateReportsFromJson(JmapPath,SpaceHeapAnalyzerJsonPath,ReportsPath,DateTimeString)
