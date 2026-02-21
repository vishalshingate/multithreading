# Tricky Java Code Output Questions

This document contains code-based tricky interview questions focusing on **Constructors, Inheritance, and Exception Handling**, designed to test your deep understanding of Java execution flow.

---

## 1. Init Blocks, Constructors & Static Blocks Execution Order

**Question:** What is the output of the following code?

```java
class Parent {
    static {
        System.out.println("Parent Static Block");
    }
    
    {
        System.out.println("Parent Init Block");
    }
    
    public Parent() {
        System.out.println("Parent Constructor");
    }
}

class Child extends Parent {
    static {
        System.out.println("Child Static Block");
    }
    
    {
        System.out.println("Child Init Block");
    }
    
    public Child() {
        System.out.println("Child Constructor");
    }
}

public class Test {
    public static void main(String[] args) {
        new Child();
    }
}
```

**Answer:**
```
Parent Static Block
Child Static Block
Parent Init Block
Parent Constructor
Child Init Block
Child Constructor
```
**Explanation:**
1.  **Static Blocks** run first when the class is loaded, starting from the parent class down to the child class.
2.  **Instance Initialization Blocks** run before the constructor of the class.
3.  **Constructors** run last, but the `super()` call (implicit or explicit) ensures the parent constructor completes before the child constructor body executes.

---

## 2. Calling Overridden Methods from Constructor (Very Tricky!)

**Question:** What will be printed?

```java
class Parent {
    public Parent() {
        print();
    }
    
    void print() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
    int x = 10;
    
    void print() {
        System.out.println("Child: " + x);
    }
}

public class Test {
    public static void main(String[] args) {
        new Child();
    }
}
```

**Answer:**
```
Child: 0
```
**Explanation:**
1.  `new Child()` calls `Parent` constructor first.
2.  Inside `Parent` constructor, `print()` is called.
3.  Since the object being created is an instance of `Child`, the **overridden** `print()` method in `Child` is called (Polymorphism works even during construction!).
4.  At this point, `Child`'s variables (like `int x = 10`) have **not been initialized yet**. They only get their values *after* the `super()` constructor returns.
5.  Default value of `x` is `0`, so it prints `Child: 0`.

---

## 3. Exception Handling: Finally vs Return

**Question:** What does this method return?

```java
public int test() {
    try {
        return 1;
    } catch (Exception e) {
        return 2;
    } finally {
        return 3;
    }
}
```

**Answer:**
```
3
```
**Explanation:**
The `finally` block always executes. If the `finally` block contains a `return` statement, it **overrides** any return value from the `try` or `catch` blocks. The method will return `3`.

---

## 4. Exception Handling: Return in Try, Exception in Finally

**Question:** What is the result?

```java
public static void main(String[] args) {
    System.out.println(test());
}

public static int test() {
    try {
        return 1;
    } finally {
        throw new RuntimeException("Error in finally");
    }
}
```

**Answer:**
```
Exception in thread "main" java.lang.RuntimeException: Error in finally
```
**Explanation:**
The exception thrown in the `finally` block creates an abrupt termination that discards the pending `return 1` from the try block. The method does not return; it throws the exception.

---

## 5. Exception Handling with Inheritance

**Question:** Will this code compile?

```java
import java.io.IOException;

class Parent {
    void method() throws IOException {
    }
}

class Child extends Parent {
    @Override
    void method() throws Exception {
    }
}
```

**Answer:**
**Compilation Error**.

**Explanation:**
When overriding a method, the child class method **cannot throw a broader checked exception** than the parent class method. `Exception` is broader than `IOException`. It can throw `IOException`, any subclass of `IOException`, or no exception at all, but not `Exception`.

---

## 6. Static Method Hiding vs Overriding

**Question:** What is the output?

```java
class Parent {
    static void staticMethod() {
        System.out.println("Parent Static");
    }
    
    void instanceMethod() {
        System.out.println("Parent Instance");
    }
}

class Child extends Parent {
    static void staticMethod() {
        System.out.println("Child Static");
    }
    
    @Override
    void instanceMethod() {
        System.out.println("Child Instance");
    }
}

public class Test {
    public static void main(String[] args) {
        Parent p = new Child();
        p.staticMethod();
        p.instanceMethod();
    }
}
```

