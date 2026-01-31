# Deployment Guide (Kubernetes)

This guide explains how to deploy the Microservice Architecture to Kubernetes on Cloud Providers like GCP (Google Cloud Platform) or Azure.

---

## 1. Containerization

Before deploying to Kubernetes, your services must be available in a container registry.

### Build Images
For each service:
```bash
./gradlew bootBuildImage --imageName=<registry-url>/<project-id>/<service-name>
```

### Push to Registry
- **GCP (Artifact Registry)**:
  ```bash
  gcloud auth configure-docker us-central1-docker.pkg.dev
  docker push us-central1-docker.pkg.dev/my-project/my-repo/currency-exchange:latest
  ```
- **Azure (ACR)**:
  ```bash
  az acr login --name myregistry
  docker push myregistry.azurecr.io/currency-exchange:latest
  ```

---

## 2. Infrastructure Setup

### GCP (Google Kubernetes Engine - GKE)
1. **Create Cluster**:
   ```bash
   gcloud container clusters create microservices-cluster --num-nodes=3 --zone=us-central1-a
   ```
2. **Get Credentials**:
   ```bash
   gcloud container clusters get-credentials microservices-cluster --zone=us-central1-a
   ```

### Azure (Azure Kubernetes Service - AKS)
1. **Create Resource Group**:
   ```bash
   az group create --name myResourceGroup --location eastus
   ```
2. **Create Cluster**:
   ```bash
   az aks create --resource-group myResourceGroup --name myAKSCluster --node-count 3 --generate-ssh-keys
   ```
3. **Get Credentials**:
   ```bash
   az aks get-credentials --resource-group myResourceGroup --name myAKSCluster
   ```

---

## 3. Kubernetes Objects

You need to define `Deployment` and `Service` (and potentially `ConfigMap` or `Secret`) for each microservice.

### Example Deployment (deployment.yaml)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: currency-exchange
spec:
  replicas: 2
  selector:
    matchLabels:
      app: currency-exchange
  template:
    metadata:
      labels:
        app: currency-exchange
    spec:
      containers:
      - name: currency-exchange
        image: <registry-url>/currency-exchange:latest
        ports:
        - containerPort: 8000
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: http://naming-server:8761/eureka/
```

### Example Service (service.yaml)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: currency-exchange
spec:
  selector:
    app: currency-exchange
  ports:
    - protocol: TCP
      port: 8000
      targetPort: 8000
  type: ClusterIP
```

---

## 4. Deployment Strategy

1. **Naming Server First**: Deploy Eureka/Naming Server so other services can register.
2. **Config Server**: If using centralized config, deploy it next.
3. **Microservices**: Deploy individual services.
4. **API Gateway**: Deploy the gateway and use a `LoadBalancer` type service to expose it to the internet.

```bash
kubectl apply -f k8s/naming-server.yaml
kubectl apply -f k8s/currency-exchange.yaml
# ... and so on
```

---

## 5. Helpful Commands
- **Check Pods**: `kubectl get pods`
- **View Logs**: `kubectl logs -f <pod-name>`
- **Check Services**: `kubectl get svc`
- **Describe Resources**: `kubectl describe pod <pod-name>`

---

## 6. Zero Downtime Secret Rotation (Compromised Passwords)

**Scenario**: A database password or API key is compromised. You need to update it across all microservices without stopping the service for customers.

### Strategy: Rolling Update
Kubernetes `Deployment` by default uses a `RollingUpdate` strategy.

1.  **Update the Database**: Change the password in your database.
    *   *Tip*: Many modern databases (like Vault or managed RDS) allow you to have two active passwords during a migration. If not, proceed to step 2 immediately.
2.  **Update Kubernetes Secret**:
    ```bash
    kubectl create secret generic mssql-secret \
      --from-literal=password='NewStrongPassword123' \
      --dry-run=client -o yaml | kubectl apply -f -
    ```
3.  **Trigger Rolling Update**:
    Updating the secret does **not** automatically restart the pods if the secret is mapped as environment variables. You must trigger a restart:
    ```bash
    kubectl rollout restart deployment/currency-exchange
    ```
4.  **How it achieves Zero Downtime**:
    - Kubernetes starts **new pods** one by one.
    - These new pods read the **new secret** value.
    - Kubernetes waits for the new pods to pass their **Readiness Probes** before terminating the old pods.
    - Customers are gradually routed from old pods (using old password) to new pods (using new password).

### Advanced: Spring Cloud Bus
If you are using **Spring Cloud Config** with **Spring Cloud Bus** (RabbitMQ/Kafka), you can push a `/actuator/bus-refresh` event to update properties across all services without any pod restarts.
