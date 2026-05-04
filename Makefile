build: build/Prog4.class

build/Prog4.class:
	mkdir -p build
	javac -d build src/*.java

clean:
	rm -rf build

run: build
	java -cp build:/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar Prog4
