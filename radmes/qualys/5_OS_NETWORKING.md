# Qualys Interview Guide - Part 5: OS & Networking

This document covers unique questions related to **Operating Systems** and **Networking** which are specific to Qualys due to its nature as a security company.

---

## 1. Networking (Very Frequent)

### A. OSI Model Layers
1.  **Physical:** Bits, Cables, Hubs (e.g., Ethernet, Fiber).
2.  **Data Link:** Frames, MAC Addresses, Switches (e.g., ARP, VLAN).
3.  **Network:** Packets, IP Addresses, Routers (e.g., IPv4, ICMP).
4.  **Transport:** Segments, Ports, Reliability (e.g., **TCP, UDP**).
5.  **Session:** Establishing/terminating sessions (e.g., NetBIOS).
6.  **Presentation:** Encryption, Compression (e.g., SSL/TLS, JPEG).
7.  **Application:** User interface, APIs (e.g., **HTTP, SMTP, DNS**).

**Analogy:** Please Do Not Throw Sausage Pizza Away.

### B. TCP vs UDP
| Feature | TCP (Transmission Control Protocol) | UDP (User Datagram Protocol) |
| :--- | :--- | :--- |
| **Reliability** | Connection-oriented (Handshake). Guarantees delivery. | Connectionless. Fire and forget. |
| **Speed** | Slower (Overhead of Ack, Retransmission). | Faster (No overhead). |
| **Order** | Packets arrive in order (Sequencing). | May arrive out of order/lost. |
| **Use Case** | Web Browsing (HTTP), Email, File Transfer. | Video Streaming, Gaming, VoIp (DNS). |

### C. HTTP vs HTTPS
| Feature | HTTP | HTTPS (Secure) |
| :--- | :--- | :--- |
| **Port** | 80 | 443 |
| **Security** | Plain text (Sniffable). | **Encrypted** using SSL/TLS Certificate. |
| **Speed** | Faster (No handshake overhead). | Slightly Slower (Encryption overhead). |
| **Trust** | No validation of server identity. | Validates identity via Certificate Authority (CA). |

### D. Ports & Protocols
*   **20/21:** FTP (File Transfer)
*   **22:** SSH (Secure Shell)
*   **23:** Telnet (Insecure remote)
*   **25:** SMTP (Email Sending)
*   **53:** DNS (Domain Name)
*   **80:** HTTP
*   **443:** HTTPS
*   **3306:** MySQL
*   **5432:** PostgreSQL
*   **6379:** Redis

---

## 2. Operating Systems

### A. Process vs Thread
| Feature | Process | Thread |
| :--- | :--- | :--- |
| **Definition** | An executing instance of a program. | A subset of a process (Lightweight Process). |
| **Memory** | Isolated memory space (Stack + Heap). | Shared memory space (Heap shared, Stack private). |
| **Creation** | Heavy (Needs OS calls like fork()). | Lightweight. |
| **Context Switch** | Slow (Save/Restore entire state). | Fast (Switch registers/Stack only). |
| **Example** | Browser Tab (Chrome Process). | JavaScript execution inside Tab. |

### B. Context Switching
**Definition:** Process of storing the state (registers, program counter) of an active process/thread so it can be resumed later, and loading the state of another process.
**Triggered by:**
*   Multitasking (Time slice expiry).
*   Interrupts (I/O completion).
*   System calls (Blocking operations).
**Impact:** High context switching degrades performance (CPU overhead).

### C. Virtual Memory
**Concept:** A technique that gives an application the impression of contiguous working memory, while in reality, it may be fragmented in physical RAM or stored on disk (Swap space).
*   **Paging:** Memory is divided into fixed-size blocks (Pages).
*   **Page Fault:** When a program accesses a page not in RAM, OS retrieves it from disk (Slow).
*   **Thrashing:** Excessive paging due to low RAM causing system freeze.

### D. Deadlock (OS Perspective)
**Conditions (Coffman Conditions):**
1.  **Mutual Exclusion:** Resource cannot be shared.
2.  **Hold and Wait:** Process holds one resource while waiting for another.
3.  **No Preemption:** Resource cannot be forcibly taken away.
4.  **Circular Wait:** P1 waits for P2 -> P2 waits for P1.
**Prevention:** Break one of the 4 conditions (e.g., Avoid Circular Wait by resource ordering).
**Detection:** Resource Allocation Graph.

