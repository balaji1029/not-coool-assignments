import java.util.*;

import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ForwardFlowAnalysis;

class HeapObject {
    Type type;
    int lineNumber;
    boolean isMarked;
    SortedSet<Integer> lineNumbers = new TreeSet<>();

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

    public void addLineNumber(int num) {
        lineNumbers.add(num);
    }

    public void addLineNumbers(HeapObject other) {
        lineNumbers.addAll(other.lineNumbers);
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
        if (lineNumber == 0) {
            return System.identityHashCode(this);
        }
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

        if (lineNumber == 0)
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
    // int count = 0;

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
        // HeapObject newMarkedObject = new HeapObject(obj.type, obj.lineNumber, true);
        // for (Map.Entry<Local, Set<HeapObject>> e : stackVars.entrySet()) {
        // if (e.getValue().contains(obj)) {
        // e.getValue().remove(obj);
        // e.getValue().add(newMarkedObject);
        // }
        // }

        // Map<HeapObject, Map<SootField, Set<HeapObject>>> newHeapVars = new
        // HashMap<>();
        // for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e :
        // heapVars.entrySet()) {
        // if (e.getKey().equals(obj)) {
        // newHeapVars.put(newMarkedObject, new HashMap<>());
        // for (Map.Entry<SootField, Set<HeapObject>> mapEntries :
        // heapVars.get(e.getKey()).entrySet()) {
        // newHeapVars.get(newMarkedObject).put(mapEntries.getKey(), new
        // HashSet<>(mapEntries.getValue()));
        // }
        // } else {
        // newHeapVars.put(e.getKey(), new HashMap<>());
        // for (Map.Entry<SootField, Set<HeapObject>> mapEntries :
        // e.getValue().entrySet()) {
        // newHeapVars.get(e.getKey()).put(mapEntries.getKey(), new
        // HashSet<>(mapEntries.getValue()));
        // }
        // }
        // }

        // for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e :
        // newHeapVars.entrySet()) {
        // for (Map.Entry<SootField, Set<HeapObject>> mapEntry :
        // e.getValue().entrySet()) {
        // if (mapEntry.getValue().contains(obj)) {
        // mapEntry.getValue().remove(obj);
        // mapEntry.getValue().add(newMarkedObject);
        // }
        // }
        // }

        // heapVars = newHeapVars;
        obj.mark();
        return obj;
        // return newMarkedObject;
    }

    public void mark(HeapObject obj) {
        if (obj.isMarked)
            return;
        obj.mark();
        // count++;
        // System.out.println("Marking " + obj);
        // HeapObject newMarkedObject = mark_object(obj);
        // List<HeapObject> toMark = new ArrayList<>();
        for (Map.Entry<SootField, Set<HeapObject>> e : heapVars.computeIfAbsent(obj, k -> new HashMap<>())
                .entrySet()) {
            for (HeapObject childObject : e.getValue()) {
                mark(childObject);
                // toMark.add(childObject);
            }
        }
        // for (HeapObject o : toMark) {
        // mark(o);
        // }
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

        // params = new ArrayList<>(src.params);

        // paramMap = new HashMap<>();
        // for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> e :
        // src.paramMap.entrySet()) {
        // Map<SootField, Set<HeapObject>> fieldMap = new HashMap<>();
        // for (Map.Entry<SootField, Set<HeapObject>> h : e.getValue().entrySet()) {
        // fieldMap.put(h.getKey(), new HashSet<>(h.getValue()));
        // }
        // }
    }

