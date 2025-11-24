Dockerfile
Dockerfile is used to create a custom docker image
The file contains the instructions to create the docker image
The first command inside the Dockerfile should always be the base image
The complete list of docker commands can be found in this link - https://docs.docker.com/reference/dockerfile/
The most commonly used commands are:
FROM - specifies the base image to use for the docker image
RUN - used to run commands inside the docker image during the build process
COPY - used to copy files from the host machine to the docker image
WORKDIR - sets the working directory inside the docker image
CMD - specifies the command to run when a container is started from the docker image
EntryPoint - used to set the main command to be executed when the container starts

The difference between CMD and EntryPoint is that CMD can be overridden when starting a container, while EntryPoint cannot be easily overridden. EntryPoint is used to define the main command for the container, while CMD is used to provide default arguments for that command.

Also, there can by multiple RUN commands in a Dockerfile, but only one CMD and one EntryPoint command is allowed.

Also, the difference between the COPY and RUN command is that the COPY command will copy the files from the source(Host) to the destination(Docker image) while the RUN command will execute the command inside the docker image during the build process. The intermediate containers created by the RUN command will be cached and can be reused in subsequent builds to speed up the build process. i.e to go from one layer to another layer in the docker image we have to create intermediate containers using the RUN command.

Lets start writing the Dockerfile for our Spring boot application

The common patterns for writing a Dockerfile for a Spring Boot application are as follows:
the Dockerfile should be split into two stages - build stage and runtime stage
In the build stage, we will use a maven image to build the Spring Boot application and create the jar file and extract the contents of the jar file.
In the runtime stage, we will use a lightweight JRE image to run the Spring Boot application.

The first image will make use of JDK to build the application and the second image will make use of JRE to run the application. The JDK contains the tools to compile and build the application while the JRE only contains the tools to run the application which is needed to run the Spring Boot application.

Also the JDK is heavier than the JRE image, so using a lightweight JRE image for the runtime stage will help to reduce the size of the final docker image.


Here is a sample Dockerfile for a Spring Boot application:

