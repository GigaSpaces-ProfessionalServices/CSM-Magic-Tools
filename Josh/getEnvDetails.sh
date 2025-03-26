#!/bin/bash
ENV=$(kubectl get ns |grep -iw $1 |awk '{print $1}')
if [[ -z $ENV ]];then
    echo "Please specify a valid environment name."
    exit
fi
    
echo "==========================="
echo "Environment: $ENV"
echo "==========================="
echo

# echo "RDS details:"
# echo "------------"
# TF=$(kubectl get secrets -n $ENV |grep tf |awk '{print $1}')
# if [[ ! -z $TF ]];then
#     kubectl view-secret -n $ENV $TF --all
# fi
# echo

echo "RDS details:"
echo "------------"
TF_SECRETS=$(kubectl get secrets -n $ENV | grep tf | awk '{print $1}')

if [[ -n "$TF_SECRETS" ]]; then
    for TF in $TF_SECRETS; do
        OUTPUT=$(kubectl view-secret -n $ENV $TF --all)
        PORT=$(echo "$OUTPUT" | grep "port=" | cut -d"'" -f2)

        case $PORT in
            1521) DB_TYPE="Oracle" ;;
            1433) DB_TYPE="MSSQL" ;;
            5432) DB_TYPE="Postgres" ;;
            3306) DB_TYPE="MySQL" ;;
            *) DB_TYPE="Unknown" ;;
        esac

        echo "RDS: $DB_TYPE"
        echo "$OUTPUT"
        echo "------------"
    done
else
    echo "No secrets found."
fi
echo


echo "metadata details:"
echo "-----------------"
METADATA_SVC=$(kubectl get svc -n $ENV |grep backend-pooler |awk '{print $1}')
HOST=$(kubectl get svc -n $ENV $METADATA_SVC -o yaml |grep "external-dns.alpha.kubernetes.io/hostname:" |awk '{print $2}')
echo "Host: "$HOST 
echo "Port: 5342"
METADATA=$(kubectl get secrets -n $ENV |grep metadata |awk '{print $1}')
kubectl view-secret -n $ENV $METADATA --all
echo

echo "datalab:"
echo "--------"
DATALAB_SVC=$(kubectl get svc -n $ENV |grep data-lab-pooler |awk '{print $1}')
HOST=$(kubectl get svc -n $ENV $DATALAB_SVC -o yaml |grep "external-dns.alpha.kubernetes.io/hostname:" |awk '{print $2}')
echo "Host: "$HOST
echo "Port: 5342" 
DATALAB=$(kubectl get secrets -n $ENV |grep datalab |awk '{print $1}')
kubectl view-secret -n $ENV $DATALAB --all
echo

echo "eRAG UI URL"
echo "-----------"
UI_ING=$(kubectl get ing -n $ENV |grep ui |awk '{print $1}')
UI_HOST=$(kubectl get ing -n $ENV $UI_ING -o json |grep -w '"host":' |awk '{print $2}' | sed 's/[",]//g')
echo "https://"$UI_HOST
echo

echo "argoCD URL"
echo "----------"
ARGO_URL="https://argocd.mercury.gigaspaces.net/applications/argocd"
ARGO_INST=$(kubectl get svc -n $ENV -o yaml |grep argocd.argoproj.io/instance: |awk '{print $2}' |head -1)
echo $ARGO_URL/$ARGO_INST
echo

echo "eRAG swagger"
echo "------------"
echo "https://$UI_HOST/api/erag-backend/docs"
echo

echo "milvus URL"
echo "---------"
ATTU_ING=$(kubectl get ing -n $ENV |grep attu |awk '{print $1}')
ATTU_HOST=$(kubectl get ing -n $ENV $ATTU_ING -o json |grep -w '"host":' |awk '{print $2}' | sed 's/[",]//g')
echo "https://"$ATTU_HOST
