package com.designpatterns.observer;

import java.util.ArrayList;
import java.util.List;

public class ObserverPatternDemo {

    public static void main(String[] args) {
        // 1. Create the Subject (The Publisher)
        YouTubeChannel channel = new YouTubeChannel("TechWithJava");

        // 2. Create Observers (The Subscribers)
        Subscriber s1 = new Subscriber("Alice");
        Subscriber s2 = new Subscriber("Bob");
        Subscriber s3 = new Subscriber("Charlie");

        // 3. Register Subscribers
        channel.subscribe(s1);
        channel.subscribe(s2);

        // 4. Action: New Video Upload
        System.out.println("--- Action 1: Uploading Video 1 ---");
        channel.uploadVideo("Java 21 Features");

        // 5. Dynamic Change: Add/Remove Subscribers
        System.out.println("\n--- Action 2: Charlie subscribes, Bob Unsubscribes ---");
        channel.subscribe(s3);
        channel.unsubscribe(s2);

        // 6. Action: New Video Upload
        channel.uploadVideo("Observer Pattern Explained");
    }
}

// ==========================================
// INTERFACES
// ==========================================

// Observer Interface (Subscriber)
interface Observer {
    void update(String channelName, String videoTitle);
}

// Subject Interface (The Channel)
interface Subject {
    void subscribe(Observer o);
    void unsubscribe(Observer o);
    void notifyObservers(String videoTitle);
}

// ==========================================
// CONCRETE IMPLEMENTATIONS
// ==========================================

class YouTubeChannel implements Subject {
    private String channelName;
    private List<Observer> subscribers = new ArrayList<>();

    public YouTubeChannel(String name) {
        this.channelName = name;
    }

    @Override
    public void subscribe(Observer o) {
        subscribers.add(o);
        System.out.println("New subscriber added!");
    }

    @Override
    public void unsubscribe(Observer o) {
        subscribers.remove(o);
        System.out.println("Subscriber removed.");
    }

    // This is the core logic: Iterate and Notify
    @Override
    public void notifyObservers(String videoTitle) {
        for (Observer o : subscribers) {
            o.update(this.channelName, videoTitle);
        }
    }

    // Business Logic that triggers the notification
    public void uploadVideo(String title) {
        System.out.println("Channel '" + channelName + "' is uploading: " + title);
        notifyObservers(title);
    }
}

class Subscriber implements Observer {
    private String name;

    public Subscriber(String name) {
        this.name = name;
    }

    @Override
    public void update(String channelName, String videoTitle) {
        System.out.println("   [Notification] Hey " + name + ", " + channelName + " uploaded: " + videoTitle);
    }
}

