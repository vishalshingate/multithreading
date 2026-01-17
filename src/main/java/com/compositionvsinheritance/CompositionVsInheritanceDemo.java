package com.compositionvsinheritance;

public class CompositionVsInheritanceDemo {

    public static void main(String[] args) {
        System.out.println("--- 1. Inheritance (Rigid) ---");
        InheritanceCar iCar = new InheritanceCar();
        iCar.drive();
        // Problem: iCar is stuck with the engine logic defined in the parent class.
        // It cannot switch to an Electric Engine easily at runtime.

        System.out.println("\n--- 2. Composition (Flexible) ---");
        // We can plug in ANY implementation of Engine
        Engine petrolEngine = new PetrolEngine();
        CompositionCar cCar = new CompositionCar(petrolEngine);
        cCar.drive();

        System.out.println("-> Switching Engine at runtime...");
        Engine electricEngine = new ElectricEngine();
        cCar.setEngine(electricEngine); // Dynamic behavior change!
        cCar.drive();
    }
}

// ==========================================
// APPROACH 1: INHERITANCE ("IS-A" Relationship)
// ==========================================
// TIGHT COUPLING: Car IS an Engine implementation (Logical flaw often seen)
class PetrolEngineParent {
    void startEngine() {
        System.out.println("Inheritance: Petrol Engine starting... Vroom!");
    }
}

class InheritanceCar extends PetrolEngineParent {
    void drive() {
        startEngine(); // Directly dependent on parent implementation
        System.out.println("Inheritance: Car is moving.");
    }
}


// ==========================================
// APPROACH 2: COMPOSITION ("HAS-A" Relationship)
// ==========================================
// LOOSE COUPLING: Car HAS an Engine
interface Engine {
    void start();
}

class PetrolEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Composition: Petrol Engine starting... Vroom!");
    }
}

class ElectricEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Composition: Electric Motor starting... Silent!");
    }
}

class CompositionCar {
    private Engine engine; // The component

    // Dependency Injection (Constructor Injection)
    public CompositionCar(Engine engine) {
        this.engine = engine;
    }

    // Setter allows changing behavior at runtime!
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    void drive() {
        engine.start(); // Delegating the work
        System.out.println("Composition: Car is moving.");
    }
}

