#!/bin/bash

# gcom automated script

# service: get_user_messages
/giga/deployment/gcom full -s /giga/deployment/services/get_user_messages -j /giga/deployment/services/get_user_messages/Get_User_Messages-1.20-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: program_study
/giga/deployment/gcom full -s /giga/deployment/services/program_study -j /giga/deployment/services/program_study/Program_Study-1.11-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_stats_by_sem
/giga/deployment/gcom full -s /giga/deployment/services/get_stats_by_sem -j /giga/deployment/services/get_stats_by_sem/Get_StatsBySem-1.2-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_sem_dates 
/giga/deployment/gcom full -s /giga/deployment/services/get_sem_dates -j /giga/deployment/services/get_sem_dates/Get_TASemDates-1.4-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: person_tziun_kurs
/giga/deployment/gcom full -s /giga/deployment/services/person_tziun_kurs -j /giga/deployment/services/person_tziun_kurs/Person_Tziun_Kurs-1.30-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_tuition_years
/giga/deployment/gcom full -s /giga/deployment/services/get_tuition_years -j /giga/deployment/services/get_tuition_years/Get_TuitionYears-1.0-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_lecturer_hours
/giga/deployment/gcom full -s /giga/deployment/services/get_lecturer_hours -j /giga/deployment/services/get_lecturer_hours/Get_LecturerHours-1.2-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: person_details
/giga/deployment/gcom full -s /giga/deployment/services/person_details -j /giga/deployment/services/person_details/Person_Details-1.13-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: person_schedule - 2024-01-16 10:32 Ami requested changed from 1.19 to 1.16
/giga/deployment/gcom full -s /giga/deployment/services/person_schedule -j /giga/deployment/services/person_schedule/Person_Schedule-1.29-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_internal_id
/giga/deployment/gcom full -s /giga/deployment/services/get_internal_id -j /giga/deployment/services/get_internal_id/GetInternalId-1.1-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_kurs_kvutza
/giga/deployment/gcom full -s /giga/deployment/services/get_kurs_kvutza -j /giga/deployment/services/get_kurs_kvutza/Get_Kurs_Kvutza-1.2-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_tochnit_sems
/giga/deployment/gcom full -s /giga/deployment/services/get_tochnit_sems -j /giga/deployment/services/get_tochnit_sems/Get_Tochnit_Sems-1.3-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_avoda_and_manchim
/giga/deployment/gcom full -s /giga/deployment/services/get_avoda_and_manchim -j /giga/deployment/services/get_avoda_and_manchim/Get_Avoda_And_Manchim-1.2-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_appeal_grade
/giga/deployment/gcom full -s /giga/deployment/services/get_appeal_grade -j /giga/deployment/services/get_appeal_grade/GetAppealGrade-1.0-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_list_code_desc
/giga/deployment/gcom full -s /giga/deployment/services/get_list_code_desc -j /giga/deployment/services/get_list_code_desc/Get_List_Code_Desc-1.2-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

#  service: get_grade_deviation
/giga/deployment/gcom full -s /giga/deployment/services/get_grade_deviation -j /giga/deployment/services/get_grade_deviation/Get_Grades_Deviation-1.1-SNAPSHOT-jar-with-dependencies.jar  -c 3 -m 1g -i 3 -p 8113-8311

#  service: get_reasons_additional_exam
/giga/deployment/gcom full -s /giga/deployment/services/get_reasons_additional_exam -j /giga/deployment/services/get_reasons_additional_exam/Get_ReasonsAdditionalExam-1.0-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_additional_exam_request
/giga/deployment/gcom full -s /giga/deployment/services/get_additional_exam_request -j /giga/deployment/services/get_additional_exam_request/Get_AdditionalExamRequest-1.0-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_weekly_schedule
/giga/deployment/gcom full -s /giga/deployment/services/get_weekly_schedule -j /giga/deployment/services/get_weekly_schedule/Get_WeeklySchedule-1.0-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_edu_adjust
/giga/deployment/gcom full -s /giga/deployment/services/get_edu_adjust -j /giga/deployment/services/get_edu_adjust/Get_EduAdjust-1.0-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: person_tuition
#/giga/deployment/gcom full -s /giga/deployment/services/person_tuition -j /giga/deployment/services/person_tuition/Person_Tuition-1.4-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311
#  service: get_exams_service
#/giga/deployment/gcom full -s /giga/deployment/services/get_exams -j /giga/deployment/services/get_exams/Exams-1.1-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311

# service: get_core_curriculum
/giga/deployment/gcom full -s /giga/deployment/services/get_core_curriculum -j /giga/deployment/services/get_core_curriculum/Get_Core_Curriculum-1.1-SNAPSHOT-jar-with-dependencies.jar -c 3 -m 1g -i 3 -p 8113-8311