**Answer:**
```
Parent Static
Child Instance
```
**Explanation:**
1.  **Static Methods** are bonded at compile time based on the **reference type** (`Parent`). This is called **Method Hiding**, not overriding. So `p.staticMethod()` calls `Parent.staticMethod()`.
2.  **Instance Methods** are bonded at runtime based on the **actual object type** (`Child`). This is **Method Overriding**. So `p.instanceMethod()` calls `Child.instanceMethod()`.

---

## 7. Try-with-resources AutoCloseable Order

**Question:** What is the order of execution?

```java
class Resource implements AutoCloseable {
    String name;
    public Resource(String name) { this.name = name; }
    public void close() { System.out.println(name + " Closed"); }
}

public class Test {
    public static void main(String[] args) {
        try (Resource r1 = new Resource("R1");
             Resource r2 = new Resource("R2")) {
            System.out.println("Inside Try");
        }
    }
}
```

**Answer:**
```
Inside Try
R2 Closed
R1 Closed
```
**Explanation:**
Resources declared in a `try-with-resources` statement are closed in the **reverse order** of their creation. `R2` was created last, so it is closed first.

---

## 8. Variable Hiding (Shadowing) in Inheritance

**Question:** What is the output?

```java
class Parent {
    String name = "Parent";
    
    void print() {
        System.out.println(name);
    }
}

class Child extends Parent {
    String name = "Child";
    
    @Override
    void print() {
        System.out.println(name);
    }
}

public class Test {
    public static void main(String[] args) {
        Parent p = new Child();
        System.out.println(p.name);
        p.print();
    }
}
```

**Answer:**
```
Parent
Child
```

**Explanation:**
1.  **Variables are NOT polymorphic** in Java. When you access `p.name`, the compiler looks at the reference type (`Parent`), so it accesses `Parent`'s variable `name`.
2.  **Methods are polymorphic**. When you call `p.print()`, the runtime looks at the actual object type (`Child`), so it calls `Child`'s `print()` method, which uses `Child`'s `name`.

---

## 9. Method Overloading with Null

**Question:** Which method will be called?

```java
public class Test {
    public void print(Object o) {
        System.out.println("Object");
    }

    public void print(String s) {
        System.out.println("String");
    }

    public static void main(String[] args) {
        new Test().print(null);
    }
}
```

**Answer:**
```
String
```

**Explanation:**
When passing `null`, the compiler tries to pick the **most specific** method type. Since `String` inherits from `Object`, `String` is more specific than `Object`. Therefore, `print(String s)` is chosen.

*Note:* If you had another method `print(Integer i)`, it would cause a compile-time `Ambiguous method call` error because neither `String` nor `Integer` is more specific than the other.

---

## 10. Overloading: Widening vs Boxing vs Varargs

**Question:** What is the output?

```java
public class Test {
    static void execute(int x) { System.out.println("Primitive int"); }
    static void execute(Integer x) { System.out.println("Wrapper Integer"); }
    static void execute(long x) { System.out.println("Primitive long"); }
    static void execute(int... x) { System.out.println("Varargs"); }

    public static void main(String[] args) {
        int num = 5;
        execute(num);
    }
}
```

**Scenario A:** If `execute(int x)` is present?
**Answer:** `Primitive int` (Exact match).

**Scenario B:** If `execute(int x)` is commented out?
**Answer:** `Primitive long` (Widening prefers over Boxing).

**Scenario C:** If `execute(int x)` and `execute(long x)` are commented out?
**Answer:** `Wrapper Integer` (Boxing).

**Scenario D:** If only `execute(int... x)` remains?
**Answer:** `Varargs` (Lowest priority).

**Priority Assumption Order:**
1.  Exact Match
2.  Widening (int -> long -> float -> double)
3.  Autoboxing (int -> Integer)
4.  Varargs (int...)

---

## 11. Interface Default Method "Diamond Problem"

**Question:** What is the output or error?

