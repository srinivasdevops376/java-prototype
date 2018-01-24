default: build

build:
	@mvn clean package

run:
	@java --illegal-access=deny -Dserver.port=8080 -jar target/monjavpro*

dev: build run
