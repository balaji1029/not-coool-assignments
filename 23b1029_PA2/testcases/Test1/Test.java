class Node {
	Node f1;
	Node f2;
	Node g;
	Node() {}
}

public class Test {
	public static void main(String[] args) {
		int i = 0;
		while (i < 10) {
			Node a = new Node(); // O12
			a.f1 = new Node(); // O13
			Node b = new Node(); // O14
			b.f1 = new Node(); // O15
			a.f2 = new Node(); // O16
			Node c = a.f1;
			a.f2 = a.f1; // Redundant
			b.f1 = a.f2; // Redundant
			i += 1;
		}
	}
}
