#! /bin/bash
set -e

## Start minikube before
#minikube status &> /dev/null || echo "[INFO] Starting minikube" && minikube start

echo "[INFO] Building the image in minikube env"
cd ..
eval $(minikube docker-env) && \
mvn clean package -DskipTests -pl "app-backend" jib:dockerBuild

echo "[INFO] Deploying"
mvn -pl "app-deployment" bundlebee:apply@k8s

echo "[INFO] Enabling metrics-server"
minikube addons enable metrics-server

echo "[INFO] Starting dashboard"
minikube dashboard

## Show and open all observability consoles
#minikube service --all -n observability
