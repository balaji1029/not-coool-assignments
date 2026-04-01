import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ForwardFlowAnalysis;

class HeapObject {
    Type type;
    int lineNumber;
    boolean isMarked;

    HeapObject(Type type) {
        this.type = type;
    }

    HeapObject(Type type, int lineNumber) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.isMarked = false;
    }

    HeapObject(Type type, int lineNumber, boolean isMarked) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.isMarked = isMarked;
    }

    public void mark() {
        // System.out.println("Marking obj_" + lineNumber);
        this.isMarked = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        HeapObject other = (HeapObject) obj;

        if ((type == other.type) && (lineNumber == other.lineNumber) && (isMarked == other.isMarked))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lineNumber, isMarked);
    }

    @Override
    public String toString() {
        if (isMarked)
            return "X" + type.toString() + "_" + lineNumber + "X";
        return type.toString() + "_" + lineNumber;
    }
}

class NullObject extends HeapObject {
    HeapObject heapObject;
    SootField field;

    public NullObject(Type type, HeapObject hObject, SootField field) {
        super(type);
        this.heapObject = hObject;
        this.field = field;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        NullObject other = (NullObject) obj;

        if ((type == other.type) && (heapObject.equals(other.heapObject)) && (field == other.field))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, heapObject, field);
    }

    @Override
    public String toString() {
        return heapObject.type + "_" + heapObject.lineNumber + "::" + field + "_Nullable";
    }

}

class PointsToGraph {
    Map<Local, Set<HeapObject>> stackVars = new HashMap<>();
    Map<HeapObject, Map<SootField, Set<HeapObject>>> heapVars = new HashMap<>();
    Map<HeapObject, Set<Local>> inStack = new HashMap<>();
    Set<Local> canBeUninitialized = new HashSet<>();

    private static boolean stackMapsEqual(Map<Local, Set<HeapObject>> map1, Map<Local, Set<HeapObject>> map2) {
        if (map1.size() != map2.size())
            return false;

        for (Map.Entry<Local, Set<HeapObject>> entry : map1.entrySet()) {
            Set<HeapObject> set2 = map2.get(entry.getKey());
            if (set2 == null || !setsEqual(entry.getValue(), set2))
                return false;
        }

        return true;
    }

