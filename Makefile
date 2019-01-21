BASE_DIR=.
BIN_DIR=$(BASE_DIR)/bin
RESOURCES_DIR=$(BIN_DIR)/src/main/resources
C=1
F=0
bin-build:
	mvn install && \
	rm -r $(RESOURCES_DIR)/* && \
	sudo chmod 777 $(BIN_DIR)/run_docker.sh && \
	sudo chmod 777 $(BIN_DIR)/run_single.sh

docker_build:
	docker build -t toy:0.1 .

docker-composer:
	./make_scripts/docker-composer.sh $(BASE_DIR) $(C)

docker-generate_configuration:
	./make_scripts/generate_configuration.sh $(BASE_DIR) $(C) $(F)

docker-full-build:
	make docker-generate_configuration
	make docker-composer
	make bin-build
	make docker_build

docker-run:
	docker-compose up

docker-run-tests:
	./make_scripts/runner.sh


