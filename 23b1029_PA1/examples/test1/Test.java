class SimpleClass {
    int x;
    int y;

    public SimpleClass() {}

    public int sum(int a, int b) {
        return a + b;
    }
}


public class Test {
    public static void main(String[] args) {
        SimpleClass sc = new SimpleClass();
        int r = sc.sum(1, 2);
    }
}
