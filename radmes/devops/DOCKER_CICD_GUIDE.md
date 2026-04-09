# Docker, Deployment, and CI/CD Guide

## 1. What is Docker?
Docker is an open-source platform that uses **OS-level virtualization** to deliver software in packages called **containers**.
*   **Problem it Solves:** "It works on my machine!" – Discrepancies between development and production environments.
*   **Key Concept:** Containers bundle an application with its dependencies (libraries, config files) into a single standard unit.
*   **Difference from VM:** Containers share the host OS kernel, making them lightweight (MBs vs GBs) and fast to start (seconds vs minutes).

## 2. Docker vs Virtual Machine (VM)

| Feature | Docker (Container) | Virtual Machine (VM) |
| :--- | :--- | :--- |
| **Architecture** | OS-level virtualization (Shares Host Kernel) | Hardware-level virtualization (Hypervisor + Guest OS) |
| **Size** | Lightweight (Megabytes) | Heavyweight (Gigabytes, includes full OS) |
| **Startup Time** | Milliseconds/Seconds | Minutes (Booting full OS) |
| **Resource Usage** | Efficient (Native performance) | High overhead (Duplicated OS resources) |
| **Isolation** | Process isolation (Namespaces/Cgroups) | Strong isolation (Separate Kernel) |
| **Portability** | High (Run anywhere Docker runs) | Moderate (Dependent on Hypervisor format) |

## 3. What is Container Lifecycle?
A Docker container goes through several states during its lifetime:
1.  **Created:** The container is created from an image but not started yet (`docker create`).
2.  **Running:** The container is actively executing processes (`docker start`, `docker run`).
3.  **Paused:** All processes inside are suspended but memory is preserved (`docker pause`).
4.  **Stopped/Exited:** The main process has finished or been killed (`docker stop`, `exit 0`).
5.  **Deleted:** The container is removed from the host (`docker rm`).

**Typical Flow:** `docker pull` -> `docker create` -> `docker start` -> `docker stop` -> `docker rm`

## 4. How do you deploy an app using Docker + ECS?
**AWS ECS (Elastic Container Service)** is a container orchestration service.

**Steps:**
1.  **Build Image:** Create a `Dockerfile`, run `docker build -t my-app:v1 .`.
2.  **Push to ECR:** Tag the image for AWS ECR (Elastic Container Registry) and push it (`docker push <aws-account-id>.dkr.ecr.us-east-1.amazonaws.com/my-app:v1`).
3.  **Create Task Definition:** Define the blueprint for your application (JSON/Console):
    *   Image URI (from ECR)
    *   CPU/Memory limits
    *   Environment Variables
    *   Port Mappings
    *   IAM Roles (Task Execution Role)
4.  **Create Service:** A Service maintains a specified number of instances of the Task Definition.
    *   Select Cluster (Fargate or EC2)
    *   Desired Count (e.g., 2 replicas)
    *   Attach Application Load Balancer (ALB)
5.  **Deploy:** Update the service to use the new Task Definition revision. ECS performs a rolling update (starts new tasks, drains old ones).

## 5. What is Image Layering?
Docker images are built from a series of layers.
*   **Union File System:** Docker uses a Union File System (e.g., Overlay2) to stack layers on top of each other into a single view.
*   **Base Image:** The bottom layer (e.g., `FROM ubuntu:20.04`).
*   **Instructions:** Each instruction in a `Dockerfile` (`RUN`, `COPY`, `ADD`) creates a new read-only layer.
*   **Caching:** Docker caches intermediate layers. If you change a line in your `Dockerfile`, only that layer and subsequent layers are rebuilt.
*   **Container Layer:** When a container starts, a thin **read-write** layer is added on top. All changes made by the running container happen here. When the container is deleted, this layer is lost unless committed.

## 6. What is CI/CD Pipeline?
**CI/CD** stands for **Continuous Integration** and **Continuous Delivery/Deployment**. It automates the software delivery process.

*   **Continuous Integration (CI):** Developers merge code changes into a central repository frequently. Automated builds and tests run to verify the changes.
    *   *Goal:* Detect errors quickly ("Fail Fast").
*   **Continuous Delivery (CD):** Code changes are automatically built, tested, and prepared for a release to production. Requires manual approval to deploy.
*   **Continuous Deployment (CD):** Every change that passes all stages of the production pipeline is released to customers automatically, with *no human intervention*.

## 7. GitHub Actions vs Bitbucket Pipelines

| Feature | GitHub Actions | Bitbucket Pipelines |
| :--- | :--- | :--- |
| **Integration** | Native to GitHub. Tight integration with Issues, PRs, Packages. | Native to Bitbucket. Strong Jira/Atlassian integration. |
| **Configuration** | `.github/workflows/*.yml` | `bitbucket-pipelines.yml` |
| **Marketplace** | Massive marketplace with community-built actions. | Uses "Pipes". Smaller eco-system compared to GH Actions. |
| **Runners** | GitHub-hosted (Linux, Windows, macOS) & Self-hosted. | Cloud runners & Self-hosted runners. |
| **Cost** | Free tier available (2000 mins/month for free accounts). | Free tier available (50 mins/month for free accounts). |
| **Use Case** | General purpose, open source projects, GitHub users. | Enterprise teams using Jira & Bitbucket. |

## 8. How to design a deployment pipeline?
A robust pipeline ensures code quality and reliability.

**Stages:**
1.  **Source:** Developer pushes code to Git (Feature Branch).
2.  **CI (Build & Test):**
    *   **Linting:** Check code style.
    *   **Unit Tests:** Run JUnit/Mockito tests.
    *   **SAST:** Static Application Security Testing (SonarQube).
    *   **Build:** Compile code and build artifact (JAR/WAR or Docker Image).
3.  **Publish:** Push Docker image to Registry (ECR/DockerHub) or Artifact to Nexus/Artifactory.
4.  **Deploy to Dev/QA:** Automatically deploy to a test environment.
    *   **Integration Tests:** Run API/Selenium tests against the deployed app.
5.  **Staging (Pre-Prod):** Deploy to an environment identical to Production.
    *   **Performance Testing:** Load testing (JMeter).
    *   **Manual Approval:** A gate requiring a manager/lead to click "Approve".
6.  **Production Deployment:** Deploy to Live environment using a strategy (Blue-Green/Canary).
7.  **Post-Deployment:** Monitor logs (Splunk/ELK) and metrics (Prometheus/Grafana).

## 9. What is Blue-Green Deployment?
A deployment strategy to minimize downtime and risk.
*   **Setup:** Two identical environments, **Blue** (Current Live) and **Green** (New Version).
*   **Process:**
    1.  Deploy the new version (v2) to the **Green** environment.
    2.  Run tests on Green (Private access).
    3.  Switch the Load Balancer / Router to point traffic from Blue to Green.
*   **Benefits:** Instant rollback (switch back to Blue), zero downtime.
*   **Drawbacks:** Requires double the infrastructure resources (costly).

## 10. What is Canary Deployment?
A deployment strategy that releases an application to a small subset of users gradually.
*   **Concept:** Like a "canary in a coal mine" – detect issues early with low impact.
*   **Process:**
    1.  Deploy new version (v2) alongside old version (v1).
    2.  Route a small percentage of traffic (e.g., **5%**) to v2.
    3.  Monitor metrics (Error rates, latency).
    4.  If successful, gradually increase traffic (10% -> 50% -> 100%).
    5.  If errors spike, route all traffic back to v1 immediately.
*   **Benefits:** Lowest risk, real-world testing.
*   **Tools:** Istio, AWS App Mesh, Nginx, Kubernetes.

