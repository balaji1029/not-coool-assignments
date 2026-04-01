# Programming Assignment 1 (PA1) [CS6004 - Code Optimization for Object-Oriented Languages][Deadline : Jan 24, 2026 11:55PM]

## Objective

For all **application classes**, in the input Java program, print structural information about the class, including its fields, object size, and methods, in a strictly defined format.

## Output Order

Classes must be processed and printed in **lexicographical order** of class names.

## Output Format (elaboration follows this section)

For each class, print:

```text
CLASS <ClassName>
FIELDS
<DeclaringClass>::<type> <fieldName>
...
OBJECT_SIZE <size>
METHODS
<DeclaringClass>::<returnType> <name>(<parameterTypeArgsList>)
...
END_CLASS
----------------------[ <newline break> ]----------------------
CLASS <ClassName>
FIELDS
<DeclaringClass>::<type> <fieldName>
...
OBJECT_SIZE <size>
METHODS
<DeclaringClass>::<returnType> <name>(<parameterTypeArgsList>)
...
END_CLASS
```

> ## FIELDS

For a class `C`, print all fields that **contribute to the object size of `C`**.

- Field ordering must follow declaration order.
Each field must be printed as:

```text
<DeclaringClass>::<type> <fieldName>
```

> ## OBJECT_SIZE

size [bytes] should include the default 12-byte object header, along with the memory contributed by the object’s fields

Ignore padding, and alignment.

Assume the following sizes:

| Type             | Size (bytes) |
| ---------------- | ------------ |
| `byte`, `boolean`   | 1 |
| `short`, `char`     | 2 |
| `int`, `float`      | 4 |
| `long`, `double`    | 8 |
| `object reference` | 4 |

> ## METHODS

For a class `C`, methods must be printed in the **order in which they appear in the vtable of `C`**.Within vtable, relative order follows **method declaration order**.
- Ignore constructors and static methods.

Each method must be printed as:
```text
<DeclaringClass>::<accessModifier> <returnType> <name>(<parameterTypes>)
```

## Examples

Consult the testcases and expected output present in the "examples" directory.


## Submission Instructions

- Submit a tar file named: `rollnum_PA1.tar.gz`
```
tar cvzf rollnum_PA1.tar.gz rollnum_PA1/ 
```
- `rollnum` = your roll number in lowercase
- Archive must contain directory: `rollnum_PA1` 
- Main class inside directory: `PA1.java`
- **Only Java source files** (no `.class` or `.jar` files)

## Evaluation

For a testcase `Test.java` inside the `testcases/` directory, the following commands will be used for evaluation:

```bash
javac Test.java
javac -cp .:soot-4.6.0-jar-with-dependencies.jar PA1.java
java -cp .:soot-4.6.0-jar-with-dependencies PA1
```


## Important Notes

- Output must **match the specified format exactly**
- Do **not** print any extra output
- Comment out all debugging or auxiliary print statements before submission

