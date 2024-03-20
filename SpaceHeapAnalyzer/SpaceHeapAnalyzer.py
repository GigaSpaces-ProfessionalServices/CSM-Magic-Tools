#!/usr/bin/python3

import json
import os
import pandas as pd
import pyfiglet
import shutil
import subprocess
import time
from datetime import datetime
from platform import system
from configobj import ConfigObj
from statistics import mean
import re

if system() == 'Linux':
    CLRSCR = "clear"
if system() == 'Windows':
    CLRSCR = "cls"
if system() == 'Darwin':
    CLRSCR = "clear"


def clearScreen():
    os.system(CLRSCR)
    osPrint(pyfiglet.figlet_format("    Space Heap             Analyzer", font='slant'))

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

def readValueByConfigObj(key):
    sourceInstallerDirectory = str(os.getenv("ENV_CONFIG"))
    file=sourceInstallerDirectory+'/app.config'
    config = ConfigObj(file)
    return  config.get(key)

def generateHeapHprof(_JmapPath,_PID,_SelectedSpaceName):
    osPrint("Generating Heap Hprof Report for Space Name = " + str(_SelectedSpaceName))
    jmapCmd = "jmap -dump:live,format=b,file="+_JmapPath+"/heap"+_PID+".hprof " + _PID
    subprocess.check_output(jmapCmd, shell=True, universal_newlines=True)
    osPrint("Heap Hprof reports were generated successfully for Space Name = " + str(_SelectedSpaceName) + "\n")

