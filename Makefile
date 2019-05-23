build:
	./make_scripts/build.sh

docker_build:
	docker build -t toy:0.1 .

cdocker_build:
	docker build -f CDockerfile -t ctoy:0.1 .

docker-composer:
	./make_scripts/docker-composer.sh

docker-generate-configuration:
	./make_scripts/generate_configuration.sh

docker-full-build:
	make build
	make docker_build
	make cdocker_build
	make docker-generate-configuration
	make docker-composer

docker-generate-run:
	make docker-generate-configuration
	make docker-composer

docker-run:
	docker-compose up

docker-run-tests:
	./make_scripts/runner.sh

clean:
	./make_scripts/clean.sh



