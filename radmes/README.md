# ComparatorExample

This package demonstrates sorting strategies with Comparable vs Comparator, and implements a correct equals/hashCode for the `Employee` class.

## Employee ordering

- Natural ordering (Comparable.compareTo): currently compares by `salary` only.
- Custom comparator (Comparator.compare): compares by `lastName`, then `salary`, then `id`.

Examples:
- Natural order:
  - `Collections.sort(employees);` // uses `compareTo`
  - `employees.sort(null);`
  - `employees.stream().sorted()`
- Explicit comparator:
  - `employees.sort(new Employee());` // uses `compare(o1, o2)` defined in Employee
  - `employees.sort(Comparator.comparing(Employee::getLastName)
      .thenComparingInt(Employee::getSalary)
      .thenComparingInt(Employee::getId));`
- Descending salary:
  - `employees.sort(Comparator.comparingInt(Employee::getSalary).reversed());`

If you want natural ordering to match lastName → salary → id, update `compareTo` accordingly:

```
@Override
public int compareTo(Employee other) {
    int ln = this.lastName.compareTo(other.lastName);
    if (ln != 0) return ln;
    int sal = Integer.compare(this.salary, other.salary);
    if (sal != 0) return sal;
    return Integer.compare(this.id, other.id);
}
```

## equals and hashCode

`equals` compares: `firstName`, `lastName`, `salary`, and `id`.

`hashCode` must hash the same fields to satisfy the equals/hashCode contract:

```
@Override
public int hashCode() {
    return Objects.hash(firstName, lastName, salary, id);
}
```

Why this is correct:
- Contract: if `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` must be true.
- Using the same fields in both methods ensures consistent behavior in `HashMap`, `HashSet`, and other hash-based collections.
- Avoid mutating fields used in equality after inserting into hash-based collections; it can break lookups.

Optional alternative:
- If `id` uniquely and immutably identifies an employee, consider basing equality on `id` only to avoid issues when names or salaries change:

```
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Employee)) return false;
    Employee that = (Employee) o;
    return this.id == that.id;
}

@Override
public int hashCode() {
    return Integer.hashCode(id);
}
```

## Run the demo

Compile and run the demo class:

```
javac -d out src/ComparatorExample/Employee.java src/ComparatorExample/SortEmployeesDemo.java
java -cp out ComparatorExample.SortEmployeesDemo
```

## Interview questions (with suggested answers)

- What is the equals/hashCode contract?
  - If two objects are equal according to `equals`, they must have the same `hashCode`. The reverse isn’t required.
- Why must you override hashCode when overriding equals?
  - Hash-based collections use `hashCode` for bucket placement; inconsistent implementations cause lookups/removals to fail or duplicate entries.
- Which fields should be used in equals/hashCode?
  - Exactly the fields that define logical equality for the domain. In the current `Employee`, that’s `firstName`, `lastName`, `salary`, `id`.
- What happens if fields used in hashCode are mutated after insertion into a HashSet?
  - The object may reside in the wrong bucket; lookups/removals fail. Avoid mutating identity fields or reinsert after changes.
- Is `Objects.hash` sufficient?
  - Yes for most cases. It’s concise and correct. For performance-sensitive paths, a manual prime-based combination can be faster.
- Comparable vs Comparator—what’s the difference?
  - `Comparable` defines a class’s natural order via instance method `compareTo(this vs other)`. `Comparator` defines external comparison `compare(a, b)`; better for custom orders and lambdas.
- Why do people prefer Comparator for lambdas?
  - Lambdas typically compare two values without a receiver. `Comparator` has `compare(a, b)`, while `Comparable` is tied to `this`.
- How would you sort by salary descending?
  - `employees.sort(Comparator.comparingInt(Employee::getSalary).reversed());` or implement `compareTo` to return `Integer.compare(other.salary, this.salary)`.

