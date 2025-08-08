# Technical Guide: From Kubernetes Manifests to a Helm Chart

This document provides a detailed, technical explanation of how the original Kubernetes manifests in the `@kubernetes` directory were translated into this dynamic Helm chart. It's designed to help you understand the structure, the connections between components, and how to manage configuration and secrets effectively.

## 1. Core Concepts: The "Why" of Helm

The original `@kubernetes` directory contained a set of static YAML files. To deploy the application, you had to run a shell script (`kubectl-startup.sh`) that applied these files in a specific order. This works, but it has some drawbacks:

-   **Static and Inflexible**: If you wanted to change a value (like a Docker image tag or a password), you had to manually edit the YAML files. This is prone to errors.
-   **Manual Process**: The deployment relied on a script. This is not a "declarative" approach, which is the standard for modern cloud-native applications.
-   **No Reusability**: You couldn't easily share or version this deployment. Every new deployment would be a copy-paste of the files.

**Helm solves these problems.** It allows us to take those static YAML files and turn them into smart **templates**. We can then manage all the changing parts (the "values") in a single, clean file: `values.yaml`.

## 2. The Helm Chart Structure

Our Helm chart has a standard structure:

-   `Chart.yaml`: The "ID card" of our chart. It contains the name, version, and, most importantly, the **dependencies**.
-   `values.yaml`: The **single source of truth for configuration**. This is where you change settings.
-   `templates/`: This directory contains all the Kubernetes YAML files, but they are now smart templates instead of static files.
-   `templates/_helpers.tpl`: A special file for creating reusable template snippets, like generating standard labels for all our resources.
-   `charts/`: When you run `helm dependency update`, the dependent charts (like MariaDB) are downloaded and stored here.

## 3. From `@kubernetes` to `@fineract-helm-chart`: A Detailed Walkthrough

Let's go through each original file and see how it was transformed.

### The Database: `fineractmysql-*.yml`

-   **Original Approach**: The `@kubernetes` directory had three files to manage the database: a `Deployment` to run it, a `ConfigMap` to create the empty databases, and a `PersistentVolume` for storage.

-   **Helm Approach**: This is where we made the biggest and best change. Instead of managing a database ourselves, we declared a **dependency** on the official Bitnami MariaDB chart. This is the standard practice.

    -   **`Chart.yaml`**: We added this section:
        ```yaml
        dependencies:
          - name: mariadb
            version: "11.4.2"
            repository: "https://charts.bitnami.com/bitnami"
            condition: mariadb.enabled
        ```
        This tells Helm: "Before you install Fineract, you must first install the MariaDB chart from this repository."

    -   **`values.yaml`**: We now control the entire database setup from our `values.yaml` file. The `mariadb` section is passed directly to the MariaDB subchart.
        ```yaml
        mariadb:
          enabled: true
          auth:
            database: "fineract_tenants"
            # ... other auth settings
          primary:
            # ... persistence and initdb settings
        ```
        This is how we replaced the old `fineractmysql-deployment.yml` and `fineractmysql-configmap.yml`. We let the professional, community-maintained chart do the heavy lifting.

### The Fineract Server: `fineract-server-deployment.yml`

This is the core of our chart. Here's how we converted the static file into a template.

-   **Original Approach**: A static `Deployment` and `Service` file with hardcoded values.

-   **Helm Approach (`templates/deployment.yaml` & `templates/service.yaml`)**:

    -   **Dynamic Naming and Labels**: You'll see `{{ include "fineract-helm-chart.fullname" . }}` everywhere. This is a template call that uses the helper functions in `_helpers.tpl` to generate a unique and consistent name for all resources based on the release name you provide during `helm install`.

    -   **Configuration from `values.yaml`**: Every configurable value is pulled from `values.yaml`. For example:
        -   `replicas: {{ .Values.replicaCount }}` gets the number of pods from the `replicaCount` value.
        -   `image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"` builds the Docker image name from the `image` object in `values.yaml`.

