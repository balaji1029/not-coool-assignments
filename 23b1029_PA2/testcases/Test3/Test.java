class Node {
    Node f1;
    Node f2;
    Node() {}
    Node baba() {
        return new Node();
    }
    Node bloh() {
        Node b = baba();
        Node c = b.baba();
        return c;
    }
}


public class Test {
    static Node bleh() {
        return new Node();
    }
    public static void main(String[] args) {
        Node a = new Node();
        Node b = a.f1;
        Node x = a.f1;
        Node c, d;

        b.f1 = new Node();

        if (a == b) {
            c = a;
            c.f1 = x;
            d = c.f1;
        } else {
            c = b;
            c.f1 = x;
            d = c.f1;
        }

        // System.out.println(d);       // Node alpha = d;
        Node alpha = c.f1;
        Node r = b.f1;
        // System.out.println(r);
    }
}
