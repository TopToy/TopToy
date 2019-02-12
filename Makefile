
bin-build:
	./make_scripts/bin-build.sh

docker_build:
	docker build -t toy:0.1 .

docker-composer:
	./make_scripts/docker-composer.sh

docker-generate_configuration:
	./make_scripts/generate_configuration.sh

docker-full-build:
	make docker-generate_configuration
	make docker-composer
	make bin-build
	make docker_build

docker-run:
	docker-compose up

docker-run-tests:
	./make_scripts/runner.sh