    private static boolean heapMapsEqual(Map<HeapObject, Map<SootField, Set<HeapObject>>> map1,
            Map<HeapObject, Map<SootField, Set<HeapObject>>> map2) {
        if (map1.size() != map2.size())
            return false;
        for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> entry : map1.entrySet()) {
            Map<SootField, Set<HeapObject>> inner2 = map2.get(entry.getKey());
            if (inner2 == null || !innerMapsEqual(entry.getValue(), inner2))
                return false;
        }
        return true;
    }

    private static boolean innerMapsEqual(Map<SootField, Set<HeapObject>> map1, Map<SootField, Set<HeapObject>> map2) {
        if (map1.size() != map2.size())
            return false;

        for (Map.Entry<SootField, Set<HeapObject>> e : map1.entrySet()) {
            Set<HeapObject> set2 = map2.get(e.getKey());
            if (set2 == null || !setsEqual(e.getValue(), set2))
                return false;
        }
        return true;
    }

    private static boolean setsEqual(Set<HeapObject> set1, Set<HeapObject> set2) {
        if (set1 == set2)
            return true;
        if (set1 == null || set2 == null)
            return false;
        if (set1.size() != set2.size())
            return false;
        return set1.containsAll(set2);
    }

    private static boolean localSetsEqual(Set<Local> set1, Set<Local> set2) {
        if (set1 == set2)
            return true;
        if (set1 == null || set2 == null)
            return false;
        if (set1.size() != set2.size())
            return false;
        return set1.containsAll(set2);
    }

    private static boolean inStacksEqual(Map<HeapObject, Set<Local>> map1, Map<HeapObject, Set<Local>> map2) {
        if (map1.size() != map2.size())
            return false;

        for (Map.Entry<HeapObject, Set<Local>> e : map1.entrySet()) {
            Set<Local> set2 = map2.get(e.getKey());
            if (set2 == null || !localSetsEqual(e.getValue(), set2))
                return false;
        }

        return true;
    }

    public PointsToGraph() {
    }

    public HeapObject mark_object(HeapObject obj) {
        HeapObject newMarkedObject = new HeapObject(obj.type, obj.lineNumber, true);
        for (Map.Entry<Local, Set<HeapObject>> e : stackVars.entrySet()) {
            if (e.getValue().contains(obj)) {
                e.getValue().remove(obj);
                e.getValue().add(newMarkedObject);
            }
        }

        Map<HeapObject, Map<SootField, Set<HeapObject>>> newHeapVars = new HashMap<>();
        for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e : heapVars.entrySet()) {
            if (e.getKey().equals(obj)) {
                newHeapVars.put(newMarkedObject, new HashMap<>());
                for (Map.Entry<SootField, Set<HeapObject>> mapEntries : heapVars.get(e.getKey()).entrySet()) {
                    newHeapVars.get(newMarkedObject).put(mapEntries.getKey(), new HashSet<>(mapEntries.getValue()));
                }
            } else {
                newHeapVars.put(e.getKey(), new HashMap<>());
                for (Map.Entry<SootField, Set<HeapObject>> mapEntries : e.getValue().entrySet()) {
                    newHeapVars.get(e.getKey()).put(mapEntries.getKey(), new HashSet<>(mapEntries.getValue()));
                }
            }
        }

        for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e : newHeapVars.entrySet()) {
            for (Map.Entry<SootField, Set<HeapObject>> mapEntry : e.getValue().entrySet()) {
                if (mapEntry.getValue().contains(obj)) {
                    mapEntry.getValue().remove(obj);
                    mapEntry.getValue().add(newMarkedObject);
                }
            }
        }

        heapVars = newHeapVars;

        return newMarkedObject;
    }

    public void mark(HeapObject obj) {
        if (obj.isMarked)
            return;
        // obj.mark();
        // System.out.println("Marking " + obj);
        HeapObject newMarkedObject = mark_object(obj);
        List<HeapObject> toMark = new ArrayList<>();
        for (Map.Entry<SootField, Set<HeapObject>> e : heapVars.computeIfAbsent(newMarkedObject, k -> new HashMap<>())
                .entrySet()) {
            for (HeapObject childObject : e.getValue()) {
                // mark(childObject);
                toMark.add(childObject);
            }
        }
        for (HeapObject o : toMark) {
            mark(o);
        }
    }

    public Set<HeapObject> getHeapObjects(Local local, SootField field) {
        Set<HeapObject> returningSet = new HashSet<>();
        Set<HeapObject> bases = stackVars.computeIfAbsent(local, k -> new HashSet<>());
        for (HeapObject obj : bases) {
            returningSet.addAll(
                    heapVars.computeIfAbsent(obj, k -> new HashMap<>()).computeIfAbsent(field, k -> new HashSet<>()));
        }
        return returningSet;
    }

    public void copy(PointsToGraph src) {
        stackVars = new HashMap<>();
        for (Map.Entry<Local, Set<HeapObject>> e : src.stackVars.entrySet()) {
            stackVars.put(e.getKey(), new HashSet<>(e.getValue()));
        }

        heapVars = new HashMap<>();
        for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e : src.heapVars.entrySet()) {
            Map<SootField, Set<HeapObject>> fieldMap = new HashMap<>();
            for (Map.Entry<SootField, Set<HeapObject>> h : e.getValue().entrySet()) {
                fieldMap.put(h.getKey(), new HashSet<>(h.getValue()));
            }
            heapVars.put(e.getKey(), fieldMap);
        }

        inStack = new HashMap<>();
        for (Map.Entry<HeapObject, Set<Local>> e : src.inStack.entrySet()) {
            inStack.put(e.getKey(), new HashSet<>(e.getValue()));
        }

        canBeUninitialized = new HashSet<>(src.canBeUninitialized);
    }

    public void union(PointsToGraph src) {
        for (Map.Entry<Local, Set<HeapObject>> e : src.stackVars.entrySet()) {
            Set<HeapObject> target = stackVars.computeIfAbsent(e.getKey(), k -> new HashSet<>());
            target.addAll(e.getValue());
        }
        for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e : src.heapVars.entrySet()) {
            Map<SootField, Set<HeapObject>> target_map = heapVars.computeIfAbsent(e.getKey(), k -> new HashMap<>());
            for (Map.Entry<SootField, Set<HeapObject>> h : e.getValue().entrySet()) {
                // HeapObject nullObject = new HeapObject(h.getKey().getType(), 0, true);
                HeapObject nullObject = new NullObject(h.getKey().getType(), e.getKey(), h.getKey());
                Set<HeapObject> nullSet = new HashSet<>();
                nullSet.add(nullObject);
                Set<HeapObject> target_set = target_map.computeIfAbsent(h.getKey(), k -> nullSet);
                target_set.addAll(h.getValue());
            }
        }
        Map<HeapObject, Set<Local>> newInStack = new HashMap<>();
        for (Map.Entry<HeapObject, Set<Local>> e : src.inStack.entrySet()) {
            newInStack.put(e.getKey(), new HashSet<>());
            if (!newInStack.containsKey(e.getKey()))
                continue;
            for (Local local : e.getValue()) {
                if (inStack.get(e.getKey()).contains(local)) {
                    newInStack.get(e.getKey()).add(local);
                }
            }
        }
        inStack = newInStack;

        // Set<Local> newUninitializedLocals = new HashSet<>();
        canBeUninitialized.addAll(src.canBeUninitialized);
        // for (Local l : src.canBeUninitialized) {
        // if (canBeUninitialized.contains(l)) {
        // newUninitializedLocals.add(l);
        // }
        // }
        // canBeUninitialized = newUninitializedLocals;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        PointsToGraph other = (PointsToGraph) obj;

        if (!stackMapsEqual(stackVars, other.stackVars))
            return false;

        if (!heapMapsEqual(heapVars, other.heapVars))
            return false;

        if (!inStacksEqual(inStack, other.inStack))
            return false;

        if (!localSetsEqual(canBeUninitialized, other.canBeUninitialized))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackVars, heapVars, inStack, canBeUninitialized);
    }

    @Override
    public String toString() {
        return stackVars + "\n" + heapVars + "\n" + inStack + "\n" + canBeUninitialized;
    }
}

