class B {
    B f;
}
class A {

}
public class Test {
    static volatile int x = 1;
    static volatile int y = 2;
    public static void main(String [] args) {
        B a = new B();
        a.f = new B();
        B b = new B();

        B x;
        if (a == b) {
            x = a.f;
        } else {
            x = a.f;
        }        
        System.out.println(x);
        B alpha = a.f;
    }
}