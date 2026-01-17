package com.designpatterns.factory;

public class AbstractFactoryDemo {

    public static void main(String[] args) {
        System.out.println("--- 1. Windows OS Detected ---");
        GUIFactory winFactory = new WindowsFactory();
        Application app1 = new Application(winFactory);
        app1.paint();

        System.out.println("\n--- 2. Mac OS Detected ---");
        GUIFactory macFactory = new MacFactory();
        Application app2 = new Application(macFactory);
        app2.paint();
    }
}

// ==========================================
// 1. ABSTRACT PRODUCTS
// ==========================================
interface Button {
    void paint();
}

interface Checkbox {
    void paint();
}

// ==========================================
// 2. CONCRETE PRODUCTS (Family 1: Windows)
// ==========================================
class WindowsButton implements Button {
    @Override
    public void paint() {
        System.out.println("Rendering a Windows style Button");
    }
}

class WindowsCheckbox implements Checkbox {
    @Override
    public void paint() {
        System.out.println("Rendering a Windows style Checkbox");
    }
}

// ==========================================
// 3. CONCRETE PRODUCTS (Family 2: Mac)
// ==========================================
class MacButton implements Button {
    @Override
    public void paint() {
        System.out.println("Rendering a Mac style Button");
    }
}

class MacCheckbox implements Checkbox {
    @Override
    public void paint() {
        System.out.println("Rendering a Mac style Checkbox");
    }
}

// ==========================================
// 4. ABSTRACT FACTORY
// ==========================================
interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

// ==========================================
// 5. CONCRETE FACTORIES
// ==========================================
class WindowsFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}

class MacFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}

// ==========================================
// 6. CLIENT CODE
// ==========================================
class Application {
    private Button button;
    private Checkbox checkbox;

    // The client doesn't know if it's Windows or Mac.
    // It just uses the Abstract Interface.
    public Application(GUIFactory factory) {
        button = factory.createButton();
        checkbox = factory.createCheckbox();
    }

    public void paint() {
        button.paint();
        checkbox.paint();
    }
}