class PointerAnalysis extends ForwardFlowAnalysis<Unit, PointsToGraph> {
    List<Local> params;
    Set<Local> locals;

    // List<String> primitives = Lists.newArrayList("byte", "boolean", "short",
    // "char", "int", "float", "long", "double");

    public PointerAnalysis(Body body) {
        super(new BriefUnitGraph(body));
        params = new ArrayList<>();
        params.addAll(body.getParameterLocals());
        locals = new HashSet<>();
        locals.addAll(body.getLocals());
        doAnalysis();
    }

    @Override
    protected PointsToGraph newInitialFlow() {
        return new PointsToGraph();
    }

    @Override
    protected PointsToGraph entryInitialFlow() {
        PointsToGraph init = new PointsToGraph();

        // Right now, just adding a new dummy heap object for every parameter, will
        // check out what happens later
        for (Local param : params) {
            Type type = param.getType();
            // if (primitives.contains(type.toString())) {
            // continue;
            // }
            // System.out.println(param.getType());
            HeapObject obj = new HeapObject(type, 0, true);
            Set<HeapObject> set = new HashSet<>();
            set.add(obj);
            init.stackVars.put(param, set);
        }

        for (Local l : locals) {
            if (!l.isStackLocal() && !params.contains(l))
                init.canBeUninitialized.add(l);
        }

        return init;
    }

    @Override
    protected void merge(PointsToGraph in1, PointsToGraph in2, PointsToGraph out) {
        out.copy(in1);
        out.union(in2);
    }

    @Override
    protected void copy(PointsToGraph src, PointsToGraph dst) {
        dst.copy(src);
    }

