package com.designpatterns.decorator;

public class DecoratorPatternDemo {

    public static void main(String[] args) {
        // 1. Order a Simple Coffee
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " -> $" + coffee.getCost());

        // 2. Add Milk (Decorate it)
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " -> $" + coffee.getCost());

        // 3. Add Sugar (Decorate it again)
        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " -> $" + coffee.getCost());

        // 4. Another complex order directly
        Coffee myLatte = new SugarDecorator(new MilkDecorator(new SimpleCoffee()));
        System.out.println("\nLatte: " + myLatte.getDescription() + " -> $" + myLatte.getCost());
    }
}

// ==========================================
// 1. COMPONENT INTERFACE
// ==========================================
interface Coffee {
    String getDescription();
    double getCost();
}

// ==========================================
// 2. CONCRETE COMPONENT
// ==========================================
class SimpleCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "Simple Coffee";
    }

    @Override
    public double getCost() {
        return 5.0;
    }
}

// ==========================================
// 3. BASE DECORATOR
// ==========================================
// Wraps a Coffee object and implements the Coffee interface
abstract class CoffeeDecorator implements Coffee {
    protected Coffee decoratedCoffee; // The object being decorated

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription(); // Delegate
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost(); // Delegate
    }
}

// ==========================================
// 4. CONCRETE DECORATORS
// ==========================================

class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + ", Milk";
    }

    @Override
    public double getCost() {
        return super.getCost() + 1.5; // Add cost of milk
    }
}

class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + ", Sugar";
    }

    @Override
    public double getCost() {
        return super.getCost() + 0.5; // Add cost of sugar
    }
}

