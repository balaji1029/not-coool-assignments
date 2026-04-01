// class SimpleClass {
//     int x;
//     int y;

//     public SimpleClass() {}

//     public int sum(int a, int b) {
//         return a + b;
//     }
// }


// public class Test {
//     public static void main(String[] args) {
//         SimpleClass sc = new SimpleClass();
//         int r = sc.sum(1, 2);
//     }
// }

interface Blah {
    void canfly();
}

class Base {
    int a;

    class Idk {
        int x;
    }

    public void foo() {}
    public void bar() {}

}

class Derived extends Base {
    int b;
    Base blah;
    float x;
    char y;
    double z;
    public void foo() {}
    // public void bar() {}
}

class A extends B {
    int a;
    int b;
    int c;

    public void blah() {}
}

class B extends C {
    int b;
    int c;
    int d;
    
    public void blah() {}
    public void blahblah() {}
}

class C {
    int c;
    int d;
    Derived idk;

    public void blah() {}
}

public class Test {
    public static void main(String[] args) {
        Base b1 = new Base();
        b1.foo();
        b1.bar();

        Base b2 = new Derived();                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
        b2.foo();
        b2.bar();
    }
}
