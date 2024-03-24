#! /bin/bash

set -e

minikube status &> /dev/null || echo "[INFO] Starting minikube" && minikube start

echo "[INFO] Building the image in minikube env"
eval $(minikube docker-env) && \
mvn clean package -DskipTests -pl "$(dirname $0)/../app-backend" jib:dockerBuild

echo "[INFO] Deploying"
mvn -pl "$(dirname $0)" bundlebee:apply@k8s

echo "[INFO] Enabling metrics-server"
minikube addons enable metrics-server

echo "[INFO] Starting dashboard"
minikube dashboard