    @Override
    protected void flowThrough(PointsToGraph in, Unit unit, PointsToGraph out) {
        out.copy(in);
        // System.out.print(unit.getJavaSourceStartLineNumber());
        // System.out.println(in);
        // System.out.println();
        // System.out.println(unit.getClass() + ":" + unit);

        if (unit instanceof JAssignStmt) {
            JAssignStmt assn = (JAssignStmt) unit;
            Value lValue = assn.getLeftOp();
            Value rValue = assn.getRightOp();

            if (lValue instanceof Local) {
                Local lLocal = (Local) lValue;
                // System.out.println(rValue.getClass());

                if (out.canBeUninitialized.contains(lLocal))
                    out.canBeUninitialized.remove(lLocal);

                if (rValue instanceof Local) { // Copy
                    Local rLocal = (Local) rValue;

                    // I am taking all the heap objects that rLocal can point to and adding a
                    // points-to to lLocal - String Update
                    // Don't have to take care of new HashSet anyway
                    // Will give an error if rLocal is uninitialized, so fine

                    out.stackVars.put(lLocal,
                            new HashSet<>(out.stackVars.computeIfAbsent(rLocal, k -> new HashSet<HeapObject>())));

                } else if (rValue instanceof JNewExpr) { // Allocation
                    JNewExpr rNewExpr = (JNewExpr) rValue;

                    // I am creating a new allocation site and adding a stack connection to the
                    // lLocal

                    HeapObject obj = new HeapObject(rNewExpr.getType(), unit.getJavaSourceStartLineNumber(), false);
                    // System.out.println("obj_" + unit.getJavaSourceStartLineNumber() + ": " +
                    // obj.isMarked);
                    out.stackVars.put(lLocal, new HashSet<>());
                    out.stackVars.get(lLocal).add(obj);

                } else if (rValue instanceof JInstanceFieldRef) { // Load Statement
                    JInstanceFieldRef rInstanceFieldRef = (JInstanceFieldRef) rValue;

                    Value rBase = rInstanceFieldRef.getBase();
                    SootField rField = rInstanceFieldRef.getField();

                    if (!(rBase instanceof Local)) {
                        // System.out.println("Meow! got a base as local");
                    } else {
                        Local rLocalBase = (Local) rBase;

                        // I am getting the stack map of base and it should not be empty, otherwise it
                        // gives an error
                        // Then I am getting the heap objects' field map, in which case, I have to make
                        // dummy objects
                        // Then I am getting the fields' pointers, which can (will) be dummy

                        if (rField.isStatic()) {
                            out.stackVars.put(lLocal, Set.of(new HeapObject(rField.getType(), unit.getJavaSourceStartLineNumber(), true)));
                        } else {
                            Set<HeapObject> newSet = new HashSet<>();
                            for (HeapObject hObject : out.stackVars.computeIfAbsent(rLocalBase,
                                    k -> new HashSet<HeapObject>())) {
    
                                if (hObject.isMarked) {
                                    newSet.add(new HeapObject(rField.getType(), unit.getJavaSourceStartLineNumber(), true));
                                    continue;
                                }
    
                                newSet.addAll(out.heapVars
                                        .computeIfAbsent(hObject, k -> new HashMap<SootField, Set<HeapObject>>())
                                        .computeIfAbsent(rField, k -> Set.of(
                                                new NullObject(rField.getType(), hObject, rField)))); // Adds
                                                                                                      // a
                                                                                                      // nullable
                                                                                                      // object
                                                                                                      // as
                                                                                                      // a
                                                                                                      // dummy
                                                                                                      // object
                            }
                            out.stackVars.put(lLocal, newSet);
                        }

                    }
                } else if (rValue instanceof InvokeExpr) {
                    InvokeExpr rInvokeExpr = (InvokeExpr) rValue;

                    out.stackVars.put(lLocal,
                            Set.of(new HeapObject(lLocal.getType(), unit.getJavaSourceStartLineNumber(), true)));

                    if (rInvokeExpr instanceof JVirtualInvokeExpr) {
                        JVirtualInvokeExpr rVirtualInvokeExpr = (JVirtualInvokeExpr) rInvokeExpr;

                        Local rBase = (Local) rVirtualInvokeExpr.getBase();

                        // Marks everything iteratively
                        List<HeapObject> toMark = new ArrayList<>();
                        for (HeapObject obj : out.stackVars.computeIfAbsent(rBase, k -> new HashSet<>())) {
                            // out.mark(obj);
                            toMark.add(obj);
                        }
                        for (HeapObject o : toMark) {
                            out.mark(o);
                        }
                    }

                    List<HeapObject> toMark = new ArrayList<>();
                    for (Value arg : rInvokeExpr.getArgs()) {
                        if (arg instanceof Local) {
                            Local argLocal = (Local) arg;

                            for (HeapObject argObject : out.stackVars.computeIfAbsent(argLocal, k -> new HashSet<>())) {
                                // out.mark(argObject);
                                toMark.add(argObject);
                            }
                        }
                    }
                    for (HeapObject o : toMark) {
                        out.mark(o);
                    }
                }

            } else if (lValue instanceof JInstanceFieldRef) {
                JInstanceFieldRef lInstanceFieldRef = (JInstanceFieldRef) lValue;
                Value lBase = lInstanceFieldRef.getBase();
                SootField lField = lInstanceFieldRef.getField();

                // System.out.println(rValue.getClass());

                if (rValue instanceof Local) { // Store Statement
                    Local rLocal = (Local) rValue;

                    if (!(lBase instanceof Local)) {
                        // System.out.println("Meow! got a base as local");
                    } else {
                        Local lLocalBase = (Local) lBase;
                        Set<HeapObject> heapObjects = new HashSet<>(out.stackVars.computeIfAbsent(lLocalBase,
                                k -> new HashSet<HeapObject>())); // Can't be empty
                        if (heapObjects.size() == 1) { // Strong Update when you know you have only one point that
                                                       // lLocalBase points to
                            HeapObject heapObject = heapObjects.iterator().next();
                            out.heapVars.computeIfAbsent(heapObject, k -> new HashMap<>()).put(lField,
                                    new HashSet<>(out.stackVars.computeIfAbsent(rLocal, k -> new HashSet<>()))); // Stack
                                                                                                                 // variables
                                                                                                                 // can't
                                                                                                                 // be
                                                                                                                 // uninitialized
                            if (lField.isStatic()) {
                                out.mark(heapObject);
                            }
                        } else {
                            // Weak Update when there are more Heap Objects that rLocalBase is pointing to
                            for (HeapObject heapObject : heapObjects) {
                                if (heapObject.isMarked)
                                    continue;
                                out.heapVars.computeIfAbsent(heapObject, k -> new HashMap<>()).computeIfAbsent(lField,
                                        k -> Set.of(
                                                new NullObject(lField.getType(), heapObject, lField)))
                                        .addAll(out.stackVars.computeIfAbsent(rLocal, k -> new HashSet<>()));
                            }
                            if (lField.isStatic()) {
                                for (HeapObject heapObject : heapObjects) {
                                    out.mark(heapObject);
                                }
                            }
                        }
                    }
                } else if (rValue instanceof Constant) {
                    Constant rConstant = (Constant) rValue;

                    // I am creating a new allocation site and adding a stack connection to the
                    // lLocal

                    HeapObject obj = new HeapObject(rConstant.getType(), unit.getJavaSourceStartLineNumber());

                    if (!(lBase instanceof Local)) {
                        // System.out.println("Meow! got a base as local");
                    } else {
                        Local lLocalBase = (Local) lBase;
                        List<HeapObject> toMark = new ArrayList<>();
                        for (HeapObject hObject : out.stackVars.computeIfAbsent(lLocalBase,
                                k -> new HashSet<HeapObject>())) {
                            out.heapVars.computeIfAbsent(hObject, k -> new HashMap<SootField, Set<HeapObject>>())
                                    .computeIfAbsent(lField, k -> new HashSet<HeapObject>())
                                    .add(obj);
                            toMark.add(obj);
                        }
                        if (lField.isStatic()) {
                            for (HeapObject o : toMark) {
                                out.mark(o);
                            }
                        }
                    }
                }
            }
        } else if (unit instanceof JInvokeStmt) {
            JInvokeStmt invokeStmt = (JInvokeStmt) unit;

            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!invokeExpr.getMethod().getName().equals("<init>")) {
                if (invokeExpr instanceof JVirtualInvokeExpr) {
                    JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) invokeExpr;

                    Value base = virtualInvokeExpr.getBase();

                    Local baseLocal = (Local) base;

                    for (HeapObject argObject : out.stackVars.computeIfAbsent(baseLocal, k -> new HashSet<>())) {
                        out.mark(argObject);
                    }
                }

                // Marks everything iteratively
                List<HeapObject> toMark = new ArrayList<>();
                // System.out.println(invokeExpr.getArgCount());
                for (int i = 0; i < invokeExpr.getArgCount(); i++) {
                    Value arg = invokeExpr.getArg(i);
                    if (arg instanceof Local) {
                        Local argLocal = (Local) arg;
                        for (HeapObject argObject : out.stackVars.computeIfAbsent(argLocal, k -> new HashSet<>())) {
                            // out.mark(argObject);
                            toMark.add(argObject);
                        }
                    }
                }
                for (HeapObject o : toMark) {
                    out.mark(o);
                }
            }

        }
    }
}

