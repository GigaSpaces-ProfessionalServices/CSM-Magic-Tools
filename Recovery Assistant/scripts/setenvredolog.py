#!/usr/bin/python3
# *-* coding: utf-8 *-*

import pathlib

spaceName = "dataExampleSpace"
lookupLocators = "EC2AMAZ-PUUQMQH"
lookupGroups = "xap-16.2.1"
spaceHostsFileName = "spaceHosts.txt"
redoLogScriptName = "copyRedoLogScript.bat"
deserializeScriptName = "deserializeScript.py"
jarFileName = 'redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar'

# setting paths accordding to this script location
raHome = pathlib.PurePath(__file__).parent.parent
gsHome = pathlib.PurePath(raHome).parent
scriptLocation = raHome.joinpath('scripts')
resourceLocation = raHome.joinpath('resources')
resourceAdminShare = str(resourceLocation.drive).replace(':','$')
resourceNetPath = f"{resourceAdminShare}{str(resourceLocation).replace(resourceLocation.drive , '')}"
jarFilePath = resourceLocation.joinpath(jarFileName)
sourcePath = gsHome.joinpath('Work/redo-log', spaceName)
targetPath = gsHome.joinpath('backup/work/redo-log', spaceName)
targetPathBaseDir = gsHome.joinpath('backup')
deserializeFullPath = gsHome.joinpath('backup')

redoLogYaml = str(gsHome.joinpath('backup','AllDeserializedFiles'))
assemblyFileName = str(gsHome.joinpath('Deploy','DataProcessor','GigaSpaces.Examples.ProcessingUnit.Common.dll'))
