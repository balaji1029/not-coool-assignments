class Node {
    Node f;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();
        Node b = new Node();
        Node x;
        if (a == b) {
            x = a;
        } else {
            x = b;
        }
        b.f = new Node();
        Node y = b.f;
        Node z = x.f;
        System.out.println(y);
    }
}
