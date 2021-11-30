.PHONE: package image push deploy
all: package image push deploy

DEPLOY_DATE := $(shell date +%s)

package:
	./mvnw clean package -DskipTests
image:
	docker build -f src/main/docker/Dockerfile.jvm -t quay.io/jules0/qiot-manufacturing-factory-machinemetrics:latest .
push:
	docker push quay.io/jules0/qiot-manufacturing-factory-machinemetrics:latest
deploy:
	oc patch deployment machinemetrics -p "{\"spec\": {\"template\": {\"metadata\": { \"labels\": {  \"redeploy\": \"${DEPLOY_DATE}\"}}}}}"

