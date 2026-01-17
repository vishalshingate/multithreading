package com.designpatterns.builder;

public class BuilderPatternDemo {

    public static void main(String[] args) {
        // 1. Building a basic computer (Required parameters only)
        // Note: We cannot do this easily if we had a constructor with 5 nulls.
        Computer basicPc = new Computer.ComputerBuilder("Intel i5", "8GB").build();
        System.out.println("Basic PC: " + basicPc);

        // 2. Building a Gaming PC (With Graphics Card and more storage)
        // Fluent Interface (Chaining methods)
        Computer gamingPc = new Computer.ComputerBuilder("Intel i9", "32GB")
                .setGraphicsCard("NVIDIA RTX 4090")
                .setStorage("2TB NVMe SSD")
                .setBluetoothEnabled(true)
                .build();

        System.out.println("Gaming PC: " + gamingPc);
    }
}

// ==========================================
// THE COMPLEX OBJECT
// ==========================================
class Computer {
    // Required parameters
    private String CPU;
    private String RAM;

    // Optional parameters
    private String graphicsCard;
    private String storage;
    private boolean isBluetoothEnabled;
    private boolean isWiFiEnabled;

    // Private Constructor: Forces usage of the Builder
    private Computer(ComputerBuilder builder) {
        this.CPU = builder.CPU;
        this.RAM = builder.RAM;
        this.graphicsCard = builder.graphicsCard;
        this.storage = builder.storage;
        this.isBluetoothEnabled = builder.isBluetoothEnabled;
        this.isWiFiEnabled = builder.isWiFiEnabled;
    }

    @Override
    public String toString() {
        return "Computer [CPU=" + CPU + ", RAM=" + RAM +
               ", GPU=" + (graphicsCard != null ? graphicsCard : "Integrated") +
               ", Storage=" + (storage != null ? storage : "256GB HDD") + "]";
    }

    // ==========================================
    // THE BUILDER CLASS
    // ==========================================
    public static class ComputerBuilder {
        // Required
        private String CPU;
        private String RAM;

        // Optional
        private String graphicsCard;
        private String storage;
        private boolean isBluetoothEnabled;
        private boolean isWiFiEnabled;

        // Constructor for Required Parameters
        public ComputerBuilder(String CPU, String RAM) {
            this.CPU = CPU;
            this.RAM = RAM;
        }

        // Setter methods for Optional Parameters
        // They return 'this' to allow Method Chaining (Fluent Design)

        public ComputerBuilder setGraphicsCard(String graphicsCard) {
            this.graphicsCard = graphicsCard;
            return this;
        }

        public ComputerBuilder setStorage(String storage) {
            this.storage = storage;
            return this;
        }

        public ComputerBuilder setBluetoothEnabled(boolean isBluetoothEnabled) {
            this.isBluetoothEnabled = isBluetoothEnabled;
            return this;
        }

        public ComputerBuilder setWiFiEnabled(boolean isWiFiEnabled) {
            this.isWiFiEnabled = isWiFiEnabled;
            return this;
        }

        // The method that actually constructs the final object
        public Computer build() {
            return new Computer(this);
        }
    }
}

