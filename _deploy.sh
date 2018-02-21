#!/usr/bin/env bash
set -e
. _vars.sh
T=$(mktemp)
kubectl delete deploy ${DEPLOYMENT} || true
cat manifests/deployment.yaml | sed "s/REGISTRY/${REGISTRY}/g" | sed "s/IMAGE/${IMAGE}/g" | sed "s/AUTHOR/${AUTHOR}/g" | sed "s/TAG/${TAG}/g" | sed "s/DEPLOYMENT/${DEPLOYMENT}/g" > ${T}
echo "---\n" >> ${T}
cat manifests/ingress.yaml >> ${T}
echo "---\n" >> ${T}
cat manifests/service.yaml >> ${T}
cat ${T}
kubectl apply -f ${T}
echo "|------------------------------------------------------------"
echo "|"
echo "| [JAVA PROTOTYPE]"
echo "|"
echo "| Created Kubernetes Deployment: ${DEPLOYMENT}"
echo "|"
echo "|------------------------------------------------------------"
echo ""
echo ""