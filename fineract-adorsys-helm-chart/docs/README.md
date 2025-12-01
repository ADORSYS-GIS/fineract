# Fineract Helm Chart Documentation

Welcome to the Fineract Helm chart! This document explains how this chart works, how to use it, and how you can extend it.

## 1. What is This Chart For?

This Helm chart is a simple way to deploy the Apache Fineract application to a Kubernetes cluster.

Think of a car. A car needs an engine to run, and a body so you can drive it. In our case:
-   **Fineract** is the "engine" (the main application logic).
-   **MariaDB** is the "fuel tank" and "storage" (the database where all the data is saved).

This Helm chart installs both Fineract and its MariaDB database at the same time, making sure they are connected and configured to work together perfectly.

## 2. How It Works: The Big Picture

This chart uses a **dependency** system. Instead of trying to build a database from scratch, we use a pre-built, official, and trusted Helm chart for MariaDB made by Bitnami.

Here’s the flow when you install this chart:
1.  Helm looks at our `Chart.yaml` and sees it has a "dependency" on MariaDB.
2.  It installs the MariaDB chart first. This creates a database, a user, a password, and a service for other applications to connect to it.
3.  Next, it installs our Fineract application.
4.  During the Fineract installation, it automatically gets the correct database hostname, username, and password from the MariaDB installation.
5.  Fineract starts up, connects to the database, and everything is ready to go.

This approach is great because if the Bitnami team improves their MariaDB chart (e.g., for better security or performance), we can easily update to that new version without changing our Fineract chart much.

## 3. How to Use This Chart

### Prerequisites

-   A Kubernetes cluster (like Minikube, Docker Desktop, or a cloud one).
-   `kubectl` command-line tool installed and configured.
-   `helm` command-line tool installed.

### Installation Steps

1.  **Navigate to the Chart Directory**:
    ```bash
    cd /path/to/fineract/fineract-helm-chart
    ```

2.  **Update Dependencies**: This is a crucial first step. It downloads the MariaDB chart from the internet so Helm can install it. You only need to do this the first time or when the dependency version changes.
    ```bash
    helm dependency update
    ```
    You will see it download and save a `mariadb-*.tgz` file in the `charts/` directory.

3.  **Install the Chart**: This command installs both Fineract and MariaDB into your Kubernetes cluster. We'll give our installation a name, for example, `my-fineract`.
    ```bash
    helm install my-fineract .
    ```

And that's it! Helm will now create all the necessary Kubernetes resources. You can check the status by running:
```bash
kubectl get pods -w
```
You should see a MariaDB pod and a Fineract pod being created. It might take a few minutes for them to start up completely.

### Configuration

The most important file for you is `values.yaml`. This file lets you change the settings of the deployment without touching the template files.

For example, in `values.yaml` you can see the `mariadb` section:
```yaml
mariadb:
  enabled: true
  auth:
    rootPassword: "password"
    database: "fineract_tenants"
    username: "fineract"
    password: "password"
```
Here you can easily change the database name or passwords before you install the chart.

## 4. How to Extend This Chart (e.g., Adding Keycloak)

Let's imagine you want to add another service, like Keycloak for authentication. Here is the mental model and the steps you would follow.

### Step 1: Find a Keycloak Helm Chart

Just like we did for the database, the first step is to find a good, community-trusted Helm chart for Keycloak. The Bitnami charts are usually a great place to start. Let's say you find one.

### Step 2: Add the New Dependency

You would add the Keycloak chart as a new dependency in `fineract-helm-chart/Chart.yaml`. It would look something like this:

```yaml
dependencies:
  - name: mariadb
    version: "11.4.2"
    repository: "https://charts.bitnami.com/bitnami"
    condition: mariadb.enabled
  - name: keycloak # The name of the new chart
    version: "X.Y.Z" # The version of the Keycloak chart you want to use
    repository: "https://charts.bitnami.com/bitnami" # The repository URL for the chart
    condition: keycloak.enabled # This makes it optional!
```
Adding a `condition` like `keycloak.enabled` is a best practice. It means Keycloak will only be installed if you set `keycloak.enabled: true` in your `values.yaml`.

### Step 3: Configure Keycloak in `values.yaml`

You would then add a new section to your `values.yaml` file to configure Keycloak. You'd need to look at the documentation for the Keycloak chart to see what options are available. It might look like this:

```yaml
# At the end of your values.yaml
keycloak:
  enabled: true # Set to true to install it
  auth:
    adminUser: admin
    adminPassword: "your-secure-password"
  # ... and other Keycloak-specific configurations
```

### Step 4: Connect Fineract to Keycloak

This is the most important part. Fineract now needs to know where Keycloak is. You will need to:

1.  **Find the Keycloak Service Name**: The Keycloak chart will create a Kubernetes Service so other pods can find it. The name is usually predictable, like `{{ .Release.Name }}-keycloak`.
2.  **Add Environment Variables to Fineract**: You would edit `fineract-helm-chart/templates/deployment.yaml`. Find the `env:` section for the Fineract container and add the new environment variables that Fineract needs to connect to Keycloak.

For example:
```yaml
          env:
            # ... all the existing database variables
            - name: KEYCLOAK_URL
              value: "http://{{ .Release.Name }}-keycloak:8080"
            - name: KEYCLOAK_REALM
              value: "fineract" # You would configure this in the keycloak section of values.yaml
            # etc.
```

By following this pattern, you can add any number of services as dependencies to your Fineract deployment. The core idea is always:
1.  Add the dependency to `Chart.yaml`.
2.  Expose its configuration in `values.yaml`.
3.  Connect your main application (Fineract) to the new service using environment variables that point to the new service's name.
