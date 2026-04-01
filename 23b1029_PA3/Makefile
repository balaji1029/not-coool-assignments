.PHONY: test%

all: test1

AnalysisTransformer.class: AnalysisTransformer.java PA3.java
	javac -cp .:soot-4.6.0-jar-with-dependencies.jar PA3.java

test%: testcases/Test%/Test.class AnalysisTransformer.class
	java -cp .:soot-4.6.0-jar-with-dependencies.jar PA3 Test$*

testcases/Test%/Test.class: testcases/Test%/Test.java
	javac $^