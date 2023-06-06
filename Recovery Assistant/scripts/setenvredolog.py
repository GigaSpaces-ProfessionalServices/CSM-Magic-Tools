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
gsHome = pathlib.PurePath(__file__).parent.parent.parent
raHome = pathlib.PurePath(__file__).parent.parent
scriptLocation = pathlib.PurePath(raHome).joinpath('scripts')
resourceLocation = pathlib.PurePath(raHome).joinpath('resources')
jarFilePath = pathlib.PurePath(resourceLocation).joinpath(jarFileName)
sourcePath = pathlib.PurePath(gsHome).joinpath('Work/redo-log', spaceName)
targetPath = pathlib.PurePath(gsHome).joinpath('backup/work/redo-log', spaceName)
targetPathBaseDir = pathlib.PurePath(gsHome).joinpath('backup')
deserializeFullPath = pathlib.PurePath(gsHome).joinpath('backup')

redoLogYaml = str(pathlib.PurePath(gsHome).joinpath('backup','AllDeserializedFiles'))
assemblyFileName = str(pathlib.PurePath(gsHome).joinpath('Deploy','DataProcessor','GigaSpaces.Examples.ProcessingUnit.Common.dll'))