```java
interface A {
    default void hello() { System.out.println("Hello A"); }
}

interface B {
    default void hello() { System.out.println("Hello B"); }
}

class Test implements A, B {
    public static void main(String[] args) {
        new Test().hello();
    }
}
```

**Answer:**
**Compilation Error**.

**Explanation:**
The class `Test` inherits two default methods with the same signature `hello()` from `A` and `B`. This creates an ambiguity (Diamond Problem). The class **must** override the method to resolve the conflict.

**Fix:**
```java
class Test implements A, B {
    @Override
    public void hello() {
        A.super.hello(); // Or provide custom implementation
    }
}
```

---

## 12. String Interning and Equality

**Question:** What will be printed?

```java
public class StringTest {
    public static void main(String[] args) {
        String s1 = "Hello";
        String s2 = new String("Hello");
        String s3 = s2.intern();
        
        System.out.println(s1 == s2); 
        System.out.println(s1 == s3); 
    }
}
```

**Answer:**
```
false
true
```

**Explanation:**
1.  `s1`: Points to "Hello" in the **String Constant Pool**.
2.  `s2`: Points to a new object in the **Heap** (because of `new String(...)`). So `s1 == s2` is false (different memory addresses).
3.  `s3`: `intern()` returns the reference from the String Constant Pool. Since "Hello" is already there (from `s1`), `s3` points to the same address as `s1`. So `s1 == s3` is true.

---

## 13. HashMap Key Mutability

**Question:** What is the output?

```java
import java.util.HashMap;

class Key {
    int id;
    Key(int id) { this.id = id; }
    
    @Override
    public int hashCode() { return id; }
    
    @Override
    public boolean equals(Object o) {
        return this.id == ((Key)o).id;
    }
}

public class MapTest {
    public static void main(String[] args) {
        HashMap<Key, String> map = new HashMap<>();
        Key k1 = new Key(1);
        map.put(k1, "Value1");
        
        k1.id = 2; // Mutating the key!
        
        System.out.println(map.get(k1));
    }
}
```

**Answer:**
```
null
```

**Explanation:**
When `k1` is put into the map, it is placed in a bucket based on `hashCode()` (which was 1).
When we change `k1.id = 2`, the key's hashcode changes to 2.
When trying to `get(k1)`, the HashMap calculates the new hashcode (2) and looks in the wrong bucket (or doesn't find it in the original bucket because the hash check fails).
**Takeaway:** Keys in `HashMap` should be immutable.

---

## 14. Inner Class and Static Context

**Question:** Will this compile?

```java
public class Outer {
    String outerField = "Outer";

    static class StaticNested {
        void print() {
            System.out.println(outerField);
        }
    }
    
    class Inner {
        void print() {
            System.out.println(outerField);
        }
    }
}
```

**Answer:**
**Compilation Error** inside `StaticNested` class.

**Explanation:**
A `static` nested class **cannot** access non-static members (`outerField`) of the outer class directly. It behaves like a standalone class. 
The non-static `Inner` class, however, has an implicit reference to the instance of `Outer` and can access `outerField`.

---

## 15. Cheat Sheet: Method Overloading vs Overriding Rules

**Question:** Summary of rules that decide compilation errors in Overloading and Overriding.

| Rule | Method Overloading | Method Overriding |
| :--- | :--- | :--- |
| **Method Name** | Must be same | Must be same |
| **Argument List** | **Must be different** (Number, Type, or Order) | **Must be same** |
| **Return Type** | Can be same or different | Must be same or **Covariant** (Subclass type) |
| **Access Modifier** | Can be anything | Cannot be **more restrictive** (e.g., public -> protected ❌) |
| **Checked Exceptions** | Can throw any exception | Cannot throw **new or broader** checked exceptions |
| **Unchecked Exceptions**| Can throw any exception | Can throw any exception |
| **Static Methods** | Can be overloaded | Cannot be overridden (Method Hiding) |
| **Private Methods** | Can be overloaded | Cannot be overridden |
| **Binding** | Compile-time (Static Binding) | Runtime (Dynamic Binding) |

