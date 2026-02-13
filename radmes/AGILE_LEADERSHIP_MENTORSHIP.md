# Agile, Leadership, and Mentorship Guide

This document covers senior-level interview questions and best practices for Agile methodologies, code reviews, and technical leadership.

---

## 🏃 Agile & Scrum (Sprint) Methodologies

### 1. What is the difference between "Definition of Done (DoD)" and "Acceptance Criteria (AC)"?
*   **Acceptance Criteria**: Specific to a **single User Story**. It defines the functional requirements (e.g., "User must be able to reset password via email link").
*   **Definition of Done (DoD)**: A global checklist applied to **every** story. It ensures quality (e.g., "Unit tests pass, code reviewed, documentation updated, security scan passed").
*   **Senior Tip**: Mention that DoD is a contract between the Team and the Product Owner to ensure no "hidden work" or technical debt is left behind.

### 2. How do you handle "Scope Creep" mid-sprint?
*   **Scenario**: The Product Owner wants to add a "small" feature to an ongoing sprint.
*   **Senior Answer**: "I protect the sprint goal. I would first assess the impact. If it's truly urgent, we negotiate: 'What current item from the sprint are we removing to accommodate this?' We never just 'add' work, as it leads into burnout and missed commitments. If it can wait, it goes to the Product Backlog for the next planning."

### 3. What would you do if the team realizes halfway through that the Sprint Goal won't be met?
*   **Action**: Transparency is key.
    1. **Notify the PO early**: Don't wait until the Review.
    2. **Reprioritize**: Focus on the highest-value items that *can* be finished.
    3. **Analyze in Retro**: Was the estimation wrong? External dependencies? Use this to improve the next sprint's velocity.

### 4. How do you manage Technical Debt in an Agile environment?
*   **Strategy**: "Technical debt isn't always bad, but it must be visible. I advocate for the **'20% Rule'**—allocating 20% of every sprint's capacity to refactoring, library updates, and fixing technical debt. If it's a major debt, I work with the PO to create dedicated 'Enabler' stories in the backlog."

### 5. How do you assess and analyze Story Points for a User Story?
When a senior developer analyzes a story to assign "points," we don't just look at **time**. We use the **CUER** framework:

1.  **Complexity**: How difficult is the logic? Does it involve legacy code with no tests? Are there many integration points?
2.  **Uncertainty (Unknowns)**: Do we have all the requirements? Is the technology new to the team? High uncertainty = High points.
3.  **Effort**: How much "typing" or manual work is involved? Even if it's simple, if it takes 5 days of repetitive task, it's higher effort.
4.  **Risk**: What could go wrong? If we touch this code, could it break the payment gateway?

#### The "Analytical Flow" for a Senior:
*   **Step 1: Check the 'Definition of Ready' (DoR)**: Does the story have clear Acceptance Criteria? If not, we can't point it—it needs more refinement.
*   **Step 2: Comparison (Triangulation)**: "Is this story bigger or smaller than the 'User Login' story we gave 3 points to last sprint?" Points are **relative**.
*   **Step 3: Break it down**: If a story feels like an 8 or a 13, I analyze if we can split it. A story that is too large is usually an **Epic** hiding in the backlog.
*   **Step 4: Team consensus**: We use **Planning Poker**. If I say 3 and a junior says 8, I don't "overrule" them. I ask **why**. Maybe they know a complexity in the codebase that I missed, or they need more guidance.

---

## 🔄 The Lifecycle of a Sprint: From Start to Finish

### 1. Sprint Planning (The Kick-off)
*   **Input**: Product Backlog (Prioritized by PO) and Team Velocity (Capacity).
*   **Process**: 
    1.  PO presents the priority items. 
    2.  Team "size" the stories (Story Points) if not already done.
    3.  Team commits to what they can realistically finish.
*   **Output**: **Sprint Goal** and **Sprint Backlog**.

### 2. Execution & Daily Scrum (The Grind)
*   **Process**: 24-hour cycle of development.
*   **Daily Scrum**: A 15-minute sync. **Not a status report**.
    *   *Question*: "What did I do? What will I do? Are there any blockers?"
*   **Senior Role**: This is where I identify hidden complexities early and help unblock juniors.

### 3. Backlog Refinement (Ongoing - Mid-Sprint)
*   **Process**: Looking ahead. We review the next 1-2 sprints' worth of stories.
*   **Goal**: Ensure stories are "Ready" (invested, clear AC, sized). This prevents a "refinement meeting" during Sprint Planning.

### 4. Sprint Review (The Demo)
*   **Process**: Demonstration of the "Done" increment to stakeholders.
*   **Outcome**: Feedback loop. Stakeholders see real progress and might adjust the Product Backlog for the next sprint based on what they see.

### 5. Sprint Retrospective (The Reflection)
*   **Process**: The team looks at **how** they worked, not **what** they build.
*   *Keywords*: "What went well? What didn't? What can we improve?"
*   **Senior Tip**: I use this to fix technical process issues (e.g., "Our builds are too slow" or "The PR review time is taking 3 days").

---

## 👔 Leadership & Senior Roles

### 1. How do you review code as a Senior Developer?
When I review code, I look at it in three layers:
1.  **Layer 1: Correctness & Logic**: Does it actually solve the problem? Are there edge cases missed (nulls, timeouts, race conditions)?
2.  **Layer 3: Architecture & Scalability**: Does this change follow our design patterns? Is it creating tight coupling? Will this code perform poorly if the database size grows 10x?
3.  **Layer 4: Readability & Maintainability**: If a new developer joins tomorrow, can they understand this? Are the names descriptive? Is the method too long?
*   **Soft Skills**: I focus on the code, not the person. Instead of "Your code is slow," I say "I'm concerned this loop might have performance issues with large datasets; what do you think about using a Map here?"

### 2. Senior Engineer: Roles & Responsibilities
A Senior Engineer's value isn't just in writing code; it's in **multiplying the impact** of the team.
*   **System Ownership**: Being responsible for the long-term health, security, and scalability of a module or service.
*   **Process Improvement**: Identifying bottlenecks (e.g., "Deployment takes too long") and fixing them (e.g., "Let's automate the CI/CD pipeline").
*   **Technical Roadmap**: Helping the PO understand technical constraints and planning for future architectural needs 6 months ahead.
*   **Mentorship**: Elevating the skills of everyone around them.

---

## 🤝 Mentorship & Growing Others

### 1. How do you mentor Junior Developers?
*   **Pair Programming**: Not just "watch me code," but letting them drive while I guide.
*   **The "Why" over the "How"**: Instead of just giving the solution, explain the underlying principle (e.g., "We use an Interface here because it allows us to swap the implementation for testing later").
*   **Safe Environment**: Encouraging them to ask "stupid" questions and allowing them to make safe mistakes.
*   **Growth Plans**: Helping them identify where they want to grow (e.g., "I want to learn more about AWS") and finding tickets that give them that exposure.

### 2. How do you handle an underperforming team member?
*   **Empathy First**: Is there a personal issue or a lack of training?
*   **Clear Feedback**: Have a 1-on-1. Be specific: "I've noticed your last three tickets were significantly delayed due to X. What's holding you up?"
*   **Action Plan**: Set small, achievable goals over 2-4 weeks. Provide extra support or pairing.
*   **Escalation**: If there's no improvement after support and clear feedback, then involve the Engineering Manager for a formal PIP.