```Dockerfile
########################
# 1) BUILD STAGE
########################
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

The above line sets the working directory inside the docker image to /app.
The JDK vesion is 21 in this case, you can change it to any version as per your requirement.
The alias 'build' is used to refer to this stage in the next stage.



# ---- Prime the Maven cache for faster incremental builds
# Copy only files needed to resolve dependencies to maximize cache hits.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

In the above lines, we are copying the maven wrapper files (from host machine) and pom.xml file to the docker image.

# Ensure wrapper is executable; use BuildKit cache for ~/.m2
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

The above line gives the executable permission to the maven wrapper file and uses the maven cache to download the dependencies needed for the application. By this step, the dependencies will be downloaded and cached in the docker image which speeds up the build process in subsequent builds.

Once the dependencies are  downloaded, we can now cop the source code and build the application.

# ---- Now copy sources and build (tests skipped here; run tests in CI instead)
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package

The above lines copy the source code from the host machine to the docker image and build the application using maven. The final jar file will be created in the target directory inside the docker image.

The below is th final step as part of the build stage where we will extract the contents of the jar file.

# ---- Extract Spring Boot layers to keep deps cached across code changes
# This enables very fast rebuilds when only application classes change.
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted


In Dockerfile, every layer has the reference to its previous layer just like a linked list. So when we make any changes to a layer, all the subsequent layers will be rebuilt. Unless there is any change to a layer, the subsequent layers will be cached and reused in subsequent builds. Hence, it is always recommended to put the layers which change frequently towards the end of the Dockerfile. the steps which are less likely to change should be put towards the beginning of the Dockerfile.

Lets move on to the runtime stage now.

########################
# 2) RUNTIME STAGE
########################
# Use a slim, well-supported JRE base.
# For even smaller images, consider distroless (see note below).
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

In the above lines, we are using a lightweight JRE image as the base image for the runtime stage. The working directory is set to /app inside the docker image.

This images is another image which contains only the JRE to run the application.

# Security: create a non-root user with no shell
RUN useradd -r -u 10001 -s /usr/sbin/nologin spring

By default, the docker containers run as root user which is not a good practice from security perspective. Hence, we are creating a non-root user 'spring' to run the application inside the docker container.

# Copy layers in stable → volatile order for better Docker layer cache reuse
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./

The above lines copy the extracted layers from the build stage(previous image) to the runtime stage. The layers are copied in the order of stability to maximize the cache hits. The dependencies layer is the most stable layer and the application layer is the most volatile layer.

# Ownership to non-root user (optional if not writing to /app at runtime)
RUN chown -R spring:spring /app
USER spring

In the above lines, we are changing the ownership of the /app directory to the non-root user 'spring' and switching to the 'spring' user to run the application inside the docker container.

# Expose your app port (change if different)
EXPOSE 8081

The above line exposes the port 8081 inside the docker container. Change the port number if your application is running on a different port. This step is optional as we can also map the ports while starting the docker container.

# Start via Spring Boot launcher so classpath stays correct with layers
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

Finally we are using the EntryPoint command to start the Spring Boot application using the JarLauncher class.

To build the docker image, run the following command from the directory where the Dockerfile is located:

```bash
docker build -t my-spring-boot-app .
```
This command will build the docker image and tag it with the name 'my-spring-boot-app'.

- t flag is used to specify the name of the docker image.
- . specifies the current directory as the build context.

This way of building the docker image using the Dockerfile ensures that the file to build the docker image is also present in the project repository and can be easily maintained and updated as part of the project.


docker image
docker container run -d order-microservice
docker container ls
docker container logs <container id>
docker container exec -it <container id> /bin/bash
curl  http://localhost:8081/api/v1/orders

we need to install dos2unix package to convert the line endings from windows to unix format.
RUN apt-get update && apt-get install -y dos2unix
RUN dos2unix ./mvnw

Pushing the Docker Image to Docker Hub

Till now, we were pulling the docker images (nginx, mysql, eclipse-temurin etc.,) from the docker hub and using them. Now we have created our own docker image for the Spring Boot application and push it to the docker hub.

To pull the docker image from public docker hub, we do not need any credentials. 
To push the docker image to docker hub, we need to have an account in docker hub and we need to login to docker hub using the docker CLI.
Also to pull the private images from docker hub, we need to provide the credentials.

Since we are now, pushing the dockee image to docker hub, we need to login to docker hub using the following command:

1 Create an account in docker hub (if not already done)
2 Login to docker hub using the following command:
  docker login -u <docker-hub-username> -p <docker-hub-password>
3. Tag the docker image with the docker hub repository name using the following command:
  docker tag order-microservice <docker-hub-username>/order-microservice:latest  
4. Push the docker image to docker hub using the following command: 
    docker push <docker-hub-username>/order-microservice:latest

The FQDN of the docker image will be <docker-hub-username>/order-microservice:latest
The version by default is latest, we can also provide a custom version while tagging the docker image.

docker tag <iamge-id>:<version> <docker-hub-username>/order-microservice:<version>
if the registry is docker-hub, we do not need to provide the registry URL while tagging the image.
Example: docker tag 1234567890ab:latest myusername/order-microservice:v1.0.0

Some other docker registries are AWS ECR, Azure Container Registry, Google Container Registry, JFrog Artifactory etc.,


Next steps would be to automate the docker image build and push process using CI/CD pipelines.

Pipeline as a Service

Mordern version control systems and code repositories provide built-in CI/CD capabilities to automate the build, test, and deployment processes. These services are often referred to as "Pipeline as a Service" (PaaS) offerings. Examples include:
- GitHub Actions: Integrated with GitHub repositories, allowing you to create workflows that automate tasks such as building, testing, and deploying code.
- GitLab CI/CD: Built into GitLab, providing a seamless experience for defining and running

For Gitlab CI/CD pipelines, you can create a .gitlab-ci.yml file in the root directory of your repository. This file defines the stages, jobs, and scripts to be executed in the pipeline.

The name of the file should be .gitlab-ci.yml

General structure of the .gitlab-ci.yml file:
stages:
  - build
  - test
  - deploy

Pipeline is made up of multiple stages. Each stage can have multiple jobs.
Each stage represents a phase in the pipeline, such as build, test, and deploy.
Stage are executed sequentially, while jobs within a stage are executed in parallel.
Each job contains a script section that defines the commands to be executed.

Stage -> Job -> Script

Gitlab pipeline to push the docker image to docker hub

image: docker:latest 
stages:
  - build 
  - deploy
services:
  - docker:dind
before_script:
  - echo -n $CI_REGISTRY_PASSWORD | docker login -u $CI_REGISTRY_USER --password-stdin $CI_REGISTRY


The above line uses the docker image as the base image for the pipeline.
The services section includes the docker-in-docker (dind) service, which allows us to build and run docker containers within the pipeline.
The before_script section logs in to the docker registry using the credentials stored in the CI/CD variables

Build:
  stage: build
  script:
    - docker pull $CI_REGISTRY_IMAGE:latest || true
    - >
      docker build
      --pull
      --cache-from $CI_REGISTRY_IMAGE:latest
      --label "org.opencontainers.image.title=$CI_PROJECT_TITLE"
      --label "org.opencontainers.image.url=$CI_PROJECT_URL"
      --label "org.opencontainers.image.created=$CI_JOB_STARTED_AT"
      --label "org.opencontainers.image.revision=$CI_COMMIT_SHA"
      --label "org.opencontainers.image.version=$CI_COMMIT_REF_NAME"
      --tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
      .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA


In the above lines, we have defined a job named Build under the build stage.
The script section contains the commands to be executed as part of the Build job.

Deploy:
  variables:
    GIT_STRATEGY: none
  stage: deploy
  only:
    - master
  script:
    - docker pull $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:latest
    - docker push $CI_REGISTRY_IMAGE:latest
In the above lines, we have defined a job named Deploy under the deploy stage.
The Deploy job is configured to run only on the master branch.
This script will  run only when there is a commit to the master branch. The script will build the docke image and push it to the docker hub.

The variables for the script will be passed from the CI/CD variables configured in the Gitlab project settings.


Limitations of running the application inside a Docker Container
- You need to mange the lifecycle of the docker containers manually or using orchestration tools like Kubernetes, Docker Swarm etc.,
- Monitoring and logging of the docker containers need to be setup separately using tools like Prometheus,
- Networking between the docker containers need to be configured properly to ensure the communication between the containers,
- Data persistence needs to be handled separately using volumes or bind mounts,
- Security of the docker containers need to be ensured by following best practices like running the containers as non-root users, using minimal base images, regularly updating the images etc.,
- Resource management of the docker containers need to be handled properly to avoid resource contention between the containers.

Need for Orchestration tools like Kubernetes
As the number of docker containers increases, managing the lifecycle of the containers manually becomes cumbersome and error
-prone. Orchestration tools like Kubernetes help to automate the deployment, scaling, and management of containerized applications. They provide features like self-healing, load balancing, service discovery, rolling updates etc., which makes it easier to manage the docker containers at scale.


Container Orchestration Tools
- Manages the  lifecycle of containers
- Automates deployment, scaling, and operations of application containers across clusters of hosts
- Provides features like load balancing, service discovery, self-healing, rolling updates etc.,

Some popular container orchestration tools are:
- Kubernetes: An open-source container orchestration platform that automates the deployment, scaling, and
- management of containerized applications.
- Docker Swarm: A native clustering and orchestration tool for Docker containers.
- Apache Mesos: A distributed systems kernel that can run and manage containers at scale.
- Amazon ECS: A fully managed container orchestration service provided by AWS.
- Azure Kubernetes Service (AKS): A managed Kubernetes service provided by Microsoft Azure.
- Google Kubernetes Engine (GKE): A managed Kubernetes service provided by Google Cloud Platform.
- Red Hat OpenShift: An enterprise Kubernetes platform provided by Red Hat.
- Rancher: An open-source container management platform that supports multiple orchestration engines including Kubernetes.
- Nomad: A flexible, enterprise-grade cluster manager and scheduler designed to handle a diverse workload of containers and non-containerized applications.
- HashiCorp Consul: A service mesh solution providing service discovery, configuration, and segmentation functionality for containerized applications.
- Vmware Tanzu: A suite of products and services for building, deploying, and managing modern applications on Kubernetes.

Day 2 - Kubernetes for Container Orchestration

The K8s cluster is divided into two main components:

Control Plane: The control plane is responsible for managing the overall state of the cluster. It consists of several components, including:
- API Server: The API server is the central management entity that exposes the Kubernetes API. It serves as the entry point for all administrative tasks and communication within the cluster.
- etcd: etcd is a distributed key-value store that stores the configuration data and state of the cluster.
- Controller Manager: The controller manager is responsible for managing various controllers that monitor the state of the cluster and make necessary adjustments to ensure the desired state is maintained.
- Scheduler: The scheduler is responsible for assigning pods to nodes based on resource availability and other constraints

Worker Nodes: Worker nodes are the machines that run the containerized applications. Each worker node contains several components, including:
- Kubelet: The kubelet is an agent that runs on each worker node and is responsible for managing the pods and containers on that node. It communicates with the control plane to receive instructions and report the status of the node.
- Kube-Proxy: The kube-proxy is responsible for maintaining network rules on the worker nodes. It enables communication between pods and services within the cluster.
- Container Runtime: The container runtime is the software responsible for running containers on a worker node. It pulls container images from a registry, starts and stops containers, and manages container lifecycle. Common container runtimes include Docker, containerd, and CRI-O.

The client tool kubectl is used to inteact with the API server to manage the K8s cluster.
The client makes the REST API calls to the API server and passes the manifest files in YAML or JSON format to create, update, delete and manage the K8s resources like pods, deployments, services etc.,

Just like REST API manages the resources on the server, K8s manages the resources like pods, RS, deployments, services etc., in the K8s cluster using the API server.

To Authenticate with the API server, the kubectl uses the kubeconfig file which contains the cluster details like cluster URL, certificate, user credentials etc.,

The configuration file is usually located at ~/.kube/config location the format of the file is as  below:

clusters:
- cluster-1:
- cluster-2
contexts:
- context-1:
- context-2:
current-context:<one of the contexts>
users:
- user:
- name:
- token: <token>
  name: <username>


Workflow of K8s cluster
1. The user makes the REST API calls (GET, POST, PUT, DELETE) using the kubectl CLI or K8s dashboard or any other client tool to the API server.
2. The API server authenticates the user, then validates the yaml/json manifest file, adds the default values(if any) and then stores the resource details in the etcd database.
3. The scheduler watches for any new pods in the etcd database and then assigns the pods to the appropriate worker nodes based on resource availability and other constraints.
4. The API server then notifies the kubelet(agent running on each worker node) about the new pods assigned to that node.
5. The kubelet then pulls the container images from the container registry and starts the containers inside the pods.

Of the control plane and data plane, cloud providers usually manage the control plane, while users are responsible for managing the data plane (worker nodes). This is referred to as managed K8s services, such as Amazon EKS, Azure AKS, and Google GKE. This ensures that the control plane components are highly available, secure, and up-to-date, while users can focus on deploying and managing their applications on the worker nodes.

To setup a K8s cluster on AWS, we can use a tool called eksctl which automates the process of creating and managing K8s clusters on AWS using Amazon EKS service.

Once the K8s cluster is created, the users will have to download the kubeconfig file to interact with the K8s cluster using kubectl CLI.

Steps to verify the kubectl command 
1. kubectl version --short 

Login to aws management console - https://classpathio.signin.aws.amazon.com/console
Navigate to IAM
Go to your username
Security credentials
Create access keys and secret keys
Copy the access key and secret key

Steps to download the kubeconfig file 
1. aws configure 
    enter the access key, secret key, region(ap-south-1) and output(json) format
2. open the config file located at ~/.aws/config 

vi ~/.aws/config
[default]
region = ap-south-1
output = json
[profile eks-admin]
region = ap-south-1
output = json
role_arn = arn:aws:iam::831955480324:role/eks-admin
source_profile = default

The above configuration lets you switch to the eks-admin profile using the following command:
aws eks --region ap-south-1 update-kubeconfig --name public-eks --profile eks-admin

kubectl get nodes 
kubectl create namespace <your-name>-ns
kubectl config set-context --current --namespace=<your-name>-ns
kubectl get namespace
kubectl get pods

code C:\Users\.kube\config
Go through the kubeconfig file and understand the structure of the file.
kubectl version

Kubernetes Resources

Pods - pods/po
- The smallest atomic and simplest K8s object
- Represents a single instance of a running process in the cluster
- Can contain one or more containers (usually one)
- Pods cannot span across multiple nodes
- Ephemeral in nature; A pod can be created, destroyed, and recreated 
- All other K8s resources either mange or expose the pods 
- Pods are specific to namespaces 

Creating a Pod using YAML manifest file



