
public class Test {
    static Test f;
    Test f1;
    
    public static void main(String[] args) {
        Test a = new Test();
        Test.f = a;
        Test b = Test.f;
        Test c = Test.f;
        Test d = c.f1;
        Test e = c.f1;
    }
}
