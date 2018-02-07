default: build

build:
	@mvn clean package

run:
	@java -Dserver.port=8080 -jar target/java-prototype*

dev: build run
