#!/bin/bash
./createNewPipeline.sh PL1 dih-tau-space STUD KR_KVUTZA,KR_KURS,TA_KTOVET,TL_HEARA
./createNewPipeline.sh PL2 dih-tau-space STUD MM_TOCHNIT_LIMUD,MM_YEHIDA,MJ_HODAOT_WEB,TM_SEGEL,TM_MECHKAR
./createNewPipeline.sh PL3 dih-tau-space STUD TL_TOCHNIT
./createNewPipeline.sh PL4 dih-tau-space STUD TA_IDS,TA_PERSON,TA_PRATIM,TL_SEM
#./createNewPipeline.sh PL5 dih-tau-space STUD TL_KURS-moved to obfuscation
./createNewPipeline.sh PL6 dih-tau-space STUD TA_HODAA
./createNewPipeline.sh PL7 dih-tau-space STUD SL_HESHBON
#./createNewPipeline.sh PL8 dih-tau-space STUD SL_TNUA-moved to obfuscation
./createNewPipeline.sh PL9 dih-tau-space STUD TL_MOED_TZIUN
./createNewPipeline.sh PL10 dih-tau-space STUD TL_KVUTZA
./createNewPipeline.sh PL11 dih-tau-space STUD KR_KVUTZA_MOED_HEARA,TL_MOED_NOSAF_BAKASHA,TL_CHOVOT_UNI,TL_IRUR_BAKASHA
./create_obfuscation_PL.sh
