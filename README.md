# not-coool-assignments

The assignments require the soot jar to be added to the classpath and compiled. Check out the Makefile in each assignment for details. The code is structured in a way that you can run the main method of the AnalysisTransformer class to execute the analysis. You can also run the test cases provided in the test directory to verify your implementation.


## Assignment 1: Predicting the structure of a Java object of each class

This assignment required us to predict the structure and size of the Java object ignoring the reordering of fields and the padding bytes. The main idea was to analyze the class files and determine the fields and their types, and then calculate the size based on the types of the fields. We also had to consider inheritance and how it affects the structure of the object.

And we also had to print out the structure of the vtable for each class, which includes the method signatures and their corresponding offsets in the vtable. This required us to analyze the methods of each class and determine their signatures and how they are laid out in the vtable.

## Assignment 2: Finding out redundant loads in Java jimple IR code

This assignment required us to analyze the jimple IR code and identify redundant loads. A load is considered redundant if it loads a value that has already been loaded and has not been modified since the last load. We had to analyze the control flow of the program and keep track of the values that have been loaded to identify redundant loads.

We had to implement a simple <b>flow-sensitive path-sensitive intra-procedural data flow analysis</b> to identify redundant loads. This involved keeping track of the values that have been loaded at each point in the program and checking for redundancy when a load instruction is encountered.

## Assignment 3: Finding out possible scalar replaceable objects in Java jimple IR code

This assignment required us to analyze the jimple IR code and identify objects that can be replaced with scalar values. An object is considered scalar replaceable if it is only used in a way that does not require it to be an object, such as being used in arithmetic operations or being passed to methods that do not require an object.

I implemented a similar flow-sensitive path-sensitive intra-procedural data flow analysis as in Assignment 2, but this time we had to keep track of the usage of objects and determine if they can be replaced with scalar values. We had to analyze the control flow of the program and keep track of how objects are used to identify scalar replaceable objects. I also used parameters and their children to map to the objects and determine if they can be replaced with scalar values.