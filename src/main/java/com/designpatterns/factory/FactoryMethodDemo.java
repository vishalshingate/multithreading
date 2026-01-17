package com.designpatterns.factory;

public class FactoryMethodDemo {

    public static void main(String[] args) {
        System.out.println("--- 1. Road Logistics ---");
        Logistics roadLogistics = new RoadLogistics();
        roadLogistics.planDelivery(); // internally calls createTransport() -> Truck

        System.out.println("\n--- 2. Sea Logistics ---");
        Logistics seaLogistics = new SeaLogistics();
        seaLogistics.planDelivery(); // internally calls createTransport() -> Ship
    }
}

// ==========================================
// 1. PRODUCT INTERFACE
// ==========================================
interface Transport {
    void deliver();
}

// ==========================================
// 2. CONCRETE PRODUCTS
// ==========================================
class Truck implements Transport {
    @Override
    public void deliver() {
        System.out.println("Delivering by land in a box.");
    }
}

class Ship implements Transport {
    @Override
    public void deliver() {
        System.out.println("Delivering by sea in a container.");
    }
}

// ==========================================
// 3. CREATOR (Abstract Factory Method)
// ==========================================
abstract class Logistics {

    // THE FACTORY METHOD
    // We don't know what Transport we are creating yet. Subclasses decide.
    public abstract Transport createTransport();

    public void planDelivery() {
        // Call the factory method to create a Product object.
        Transport t = createTransport();

        // Use the product
        t.deliver();
    }
}

// ==========================================
// 4. CONCRETE CREATORS
// ==========================================
class RoadLogistics extends Logistics {
    @Override
    public Transport createTransport() {
        return new Truck();
    }
}

class SeaLogistics extends Logistics {
    @Override
    public Transport createTransport() {
        return new Ship();
    }
}