    public void union(PointsToGraph src) {
        // While unioning, if a heapobject field is pointing to more than one
        // object, mark all of them and if a marked object is being allocated another
        // object, mark it and if a local is being allocated more than one objects, mark
        // them - Not required ig
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

        // if (count != other.count)
        //     return false;

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
    public static Map<SootMethod, PointerAnalysis> methodAnalysis = new HashMap<>();
    static CallGraph cg;
    Body body;
    List<Local> params;
    Set<Local> locals;
    int firstLineNumber;
    List<HeapObject> paramObjs = new ArrayList<>();
    Set<HeapObject> paramChildren = new HashSet<>();
    Map<HeapObject, Map<SootField, HeapObject>> paramMap = new HashMap<>();

    public PointerAnalysis(Body body) {
        super(new BriefUnitGraph(body));
        this.body = body;
        params = new ArrayList<>();
        if (!body.getMethod().isStatic()) {
            params.add(body.getThisLocal());
        }
        params.addAll(body.getParameterLocals());
        locals = new HashSet<>();
        locals.addAll(body.getLocals());
        firstLineNumber = body.getMethod().getJavaSourceStartLineNumber();
        // System.out.println(body.getMethod());
        // System.out.println(firstLineNumber);
        doAnalysis();
        endFlow();
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
            HeapObject obj = new HeapObject(type, 0);
            Set<HeapObject> set = new HashSet<>();
            set.add(obj);
            init.stackVars.put(param, set);
            paramObjs.add(obj);
            paramChildren.add(obj);
        }

        // System.out.println("Processing " + body.getMethod());
        // System.out.println("Param size: " + paramObjs.size());

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

    protected void process(PointsToGraph out, HeapObject left, HeapObject right,
            Map<HeapObject, Map<SootField, HeapObject>> paramMap2) {
        // System.out.println("Processing");
        // System.out.println(left);
        // System.out.println(right);
        left.addLineNumbers(right);
        if (right.isMarked) {
            out.mark(left);
        } else {
            Map<SootField, HeapObject> paramObjects = paramMap2.getOrDefault(right, new HashMap<>());
            // for (Map.Entry<SootField, Set<HeapObject>> fieldMap : heapObjects.entrySet())
            // {
            // if (fieldMap.getValue().size() > 1) {
            // for (HeapObject obj : fieldMap.getValue()) {
            // out.mark(obj);
            // }
            // } else {
            // var obj = fieldMap.getValue().iterator().next();
            // if (obj.isMarked) {
            // continue;
            // } else {
            // if (paramMap.getOrDefault(right, new
            // HashMap<>()).containsKey(fieldMap.getKey())) {
            // process(out, obj, paramMap.get(right).get(fieldMap.getKey()), paramMap);
            // }
            // }
            // }
            // }

            for (Map.Entry<SootField, HeapObject> field : paramObjects.entrySet()) {
                Map<SootField, Set<HeapObject>> leftFieldMap = out.heapVars.computeIfAbsent(left, k -> new HashMap<>());
                // System.out.println("yo");
                // System.out.println(field.getKey());
                // System.out.println(field.getValue());
                if (leftFieldMap.getOrDefault(field.getKey(), new HashSet<>()).size() > 1) {
                    for (HeapObject obj : leftFieldMap.get(field.getKey())) {
                        out.mark(obj);
                    }
                } else if (leftFieldMap.getOrDefault(field.getKey(), new HashSet<>()).size() == 1) {
                    var obj = leftFieldMap.get(field.getKey()).iterator().next();
                    if (obj.isMarked) {
                        continue;
                    } else {
                        process(out, obj, paramMap2.get(right).get(field.getKey()), paramMap2);
                    }
                } else {
                    // System.out.println("here3");
                    var dummyObject = new HeapObject(field.getKey().getType(), 0, false);
                    Set<HeapObject> newPointedTo = new HashSet<>();

                    if (paramChildren.contains(left)) {
                        // System.out.println("here2");
                        // Get the pointedTo map in the paramMap
                        Map<SootField, HeapObject> pointedTo = this.paramMap.computeIfAbsent(left, k -> new HashMap<>());

                        // If the pointedTo doesn't contain the field, add the new dummy object to the
                        // map and the paramChildren
                        if (!pointedTo.containsKey(field.getKey())) {
                            if (left.isMarked)
                                out.mark(dummyObject);
                            pointedTo.put(field.getKey(), dummyObject);
                            paramChildren.add(dummyObject);
                            // System.out.println(paramChildren);
                        }
                        // System.out.println(paramChildren);
                        // System.out.println(paramMap);

                        // Add this object either from this dummy object or other branch into the out
                        newPointedTo.add(pointedTo.get(field.getKey()));
                        leftFieldMap.put(field.getKey(), newPointedTo);
                    } else {
                        newPointedTo.add(dummyObject);
                        leftFieldMap.put(field.getKey(), newPointedTo);
                    }
                    process(out, dummyObject, paramMap2.get(right).get(field.getKey()), paramMap2);
                }
            }
        }
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

                if (rValue instanceof Local) { // Copy Like a = b; Don't have to do anything special
                    Local rLocal = (Local) rValue;

                    // I am taking all the heap objects that rLocal can point to and adding a
                    // points-to to lLocal - String Update
                    // Don't have to take care of new HashSet anyway
                    // Will give an error if rLocal is uninitialized, so fine

                    Set<HeapObject> stackObjs = out.stackVars.getOrDefault(rLocal, new HashSet<HeapObject>());

                    if (stackObjs.size() > 1) {
                        for (var sObj : stackObjs) {
                            out.mark(sObj);
                        }
                    }

                    out.stackVars.put(lLocal,
                            new HashSet<>(out.stackVars.computeIfAbsent(rLocal, k -> new HashSet<HeapObject>())));

                } else if (rValue instanceof JNewExpr) { // Allocation a = new A(); Nothing I can think of
                    JNewExpr rNewExpr = (JNewExpr) rValue;

                    // I am creating a new allocation site and adding a stack connection to the
                    // lLocal

                    HeapObject obj = new HeapObject(rNewExpr.getType(), unit.getJavaSourceStartLineNumber(), false);
                    // System.out.println("obj_" + unit.getJavaSourceStartLineNumber() + ": " +
                    // obj.isMarked);
                    out.stackVars.put(lLocal, new HashSet<>());
                    out.stackVars.get(lLocal).add(obj);

                } else if (rValue instanceof JInstanceFieldRef) { // Load Statement a = b.f
                    // Have to check if the b.f exists, otherwise create an object and put it
                    // in the paramMap and put it on the heapStack or whatever
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
                            // Add a dummy marked object to the field, that should be fine, but mark the
                            // objects being assigned too

                            out.stackVars.put(lLocal, Set
                                    .of(new HeapObject(rField.getType(), unit.getJavaSourceStartLineNumber(), true)));
                        } else {
                            Set<HeapObject> newSet = new HashSet<>();
                            var heapObjects = out.stackVars.computeIfAbsent(rLocalBase, k -> new HashSet<HeapObject>());
                            if (heapObjects.size() > 1) {
                                for (var hObject : heapObjects) {
                                    out.mark(hObject);
                                }
                            }

                            for (HeapObject hObject : heapObjects) {
                                if (hObject.isMarked) {
                                    // Think about this when an instance ref is done on a marked object
                                    newSet.add(new HeapObject(rField.getType(), unit.getJavaSourceStartLineNumber(),
                                            true));
                                    continue;
                                }
                                // Add ability to add to the param listnull
                                var fieldMap = out.heapVars.computeIfAbsent(hObject,
                                        k -> new HashMap<SootField, Set<HeapObject>>());

                                // // If b.f is pointing to nothing and b
                                if (fieldMap.getOrDefault(rField, new HashSet<>()).size() == 0) {
                                    // Make a dummy object
                                    var dummyObject = new HeapObject(rField.getType(), 0, false);
                                    Set<HeapObject> newPointedTo = new HashSet<>();

                                    if (paramChildren.contains(hObject)) {
                                        // System.out.println("here2");
                                        // Get the pointedTo map in the paramMap
                                        Map<SootField, HeapObject> pointedTo = paramMap.computeIfAbsent(hObject,
                                                k -> new HashMap<>());

                                        // If the pointedTo doesn't contain the field, add the new dummy object to the
                                        // map and the paramChildren
                                        if (!pointedTo.containsKey(rField)) {
                                            if (hObject.isMarked)
                                                out.mark(dummyObject);
                                            pointedTo.put(rField, dummyObject);
                                            paramChildren.add(dummyObject);
                                        }
                                        // System.out.println(paramChildren);
                                        // System.out.println(paramMap);

                                        // Add this object either from this dummy object or other branch into the out
                                        newPointedTo.add(pointedTo.get(rField));
                                        fieldMap.put(rField, newPointedTo);
                                    } else {
                                        newPointedTo.add(dummyObject);
                                        fieldMap.put(rField, newPointedTo);
                                    }
                                }
                                newSet.addAll(fieldMap.get(rField));
                            }
                            out.stackVars.put(lLocal, newSet);
                        }
                    }
                } else if (rValue instanceof InvokeExpr) {
                    // Invoke expr and compare the params
                    InvokeExpr rInvokeExpr = (InvokeExpr) rValue;

                    out.stackVars.put(lLocal,
                            Set.of(new HeapObject(lLocal.getType(), unit.getJavaSourceStartLineNumber(), true)));

                    int i = 0;

                    Iterator<Edge> targets = cg.edgesOutOf(unit);

                    List<SootMethod> methodsPossible = new ArrayList<>();

                    while (targets.hasNext()) {
                        Edge edge = targets.next();
                        SootMethod targetMethod = edge.tgt();
                        if (!targetMethod.isConcrete() || targetMethod.isJavaLibraryMethod())
                            continue;
                        methodsPossible.add(targetMethod);
                    }

                    if (rInvokeExpr instanceof JVirtualInvokeExpr) {
                        JVirtualInvokeExpr rVirtualInvokeExpr = (JVirtualInvokeExpr) rInvokeExpr;

                        Local rBase = (Local) rVirtualInvokeExpr.getBase();

                        var heapObjects = out.stackVars.get(rBase);

                        if (heapObjects.size() > 1) {
                            for (var obj : heapObjects) {
                                out.mark(obj);
                            }
                        } else {
                            var obj = heapObjects.iterator().next();
                            for (var method : methodsPossible) {
                                if (method.isConcrete()
                                        && !method.isJavaLibraryMethod()) {
                                    obj.addLineNumber(unit.getJavaSourceStartLineNumber());
                                    process(out, obj, methodAnalysis.get(method).paramObjs.get(i),
                                            methodAnalysis.get(method).paramMap);
                                } else {
                                    out.mark(obj);
                                    break;
                                }
                                if (obj.isMarked) {
                                    break;
                                }
                            }
                        }
                        i++;
                    }

                    for (Value arg : rInvokeExpr.getArgs()) {
                        if (arg instanceof Local) {
                            Local argLocal = (Local) arg;

                            var heapObjects = out.stackVars.get(argLocal);

                            if (heapObjects.size() > 1) {
                                for (var obj : heapObjects) {
                                    out.mark(obj);
                                }
                            } else {
                                var obj = heapObjects.iterator().next();
                                for (var method : methodsPossible) {
                                    if (method.isConcrete()
                                            && !method.isJavaLibraryMethod()) {
                                        obj.addLineNumber(unit.getJavaSourceStartLineNumber());
                                        process(out, obj, methodAnalysis.get(method).paramObjs.get(i),
                                                methodAnalysis.get(method).paramMap);
                                    } else {
                                        out.mark(obj);
                                        break;
                                    }
                                    if (obj.isMarked) {
                                        break;
                                    }
                                }
                                i++;
                            }
                        }
                    }
                }

            } else if (lValue instanceof JInstanceFieldRef) { // Like a.f = b;
                // Have to check if there is only one object being pointed to by a and
                // mark accordingly and then normal update

                JInstanceFieldRef lInstanceFieldRef = (JInstanceFieldRef) lValue;
                Value lBase = lInstanceFieldRef.getBase();
                SootField lField = lInstanceFieldRef.getField();

                // System.out.println(unit.getJavaSourceStartLineNumber());

                // System.out.println(rValue.getClass());

                if (rValue instanceof Local) { // Store Statement
                    Local rLocal = (Local) rValue;
                    Set<HeapObject> rObjects = out.stackVars.getOrDefault(rLocal, new HashSet<>());

                    if (rObjects.size() > 1) {
                        for (var rObj : rObjects) {
                            out.mark(rObj);
                        }
                    }

                    if (!(lBase instanceof Local)) {
                        // System.out.println("Meow! got a base as local");
                    } else {
                        Local lLocalBase = (Local) lBase;
                        Set<HeapObject> heapObjects = new HashSet<>(out.stackVars.computeIfAbsent(lLocalBase,
                                k -> new HashSet<HeapObject>())); // Can't be empty
                        if (heapObjects.size() == 1) { // Strong Update when you know you have only one point that
                                                       // lLocalBase points to
                            HeapObject heapObject = heapObjects.iterator().next();

                            // System.out.println(unit.getJavaSourceStartLineNumber());
                            // System.out.println(heapObject);

                            if (heapObject.lineNumber <= firstLineNumber)
                                out.mark(heapObject);
                            // If object is marked, mark the objects it is being forced to hold
                            if (heapObject.isMarked || lField.isStatic()) {
                                for (var rObj : rObjects) {
                                    out.mark(rObj);
                                }
                            }

                            // System.out.println(out);

                            out.heapVars.computeIfAbsent(heapObject, k -> new HashMap<>()).put(lField,
                                    new HashSet<>(rObjects)); // Stack
                                                              // variables
                                                              // can't
                                                              // be
                                                              // uninitialized
                        } else {
                            // Weak Update when there are more Heap Objects that rLocalBase is pointing to
                            for (HeapObject heapObject : heapObjects) {
                                out.mark(heapObject);
                                // If there is no object heapObject.f is pointing to and the heapObject is in
                                // the parameter map, then add an object in the map
                                Map<SootField, Set<HeapObject>> fieldMap = out.heapVars.computeIfAbsent(heapObject,
                                        k -> new HashMap<>());

                                // If b.f is pointing to nothing and b
                                if (fieldMap.getOrDefault(lField, new HashSet<>()).size() == 0) {
                                    // Make a dummy object
                                    var dummyObject = new HeapObject(lField.getType(), 0, false);
                                    Set<HeapObject> newPointedTo = new HashSet<>();

                                    if (paramChildren.contains(heapObject)) {
                                        // Get the pointedTo map in the paramMap
                                        Map<SootField, HeapObject> pointedTo = paramMap.computeIfAbsent(heapObject,
                                                k -> new HashMap<>());

                                        // If the pointedTo doesn't contain the field, add the new dummy object to the
                                        // map and the paramChildren
                                        if (!pointedTo.containsKey(lField)) {
                                            if (heapObject.isMarked)
                                                out.mark(dummyObject);
                                            pointedTo.put(lField, dummyObject);
                                            paramChildren.add(dummyObject);
                                        }

                                        // Add this object either from this dummy object or other branch into the out
                                        newPointedTo.add(pointedTo.get(lField));
                                    } else {
                                        newPointedTo.add(dummyObject);
                                    }
                                    fieldMap.put(lField, newPointedTo);

                                }

                                // Add all the objects into the lField map
                                fieldMap.computeIfAbsent(lField, k -> new HashSet<>()).addAll(rObjects);
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

                    // TODO: Edit this case to match the other if branch

                    // I am creating a new allocation site and adding a stack connection to the
                    // lLocal

                    HeapObject obj = new HeapObject(rConstant.getType(), unit.getJavaSourceStartLineNumber());

                    if (!(lBase instanceof Local)) {
                        // System.out.println("Meow! got a base as local");
                    } else {
                        Local lLocalBase = (Local) lBase;
                        Set<HeapObject> heapObjects = new HashSet<>(out.stackVars.computeIfAbsent(lLocalBase,
                                k -> new HashSet<HeapObject>())); // Can't be empty
                        if (heapObjects.size() == 1) { // Strong Update when you know you have only one point that
                                                       // lLocalBase points to
                            HeapObject heapObject = heapObjects.iterator().next();

                            // If object is marked, mark the objects it is being forced to hold

                            if (heapObject.lineNumber <= firstLineNumber) {
                                // System.out.println(unit.getJavaSourceStartLineNumber());
                                // System.out.println(heapObject);
                                // System.out.println("Marked");
                                out.mark(heapObject);
                                // System.out.println(paramMap);
                            }

                            Set<HeapObject> objSet = new HashSet<>();
                            objSet.add(obj);
                            out.heapVars.computeIfAbsent(heapObject, k -> new HashMap<>()).put(lField, objSet);
                        } else {
                            // Weak Update when there are more Heap Objects that rLocalBase is pointing to
                            for (HeapObject heapObject : heapObjects) {
                                out.mark(heapObject);
                                // If there is no object heapObject.f is pointing to and the heapObject is in
                                // the parameter map, then add an object in the map
                                Map<SootField, Set<HeapObject>> fieldMap = out.heapVars.computeIfAbsent(heapObject,
                                        k -> new HashMap<>());

                                // If b.f is pointing to nothing and b
                                if (fieldMap.getOrDefault(lField, new HashSet<>()).size() == 0) {
                                    // Make a dummy object
                                    var dummyObject = new HeapObject(lField.getType(), 0, false);
                                    Set<HeapObject> newPointedTo = new HashSet<>();

                                    if (paramChildren.contains(heapObject)) {

                                        // Get the pointedTo map in the paramMap
                                        Map<SootField, HeapObject> pointedTo = paramMap.computeIfAbsent(heapObject,
                                                k -> new HashMap<>());

                                        // If the pointedTo doesn't contain the field, add the new dummy object to the
                                        // map and the paramChildren
                                        if (!pointedTo.containsKey(lField)) {
                                            if (heapObject.isMarked)
                                                out.mark(dummyObject);
                                            pointedTo.put(lField, dummyObject);
                                            paramChildren.add(dummyObject);
                                        }

                                        // Add this object either from this dummy object or other branch into the out
                                        newPointedTo.add(pointedTo.get(lField));
                                    } else {
                                        newPointedTo.add(dummyObject);
                                    }

                                    fieldMap.put(lField, newPointedTo);
                                }

                                // Add all the objects into the lField map
                                fieldMap.computeIfAbsent(lField, k -> new HashSet<>()).add(obj);
                            }
                            if (lField.isStatic()) {
                                for (HeapObject heapObject : heapObjects) {
                                    out.mark(heapObject);
                                }
                            }
                        }
                    }
                }
            } else if (lValue instanceof StaticFieldRef) {
                StaticFieldRef lStat = (StaticFieldRef) lValue;
                if (rValue instanceof Local) {
                    Local rLocal = (Local) rValue;

                    var objs = out.stackVars.get(rLocal);

                    for (var obj : objs) {
                        out.mark(obj);
                    }
                }
            }
        } else if (unit instanceof JInvokeStmt) {
            // TODO: Same thing as the a = f(b, c)
            JInvokeStmt invokeStmt = (JInvokeStmt) unit;

            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            Iterator<Edge> targets = cg.edgesOutOf(invokeStmt);

            List<SootMethod> methodsPossible = new ArrayList<>();

            while (targets.hasNext()) {
                Edge edge = targets.next();
                SootMethod targetMethod = edge.tgt();
                if (!targetMethod.isConcrete() || targetMethod.isJavaLibraryMethod())
                    continue;
                methodsPossible.add(targetMethod);
            }

            if (!invokeExpr.getMethod().isJavaLibraryMethod()) {
                int i = 0;
                if (invokeExpr instanceof JVirtualInvokeExpr) {
                    JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) invokeExpr;

                    Value base = virtualInvokeExpr.getBase();

                    Local baseLocal = (Local) base;

                    // System.out.println("Function call at : " +
                    // unit.getJavaSourceStartLineNumber());

                    // for (HeapObject argObject : out.stackVars.computeIfAbsent(baseLocal, k -> new
                    // HashSet<>())) {
                    // out.mark(argObject);
                    // }

                    var heapObjects = out.stackVars.get(baseLocal);

                    if (heapObjects.size() > 1) {
                        for (var obj : heapObjects) {
                            out.mark(obj);
                        }
                    } else {
                        var obj = heapObjects.iterator().next();
                        obj.addLineNumber(unit.getJavaSourceStartLineNumber());
                        for (var method : methodsPossible) {
                            if (method.isConcrete()
                                    && !method.isJavaLibraryMethod()) {
                                // System.out.println("here");
                                // System.out.println(methodAnalysis.get(method).paramChildren);
                                // System.out.println(methodAnalysis.get(method).paramMap);
                                process(out, obj, methodAnalysis.get(method).paramObjs.get(i),
                                        methodAnalysis.get(method).paramMap);
                            } else {
                                out.mark(obj);
                                break;
                            }
                            if (obj.isMarked) {
                                break;
                            }
                        }
                    }
                    i++;
                } else if (invokeExpr instanceof JSpecialInvokeExpr) {
                    JSpecialInvokeExpr specialInvokeExpr = (JSpecialInvokeExpr) invokeExpr;

                    Value base = specialInvokeExpr.getBase();

                    Local baseLocal = (Local) base;

                    // System.out.println("Function call at : " +
                    // unit.getJavaSourceStartLineNumber());

                    // for (HeapObject argObject : out.stackVars.computeIfAbsent(baseLocal, k -> new
                    // HashSet<>())) {
                    // out.mark(argObject);
                    // }

                    var heapObjects = out.stackVars.get(baseLocal);

                    if (heapObjects.size() > 1) {
                        for (var obj : heapObjects) {
                            out.mark(obj);
                        }
                    } else {
                        var obj = heapObjects.iterator().next();
                        for (var method : methodsPossible) {
                            // if (method.isConcrete())
                            // obj.addLineNumber(unit.getJavaSourceStartLineNumber());
                            // System.out.println(method.getDeclaringClass().getJavaSourceStartLineNumber());
                            // System.out.println(method.getJavaSourceStartLineNumber());
                            // if (method.getDeclaringClass().getJavaSourceStartLineNumber() !=
                            // method.getJavaSourceStartLineNumber())
                            // obj.addLineNumber(unit.getJavaSourceStartLineNumber());
                            if (method.isConcrete()
                                    && !method.isJavaLibraryMethod()) {
                                process(out, obj, methodAnalysis.get(method).paramObjs.get(i),
                                        methodAnalysis.get(method).paramMap);
                            } else {
                                out.mark(obj);
                                break;
                            }
                            if (obj.isMarked) {
                                break;
                            }
                        }
                    }
                    i++;
                }

                // Marks everything iteratively
                // List<HeapObject> toMark = new ArrayList<>();
                // System.out.println(invokeExpr.getArgCount());
                for (int j = 0; j < invokeExpr.getArgCount(); j++) {
                    // System.out.println(unit.getJavaSourceStartLineNumber());

                    Value arg = invokeExpr.getArg(j);
                    // System.out.println(arg.getClass());
                    if (arg instanceof Local) {
                        Local argLocal = (Local) arg;
                        var heapObjects = out.stackVars.get(argLocal);

                        if (heapObjects.size() > 1) {
                            for (var obj : heapObjects) {
                                out.mark(obj);
                            }
                        } else {
                            var obj = heapObjects.iterator().next();
                            obj.addLineNumber(unit.getJavaSourceStartLineNumber());
                            for (var method : methodsPossible) {
                                if (method.isConcrete()
                                        && !method.isJavaLibraryMethod()) {
                                    process(out, obj, methodAnalysis.get(method).paramObjs.get(i),
                                            methodAnalysis.get(method).paramMap);
                                } else {
                                    out.mark(obj);
                                    break;
                                }
                                if (obj.isMarked) {
                                    break;
                                }
                            }
                        }
                        i++;
                    }
                }
            }

        } else if (unit instanceof JReturnStmt) {
            JReturnStmt returnStmt = (JReturnStmt) unit;

            Local returnLocal = (Local) returnStmt.getOp();

            // If the local points to two objects, both can't be scalar replaceable
            // If the object is allocated in this function, mark it
            // If the local object doesn't point doesn't point to anything, can't be
            var setOfObjs = in.stackVars.getOrDefault(returnLocal, new HashSet<>());

            if (setOfObjs.size() > 1) {
                for (var obj : setOfObjs) {
                    out.mark(obj);
                }
            } else {
                var obj = setOfObjs.iterator().next();

                if (obj.lineNumber >= firstLineNumber) {
                    out.mark(obj);
                }
            }
        }
    }

    protected void endFlow() {
        var units = body.getUnits();

        int count = 1;

        while (count > 0) {
            count = 0;
            for (var unit : units) {
                PointsToGraph out = getFlowAfter(unit);

                List<HeapObject> toMark = new ArrayList<>();

                for (Map.Entry<HeapObject, Map<SootField, Set<HeapObject>>> h : out.heapVars.entrySet()) {
                    if (h.getKey().isMarked) {
                        for (Map.Entry<SootField, Set<HeapObject>> fieldMap : h.getValue().entrySet()) {
                            for (HeapObject obj : fieldMap.getValue()) {
                                if (!obj.isMarked) {
                                    count++;
                                    // out.mark(obj);
                                    toMark.add(obj);
                                }
                            }
                        }
                    }
                }
                for (var obj : toMark) {
                    out.mark(obj);
                }
            }
        }
    }
}

