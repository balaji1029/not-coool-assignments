all: soot run

.PHONY: soot run

soot: 
	javac -cp .:soot-4.6.0-jar-with-dependencies.jar PA1.java

run:
	java -cp .:soot-4.6.0-jar-with-dependencies.jar PA1

clean:
	rm *.class */*.class