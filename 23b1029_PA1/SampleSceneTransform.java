
import soot.SceneTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;

import java.util.ArrayList;
import soot.util.Chain;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SampleSceneTransform extends SceneTransformer {
    /**
     * This method is invoked by Soot once the entire program has been loaded.
     * Implement your analysis logic here to iterate over all application classes
     * and print the required information in the specified format.
     */
    private Map<String, Integer> map = new HashMap<>();
    
    private int printFields(Chain<SootField> fields) {
        int object_size = 0;
        for (SootField sf: fields) {
            // Not static fields
            if (sf.isStatic()) continue;
            String type = sf.getType().toString();
            System.out.println(sf.getDeclaringClass() + "::" + type + " " + sf.getName());

            // If the field is a primitive
            if (map.containsKey(type)) {
                object_size += map.get(type);
            // Else it's a reference
            } else {
                object_size += 4;
            }
        }
        return object_size;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        map.put("byte", 1);
        map.put("boolean", 1);
        map.put("short", 2);
        map.put("char", 2);
        map.put("int", 4);
        map.put("float", 4);
        map.put("long", 8);
        map.put("double", 8);

        List<SootClass> classes = new ArrayList<>(Scene.v().getApplicationClasses());
        classes.sort(Comparator.comparing(SootClass::getName));

        for (SootClass sc: classes) {

            if (!sc.isConcrete()) {
                System.out.println("Found an Abstract Class");
                continue;
            }

            // Class Name Print out
            System.out.println("CLASS " + sc.getName());
            Chain<SootField> fields;

            // Fields Print out
            System.out.println("FIELDS");
            int object_size = 12;
            SootClass current_class = sc;
            // SootClass super_class = sc.getSuperclass();

            // Accumulate the super-classes until java.lang.Object is reached
            List<SootClass> class_list = new ArrayList<>();
            while (current_class.getName() != "java.lang.Object") {
                class_list.add(current_class);
                current_class = current_class.getSuperclass();
            }

            // Print in reverse order
            Collections.reverse(class_list);
            for (SootClass subclass: class_list) {
                fields = subclass.getFields();
                object_size += printFields(fields);
            }

            // Object Size Print out
            System.out.println("OBJECT_SIZE " + object_size);

            // Methods Print out
            System.out.println("METHODS");

            // Linked Hash Map to preserve the order of keys in the map
            LinkedHashMap<String, String> lhm = new LinkedHashMap<>();

            for (SootClass subclass: class_list) {

                // Get methods from the class
                List<SootMethod> methods = subclass.getMethods();                                                     
                for (SootMethod sm: methods) {
                    // Ignore Constructors and static methods
                    if (sm.isConstructor() || sm.isStatic()) continue;

                    // Concatenate to get the paramenter list
                    List<String> type_str = new ArrayList<>();
                    List<Type> types = sm.getParameterTypes();
                    for (Type t: types) {
                        type_str.add(t.toString());
                    }

                    // Method Declaration
                    String method_decl = sm.getReturnType() + " " + sm.getName() + "(" + String.join(", ", type_str) + ")";

                    // Insert into the Linked Hash Map
                    lhm.put(method_decl, subclass.getName());
                }
            }

            for (Entry<String, String> mapElement : lhm.entrySet()) {
                System.out.println(mapElement.getValue() + "::" + mapElement.getKey());
            }
            System.out.println("END_CLASS");
            System.out.println();
        }
    }
}