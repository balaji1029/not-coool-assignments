class B {
}

class A {
    B f;
}

public class Test {
    public static void main(String [] args) {
        A p = new A();
        B a = new B();
        B b = new B();
        B q;
        if (a == b) {
            p.f = a;
        } else {
            p.f = b;
        }
        q = p.f;
        B r = p.f;
        System.out.println(q);
    }
}
