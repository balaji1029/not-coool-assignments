class Base {
    int a;

    public void foo() {}
    public void bar() {}

}
class Derived extends Base {
    int b;
    public void bar() {}
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
