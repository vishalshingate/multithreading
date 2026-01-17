# Observer Design Pattern

The **Observer Pattern** defines a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically.

It is a **Behavioral Design Pattern**.

### Core Components
1.  **Subject (Publisher):** maintain a list of observers and provides methods to add/remove them. It notifies them when state changes.
2.  **Observer (Subscriber):** Defines an interface (`update()`) that receives notifications from the Subject.
3.  **Concrete Subject:** The actual class with the interesting state (e.g., `YouTubeChannel`).
4.  **Concrete Observer:** The class that reacts to changes (e.g., `User`, `AppNotificationService`).

---

## 1. Visual Flow

```text
    [Subject: YouTubeChannel]
          |   ^
   notify |   | subscribe()
          v   |
    +-----------------+
    |                 |
[Observer 1]     [Observer 2]
 (Alice)          (Bob)
```

1.  Bob calls `subject.subscribe(Bob)`.
2.  Subject adds Bob to `List<Observer>`.
3.  Subject state changes (Video Uploaded).
4.  Subject loops through list: `observer.update()`.

---

## 2. Advantages

1.  **Loose Coupling:** The Subject doesn't know *who* the Observers are (Alice? Bob? An Email Service?). It just knows they implement the `Observer` interface. You can add new types of observers without modifying the Subject.
2.  **Open/Closed Principle:** You can introduce new subscriber classes without breaking the Publisher code.
3.  **Dynamic Relationships:** Subscribers can join or leave the system at runtime.

## 3. Disadvantages

1.  **Memory Leaks:** The Lapsed Listener Problem. If an observer forgets to unsubscribe, the Subject holds a strong reference to it, preventing Garbage Collection. (Use WeakReferences to solve this).
2.  **Ordering:** You cannot guarantee the order in which observers are notified.
3.  **Performance:** If you have 10,000 observers, notifying all of them sequentially might block the Subject. (Solution: Use Async processing).

---

## 4. Real World Use Cases

### A. GUI Event Listeners (Java Swing / JavaScript)
Every button click listener is an implementation of the Observer pattern.
```java
button.addActionListener(e -> System.out.println("Clicked!"));
```

### B. MVC Architecture
In Model-View-Controller, the **View** observes the **Model**.
*   Model changes (Data updated).
*   Model notifies View.
*   View re-renders itself.

### C. Message Queues (Kafka / RabbitMQ)
This is the distributed version of Observer.
*   **Producer:** Publishes message to Topic.
*   **Consumers:** Subscribe to Topic and react to messages independently.

### D. React / Angular State Management
State stores (Redux, RxJS) are massive implementations of the Observer pattern. Components subscribe to the Store and re-render only when data changes.

### E. Java's `PropertyChangeListener`
Java Beans use this to notify UI components when a bean property changes.

---

## 5. Push vs Pull Model

*   **Push:** Subject sends all data in the notification.
    *   `observer.update(String videoTitle)`
*   **Pull:** Subject just says "I changed", and Observer asks for details.
    *   `observer.update(Subject s) { s.getLatestVideo(); }`

---

## 6. How to Avoid Memory Leaks (Weak References)

One of the biggest risks with the Observer pattern is the **Lapsed Listener Problem**. If the Subject lives longer than the Observer, and you forget to unsubscribe, the Observer will never be garbage collected because the Subject holds a strong reference to it.

**Solution: Weak References**
Java provides `WeakReference`. If an object is *only* reachable via a WeakReference, the Garbage Collector is allowed to reclaim it.

```java
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class WeakSubject {
    // Store Observers as WeakReferences
    private List<WeakReference<Observer>> observers = new ArrayList<>();

    public void subscribe(Observer o) {
        observers.add(new WeakReference<>(o));
    }

    public void notifyObservers(String msg) {
        Iterator<WeakReference<Observer>> it = observers.iterator();
        while (it.hasNext()) {
            WeakReference<Observer> ref = it.next();
            Observer o = ref.get(); // Try to get the real object
            
            if (o != null) {
                // Observer is still alive
                o.update("Subject", msg);
            } else {
                // Observer was Garbage Collected! Remove from list.
                System.out.println("Cleaning up dead observer reference...");
                it.remove();
            }
        }
    }
}
```
