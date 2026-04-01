class A {
    A f;
}
public class Test {
    static volatile int x = 2;
    static volatile int y = 2;

    static void foo(A a) {}
    public static void main(String[] args) {
        A a = new A();
        a.f = new A();
        A b = a.f;
        if (x == y) {
            foo(a);
        } else {
        }
        A c = a.f;
    }
}
