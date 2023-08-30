#!/bin/bash

# report abnormal wal file size (= larger than SIZE_TH_KB)

SPACE_NAME="dih-tau-space"
SIZE_TH_KB=4200

G_LIMIT=$((1024 * 1024))
M_LIMIT=1024
LOGS_DIR="/dbagigalogs/sqlite"
DATA_DIR="/dbagigadata"
LOG_F="${LOGS_DIR}/wal_size_monitor.log"
WALS_DIR="${DATA_DIR}/tiered-storage/${SPACE_NAME}"
TS="$(date +'%Y-%m-%d %H:%M:%S')"

function truncate_log() {
    local file_size_threshold_kb=10240
    local log_file_size=$(du -sk $LOG_F | awk '{print $1}')
    if [[ $log_file_size -gt $file_size_threshold_kb ]]; then
        i=1
        while true; do
            if [[ -f ${LOG_F}.${i}.tgz ]]; then
                ((i++))
            else
                mv ${LOG_F} ${LOG_F}.${i}
                tar zcf ${LOG_F}.${i}.tgz ${LOG_F}.${i}
                rm -f ${LOG_F}.${i}
                break
            fi
        done
    fi
}

### MAIN ###
[[ ! -d ${LOGS_DIR} ]] && mkdir ${LOGS_DIR}
touch $LOG_F
truncate_log
echo "${TS} starting $(basename $0) ..." >> $LOG_F
for host in $(runall -s -l | grep -v ===); do
    used_prct=$(ssh $host "df -h ${DATA_DIR}" | awk '{print $5}'| tail -1)
    echo "${TS} $host ${DATA_DIR} used space: $used_prct" >> $LOG_F
    ssh $host "du -sk ${WALS_DIR}/*-wal" | while read l; do
        this_wal_size=$(echo $l | awk '{print $1}')
        if [[ ${this_wal_size} -gt $SIZE_TH_KB ]]; then
            this_wal_file_name=$(basename $(echo $l | awk '{print $2}'))
            if [[ ${this_wal_size} -ge $G_LIMIT ]]; then
                this_wal_size_human="$((${this_wal_size} / 1024 / 1024))GB"
            elif [[ ${this_wal_size} -ge $M_LIMIT ]]; then
                this_wal_size_human="$((${this_wal_size} / 1024))MB"
            else
                this_wal_size_human="${this_wal_size}KB"
            fi
            echo "${TS} $host ${this_wal_size_human} ${this_wal_file_name}" >> $LOG_F
        fi
    done
done

exit
