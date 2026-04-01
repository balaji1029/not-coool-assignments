.PHONY: test%

AnalysisTransformer.class: AnalysisTransformer.java
	javac -cp .:soot-4.6.0-jar-with-dependencies.jar PA2.java

test%: testcases/Test%/Test.class AnalysisTransformer.class
	java -cp .:soot-4.6.0-jar-with-dependencies.jar PA2 Test$*

testcases/Test%/Test.class: testcases/Test%/Test.java
	javac $^