public class AnalysisTransformer extends BodyTransformer {

    public Map<String, String> result = new HashMap<>();

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {

        // boolean printed = false;

        // System.out.println(body.getMethod());

        PointerAnalysis analysis = new PointerAnalysis(body);

        UnitPatchingChain units = body.getUnits();

        String method_print_out = body.getMethod().getDeclaringClass() + ":" + body.getMethod().getName();

        List<Local> listOfLoads = new ArrayList<>();

        for (Unit unit : units) {
            PointsToGraph pointsToGraph = analysis.getFlowBefore(unit);
            // System.out.println(unit.getJavaSourceStartLineNumber() + " : " + unit);
            // System.out.println(pointsToGraph);
            // System.out.println();

            // PointsToGraph pointsToGraph2 = analysis.getFlowAfter(unit);
            // System.out.println(pointsToGraph2);
            // System.out.println();

            if (unit instanceof JAssignStmt) {
                JAssignStmt assignStmt = (JAssignStmt) unit;
                Value lValue = assignStmt.getLeftOp();
                Value rValue = assignStmt.getRightOp();

                if (lValue instanceof Local) {
                    Local lLocal = (Local) lValue;
                    if (listOfLoads.contains(lLocal))
                        listOfLoads.remove(lLocal);
                    listOfLoads.add(lLocal);

                    if (rValue instanceof JInstanceFieldRef) { // Loads
                        JInstanceFieldRef instanceFieldRef = (JInstanceFieldRef) rValue;

                        Value base = instanceFieldRef.getBase();
                        SootField field = instanceFieldRef.getField();

                        if (field.isStatic())
                            continue;

                        if (base instanceof Local) {
                            Local baseLocal = (Local) base;
                            // Set<HeapObject> suspects = pointsToGraph.getHeapObjects(baseLocal, field);

                            boolean flag = false; 
                            Set<HeapObject> suspects = new HashSet<>();

                            Set<HeapObject> bases = pointsToGraph.stackVars.computeIfAbsent(baseLocal, k -> new HashSet<>()); // Can't be empty
                            for (HeapObject obj : bases) {
                                if (obj.isMarked) {
                                    flag = true;
                                }
                                suspects.addAll(
                                        pointsToGraph.heapVars.computeIfAbsent(obj, k -> new HashMap<>()).computeIfAbsent(field, k -> Set.of(new NullObject(field.getType(), obj, field))));
                            }

                            if (flag)
                                continue;

                            // Set<HeapObject> suspects = new HashSet<>();

                            // for (Map.Entry)

                            if (suspects.size() == 1) {
                                HeapObject obj = suspects.iterator().next();

                                if (obj.isMarked)
                                    continue;

                                List<Local> reduds = new ArrayList<>();
                                for (Map.Entry<Local, Set<HeapObject>> e : pointsToGraph.stackVars.entrySet()) {
                                    if (e.getValue().size() == 1 && e.getValue().contains(obj)
                                            && !e.getKey().isStackLocal()
                                            && !pointsToGraph.canBeUninitialized.contains(e.getKey()))
                                        reduds.add(e.getKey());
                                }
                                for (Local l : listOfLoads) {
                                    // System.out.println(unit.getJavaSourceStartLineNumber() + ":" + unit + " " +
                                    // l.getName());
                                    if (reduds.contains(l)) {
                                        String printout = "\n" + unit.getJavaSourceStartLineNumber() + ":" + rValue
                                                + " "
                                                + l.getName();
                                        result.put(method_print_out,
                                                result.getOrDefault(method_print_out, "").concat(printout));
                                        break;
                                    }
                                }

                            }

                        } else {
                            // System.out.println("Meow! should be local");
                        }
                    }
                }
            }
        }
    }

    public void printResults() {
        List<String> keys = new ArrayList<>(result.keySet());

        Collections.sort(keys);

        for (String key : keys) {
            System.out.print(key);
            System.out.println(result.getOrDefault(key, ""));
        }
    }
}