public class AnalysisTransformer extends SceneTransformer {

    // List<SootMethod> methods = new ArrayList<>();
    static CallGraph cg;
    static Map<SootMethod, PointerAnalysis> analysisMap;
    SortedMap<Integer, String> printOut = new TreeMap<>();

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        // Store the call graph once as a static field...
        cg = Scene.v().getCallGraph();
        PointerAnalysis.cg = cg;

        analysisMap = new HashMap<>();

        // This code lets us get the main method, our testcases will only have one start
        // point that is the main method
        // in the Test class...
        var entrypoints = Scene.v().getEntryPoints();
        assert (entrypoints.size() == 1);
        SootMethod entryMethod = entrypoints.get(0);

        handleMainMethod(entryMethod);

        for (Map.Entry<Integer, String> p : printOut.entrySet()) {
            System.out.println(p.getValue());
        }
    }

    void handleMainMethod(SootMethod myMethod) {
        // Get the body
        Body body = myMethod.retrieveActiveBody();

        // Iterate over statements
        for (Unit u : body.getUnits()) {
            Stmt stmt = (Stmt) u;
            int lineNumber = stmt.getJavaSourceStartLineNumber();

            // If a statement contains a call expression, find and print its call targets.
            if (stmt.containsInvokeExpr()) {
                // System.out.println("Call site found: " + stmt + "@" + lineNumber);
                Iterator<Edge> targets = cg.edgesOutOf(stmt);

                while (targets.hasNext()) {
                    Edge edge = targets.next();
                    SootMethod targetMethod = edge.tgt();
                    if (!targetMethod.isConcrete() || targetMethod.isJavaLibraryMethod())
                        continue;
                    if (!analysisMap.keySet().contains(targetMethod))
                        handleMainMethod(targetMethod);
                    // if
                    // (Scene.v().getApplicationClasses().contains(targetMethod.getDeclaringClass()))
                    // for (Unit unit : targetMethod.retrieveActiveBody().getUnits()) {
                    // System.out.println(unit);
                    // }
                    // System.out.println(" -> Potential target: " + targetMethod.getSignature());
                }
            }
        }
        PointerAnalysis.methodAnalysis.put(myMethod, new PointerAnalysis(body));
        // analysisMap.put(myMethod, new PointerAnalysis(body));
        // System.out.println(myMethod);
        var units = body.getUnits();

        for (Unit unit : units) {
            // System.out.println(PointerAnalysis.methodAnalysis.get(myMethod).getFlowAfter(unit));
            if (unit instanceof JAssignStmt) {
                JAssignStmt assn = (JAssignStmt) unit;
                Value r = assn.getRightOp();
                if (r instanceof JNewExpr) {
                    PointsToGraph out = PointerAnalysis.methodAnalysis.get(myMethod).getFlowAfter(unit);

                    Value lValue = assn.getLeftOp();
                    Local lLocal = (Local) lValue;

                    if (out.stackVars.get(lLocal).iterator().next().isMarked) {
                        printOut.put(unit.getJavaSourceStartLineNumber(),
                                "O" + unit.getJavaSourceStartLineNumber() + " = " + "N");
                    } else {
                        String lineNumbers = "";
                        Set<Integer> numbers = out.stackVars.get(lLocal).iterator().next().lineNumbers;
                        // Collections.sort(numbers);
                        for (int num : numbers) {
                            lineNumbers += num + ",";
                        }
                        if (numbers.size() > 0) {
                            lineNumbers = lineNumbers.substring(0, lineNumbers.length() - 1);
                        }
                        printOut.put(unit.getJavaSourceStartLineNumber(),
                                "O" + unit.getJavaSourceStartLineNumber() + " = " + "Y" + "[" + lineNumbers + "]");

                    }

                }
            }
        }
    }
}