### The Magic of Connections: Secrets and Environment Variables

This was the trickiest part you faced before, and it's the most critical part of the Helm chart.

-   **Original Approach**: The `kubectl-startup.sh` script manually created a secret with a random password: `kubectl create secret ...`. Then, the `fineract-server-deployment.yml` file referenced this secret by its hardcoded name (`fineract-tenants-db-secret`).

-   **Helm Approach**: We automated and connected everything.

    1.  **Database Credentials**: In our `values.yaml`, we define the username and password for our Fineract database user inside the `mariadb.auth` section. These values are passed to the MariaDB chart, which uses them to create the user in the database.

    2.  **The Fineract Secret (`templates/fineract-db-secret.yaml`)**: We created this new template. Its only job is to create a Kubernetes Secret. But where does it get the data?
        ```yaml
        data:
          username: {{ .Values.mariadb.auth.username | b64enc | quote }}
          password: {{ .Values.mariadb.auth.password | b64enc | quote }}
        ```
        It reads the **same values** from the `mariadb.auth` section of our `values.yaml` file. This ensures the secret Fineract uses will always match the user that the MariaDB chart creates. The `b64enc` part is a Helm function that Base64-encodes the data, which is required for Kubernetes secrets.

    3.  **Connecting Fineract to the Secret (`templates/deployment.yaml`)**: In the Fineract deployment, we now populate the environment variables by referencing this new, dynamically named secret.
        ```yaml
        - name: FINERACT_HIKARI_USERNAME
          valueFrom:
            secretKeyRef:
              name: {{ include "fineract-helm-chart.fullname" . }}-db-secret
              key: username
        ```
        This tells the Fineract pod: "For the `FINERACT_HIKARI_USERNAME` environment variable, get your value from the secret named `my-fineract-fineract-helm-chart-db-secret` (or whatever Helm generates), and use the data stored under the `username` key."

    4.  **Connecting Fineract to the Database Service**: How does Fineract know the hostname of the database? The Bitnami MariaDB chart creates a service with a predictable name: `{{ .Release.Name }}-mariadb`. We use this in our `deployment.yaml`:
        ```yaml
        - name: FINERACT_HIKARI_JDBC_URL
          value: "jdbc:mariadb://{{ .Release.Name }}-mariadb:3306/{{ .Values.mariadb.auth.database }}"
        ```
        When you run `helm install my-fineract .`, Helm replaces `{{ .Release.Name }}` with `my-fineract`, so the final JDBC URL becomes `jdbc:mariadb://my-fineract-mariadb:3306/fineract_tenants`, which is the correct address for the database service within the Kubernetes cluster.

### The Init Container

The `initContainer` in `deployment.yaml` is a special container that runs to completion before the main Fineract container starts. Its only job is to wait until the database is ready. We updated its command to wait for the correct service name:
```yaml
command:
  - sh
  - -c
  - |
    echo -e "Checking for the availability of MYSQL server deployment";
    while ! nc -z "{{ .Release.Name }}-mariadb" 3306; do
      sleep 1;
      printf "-";
    done;
    echo -e " >> MYSQL server has started";
```
This prevents Fineract from starting and immediately crashing because the database isn't online yet.

## 4. Summary: The Flow of Data and Control

1.  You, the user, define all your settings in **`values.yaml`**. This is your control panel.
2.  You run `helm install`.
3.  Helm reads `Chart.yaml` and installs the MariaDB dependency first, passing the `mariadb` section from your `values.yaml` to it.
4.  The MariaDB chart creates the database, users, and a service named `{{ .Release.Name }}-mariadb`.
5.  Helm then processes our `templates/` directory.
6.  `templates/fineract-db-secret.yaml` creates a secret, pulling the credentials from the `mariadb` section of `values.yaml`.
7.  `templates/deployment.yaml` creates the Fineract deployment. It populates the environment variables by pointing to the database service name and the secret we just created.

This creates a powerful, reusable, and easy-to-understand deployment where all the moving parts are connected logically through the Helm templating engine.