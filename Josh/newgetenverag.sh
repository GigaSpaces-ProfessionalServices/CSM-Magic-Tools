#!/bin/bash

print_separator() {
    echo "==========================="
    echo "$1"
    echo "==========================="
    echo
}

get_hostname() {
    local svc_name=$1
    kubectl get svc -n "$ENV" "$svc_name" -o jsonpath="{.metadata.annotations.external-dns\.alpha\.kubernetes\.io/hostname}" 2>/dev/null
}

get_ingress_host() {
    local ing_name=$1
    kubectl get ing -n "$ENV" "$ing_name" -o jsonpath="{.spec.rules[0].host}" 2>/dev/null
}

# Validate environment
if [[ -z $1 ]]; then
    echo "Please specify a valid environment name."
    kubectl get ns
    exit 1
fi

ENV=$(kubectl get ns | awk -v env="$1" '$1 == env {print $1}')
if [[ -z "$ENV" ]]; then
    echo "Environment '$1' not found."
    kubectl get ns
    exit 1
fi

print_separator "Environment: $ENV"

# RDS Details
echo "RDS details:"
echo "------------"
TF_SECRETS=$(kubectl get secrets -n "$ENV" | awk '/tf/ {print $1}')
if [[ -n "$TF_SECRETS" ]]; then
    for secret in $TF_SECRETS; do
        output=$(kubectl view-secret -n "$ENV" "$secret" --all)
        port=$(echo "$output" | grep "port=" | cut -d"'" -f2)
        case $port in
            1521) db_type="Oracle" ;;
            1433) db_type="MSSQL" ;;
            5432) db_type="Postgres" ;;
            3306) db_type="MySQL" ;;
            *) db_type="Unknown" ;;
        esac
        echo "RDS: $db_type"
        echo "$output"
        echo "------------"
    done
else
    echo "No RDS-related secrets found."
fi
echo

# Metadata
echo "Metadata details:"
echo "-----------------"
metadata_svc=$(kubectl get svc -n "$ENV" | awk '/backend-pooler/ {print $1}')
metadata_host=$(get_hostname "$metadata_svc")
echo "Host: $metadata_host"
echo "Port: 5342"
metadata_secret=$(kubectl get secrets -n "$ENV" | awk '/metadata/ {print $1}')
kubectl view-secret -n "$ENV" "$metadata_secret" --all
echo

# Datalab
echo "Datalab:"
echo "--------"
datalab_svc=$(kubectl get svc -n "$ENV" | awk '/data-lab-pooler/ {print $1}')
datalab_host=$(get_hostname "$datalab_svc")
echo "Host: $datalab_host"
echo "Port: 5342"
datalab_secret=$(kubectl get secrets -n "$ENV" | awk '/datalab/ {print $1}')
kubectl view-secret -n "$ENV" "$datalab_secret" --all
echo

# eRAG UI
echo "eRAG UI URL:"
echo "-----------"
ui_ing=$(kubectl get ing -n "$ENV" | awk '/ui/ {print $1}')
ui_host=$(get_ingress_host "$ui_ing")
echo "https://$ui_host"
echo

# ArgoCD
echo "ArgoCD URL:"
echo "----------"
argo_url="https://argocd.mercury.gigaspaces.net/applications/argocd"
argo_inst=$(kubectl get svc -n "$ENV" -o yaml | awk '/argocd.argoproj.io\/instance:/ {print $2; exit}')
echo "$argo_url/$argo_inst"
echo

# Swagger
echo "eRAG Swagger:"
echo "------------"
echo "https://$ui_host/api/erag-backend/docs"
echo

# Milvus
echo "Milvus URL:"
echo "-----------"
attu_ing=$(kubectl get ing -n "$ENV" | awk '/attu/ {print $1}')
attu_host=$(get_ingress_host "$attu_ing")
echo "https://$attu_host"