def generateJsonFromHprof(_JmapPath,_JavaJarPath,_SpaceHeapAnalyzerJsonPath,_hprofPath,_SelectedSpaceName):
    osPrint("Generating Json from SpaceHeapAnalyzer Java for Space Name = "+ str(_SelectedSpaceName))
    SpaceHeapAnalyzerCmd = "java -jar " + _JavaJarPath + " " + _JmapPath + _hprofPath + " " + _SpaceHeapAnalyzerJsonPath + _hprofPath.split('.')[0] + ".json"
    os.system(SpaceHeapAnalyzerCmd)
    if os.path.exists(_SpaceHeapAnalyzerJsonPath + _hprofPath.split('.')[0] + ".json") == False:
        osPrint("Unable to Generate Heap Hprof")
        exit()
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

    CombineReportJsonList = []

    for jsonfile in SpaceBackupJsonList:
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
                                                "Property" : property["propertyName"] , "Size" : property["size"] , "Index Size" : "" , "NumOfEntries" : "" ,
                                                "NumOfProperties" : "" , "TotalSize" : "" , "UidSizeCounter" : "" , "MetadataSizeCountes" : "" , "RepeatedRefs" : property["repeatedRefs"] ,
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

    writer = pd.ExcelWriter(_ReportsPath+"Combine_Report_"+_SpaceBackupJsonFileName+"_"+data["space"]["spaceName"]+"_"+_DateTimeString+".xlsx", engine='openpyxl')

    CombineReportDataFrame = pd.DataFrame(CombineReportJsonList)
    CombineReportDataFrame = CombineReportDataFrame.sort_values(['InstanceID','TypeName', 'Property'],ascending = [True, True, True])
    CombineReportDataFrame.to_excel(writer,index=False,sheet_name="Summary report")

    CombineReportDataFrameList = CombineReportDataFrame['InstanceID'].tolist()
    CombineReportDataFrameList = list(set(CombineReportDataFrameList))

    AverageList = []

    for backupInstance in CombineReportDataFrameList:
        _backupInstance = CombineReportDataFrame[CombineReportDataFrame['InstanceID'] == backupInstance]
        _backupInstance = _backupInstance.sort_values(['TypeName', 'Property'],ascending = [True, True])
        _backupInstance.to_excel(writer,index=False,sheet_name=str(backupInstance))

        _backupInstanceList = _backupInstance.to_dict('records')
        if len(AverageList) == 0:
            for i in _backupInstanceList:
                AverageJson = {}
                AverageJson["SpaceName"] = i["SpaceName"]
                AverageJson["TypeName"] = i["TypeName"]
                AverageJson["Property"] = i["Property"]
                AverageJson["Size"] = [] if i["Size"] == "" else [i["Size"]]
                AverageJson["Index Size"] = [] if i["Index Size"] == "" else [i["Index Size"]]
                AverageJson["NumOfEntries"] = [] if i["NumOfEntries"] == "" else [i["NumOfEntries"]]
                AverageJson["NumOfProperties"] = [] if i["NumOfProperties"] == "" else [i["NumOfProperties"]]
                AverageJson["TotalSize"] = [] if i["TotalSize"] == "" else [i["TotalSize"]]
                AverageJson["UidSizeCounter"] = [] if i["UidSizeCounter"] == "" else [i["UidSizeCounter"]]
                AverageJson["MetadataSizeCountes"] = [] if i["MetadataSizeCountes"] == "" else [i["MetadataSizeCountes"]]
                AverageJson["RepeatedRefs"] = [] if i["RepeatedRefs"] == "" else [i["RepeatedRefs"]]
                AverageJson["Nulls"] = [] if i["Nulls"] == "" else [i["Nulls"]]
                AverageList.append(AverageJson)
        else:
            for i in _backupInstanceList:
                for _average in AverageList:
                    if i["SpaceName"] == _average["SpaceName"] and i["TypeName"] == _average["TypeName"] and i["Property"] == _average["Property"]:
                        _averageSizeList = _average["Size"]
                        _averageSizeList.append(i["Size"])
                        _average["Size"] = _averageSizeList

                        _IndexSizeList = _average["Index Size"]
                        True if i["Index Size"] == "" else _IndexSizeList.append(i["Index Size"])
                        _average["Index Size"] = _IndexSizeList

                        _NumOfEntriesList = _average["NumOfEntries"]
                        True if i["NumOfEntries"] == "" else _NumOfEntriesList.append(i["NumOfEntries"])
                        _average["NumOfEntries"] = _NumOfEntriesList

                        _NumOfPropertiesList = _average["NumOfProperties"]
                        True if i["NumOfProperties"] == "" else _NumOfPropertiesList.append(i["NumOfProperties"])
                        _average["NumOfProperties"] = _NumOfPropertiesList

                        _TotalSizeList = _average["TotalSize"]
                        True if i["TotalSize"] == "" else _TotalSizeList.append(i["TotalSize"])
                        _average["TotalSize"] = _TotalSizeList

                        _UidSizeCounterList = _average["UidSizeCounter"]
                        True if i["UidSizeCounter"] == "" else _UidSizeCounterList.append(i["UidSizeCounter"])
                        _average["UidSizeCounter"] = _UidSizeCounterList

                        _MetadataSizeCountesList = _average["MetadataSizeCountes"]
                        True if i["MetadataSizeCountes"] == "" else _MetadataSizeCountesList.append(i["MetadataSizeCountes"])
                        _average["MetadataSizeCountes"] = _MetadataSizeCountesList

                        _RepeatedRefsList = _average["RepeatedRefs"]
                        True if i["RepeatedRefs"] == "" else _RepeatedRefsList.append(i["RepeatedRefs"])
                        _average["RepeatedRefs"] = _RepeatedRefsList

                        _NullsList = _average["Nulls"]
                        True if i["Nulls"] == "" else _NullsList.append(i["Nulls"])
                        _average["Nulls"] = _NullsList

    for _average in AverageList:
        _average["Size"] = None if len(_average["Size"]) == 0 else round(mean(_average["Size"]),2)
        _average["Index Size"] = None if len(_average["Index Size"]) == 0 else round(mean(_average["Index Size"]),2)
        _average["NumOfEntries"] = None if len(_average["NumOfEntries"]) == 0 else sum(_average["NumOfEntries"])
        _average["NumOfProperties"] = None if len(_average["NumOfProperties"]) == 0 else max(_average["NumOfProperties"])
        _average["TotalSize"] = None if len(_average["TotalSize"]) == 0 else sum(_average["TotalSize"])
        _average["UidSizeCounter"] = None if len(_average["UidSizeCounter"]) == 0 else sum(_average["UidSizeCounter"])
        _average["MetadataSizeCountes"] = None if len(_average["MetadataSizeCountes"]) == 0 else sum(_average["MetadataSizeCountes"])
        _average["RepeatedRefs"] = None if len(_average["RepeatedRefs"]) == 0 else sum(_average["RepeatedRefs"])
        _average["Nulls"] = None if len(_average["Nulls"]) == 0 else sum(_average["Nulls"])

    AverageListDataFrame = pd.DataFrame(AverageList)
    AverageListDataFrame = AverageListDataFrame.sort_values(['TypeName', 'Property'],ascending = [True, True])

    AverageListDataFrame.to_excel(writer,index=False,sheet_name="Average report")

    writer.close()
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
    def module_exist(_module_name):
        r = subprocess.run(
            "pip3 list".split(),
            stdout=subprocess.PIPE).stdout.decode().lower()
        if re.search(_module_name.lower(), r):
            return True
        return False

    modules = ['configobj','pandas','pyfiglet']
    for module in modules:
        if not module_exist(module):
            subprocess.run([f'pip3 install {module}'], shell=True)

    clearScreen()

    AppConfigValues = input("Use Values from app.config [(Yes) Y/ (No) n] [Y] -  ")
    if AppConfigValues == "":
        AppConfigValues = "Y"

    if AppConfigValues.lower() == "y":
        sourceInstallerDirectory = str(os.getenv("ENV_CONFIG"))
        file=sourceInstallerDirectory+'/app.config'
        if os.path.exists(file) == False:
            osPrint("app.config unavailable at " + sourceInstallerDirectory + " path")
            exit()
        ManagerIP = readValueByConfigObj("app.spaceheapanalyzer.managerip")
        ManagerUserName = readValueByConfigObj("app.spaceheapanalyzer.managerusername")
        ManagerPassword = readValueByConfigObj("app.spaceheapanalyzer.managerpassword")
        JmapPath = readValueByConfigObj("app.spaceheapanalyzer.jmappath")
        JavaJarPath = readValueByConfigObj("app.spaceheapanalyzer.javajarpath")
    else:
        osPrint("Manager IP Example - '192.1.12.32' or 'localhost'")
        ManagerIP = input("Enter Manager IP - ")
        ManagerUserName = input("Manager UserName (leave empty for unsecured) - ")
        ManagerPassword = input("Manager Password (leave empty for unsecured) - ")
        JmapPath = input("Enter Output folder - ")
        JavaJarPath = input("Enter SpaceHeapAnalyzer Java Jar Path - ")

    if "http://" not in ManagerIP:
        ManagerIP = "http://" + ManagerIP

    if os.path.exists(JavaJarPath) == False:
        osPrint(os.getcwd()+"/target/SpaceHeapAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar")
        osPrint("Java Jar Not found Path")
        exit()

    if JmapPath[-1] != "/":
        JmapPath = JmapPath + "/"

    SpaceHeapAnalyzerJsonPath = JmapPath + "SpaceHeapAnalyzerJson/"
    ReportsPath = JmapPath + "Report/"
    DateTimeString = datetime.now().strftime("%d-%m-%Y~%H.%M")

    if (ManagerUserName == "" and ManagerPassword == ""):
        listSpacesCmd = "curl -X GET --header 'Accept: application/json' '" + ManagerIP + ":8090/v2/spaces'"
    else:
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
        if (ManagerUserName == "" and ManagerPassword == ""):
            ListInstanceIdType = "curl -X GET --header 'Accept: application/json' '" + ManagerIP + ":8090/v2/spaces/" + _SpaceName + "/instances/" + _InstanceId + "'"
        else:
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
        if (int(PartitionsSelection) != len(PIDList)):
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

    start_time = time.time()
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

    print("--- %s seconds ---" % (time.time() - start_time))
