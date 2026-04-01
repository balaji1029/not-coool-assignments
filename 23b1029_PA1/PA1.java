import soot.PackManager;
import soot.Transform;

public class PA1 {
    public static void main(String[] args) {
        String classPath = "./testcases/"; //do not change this, as evaluation would have testcases under this directory

        //Set up arguments for Soot
        String[] sootArgs = {
            "-cp", classPath, 
            "-pp", // sets the class path for Soot
            "-f", "J",
            "-t", "1",
            "-w",
            "-main-class", "Test",	// specify the main class
            "-process-dir", classPath
        };

        // Create transformer for analysis
        SampleSceneTransform pass = new SampleSceneTransform();

        // Add transformer to appropriate pack in PackManager; PackManager will run all packs when soot.Main.main is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.idfa", pass));

        // Call Soot's main method with arguments
        soot.Main.main(sootArgs);
    }
}