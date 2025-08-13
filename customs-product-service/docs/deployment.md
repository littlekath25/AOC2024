### Argo CD Deployment

This repository contains the Helm chart and deployment pipeline for the Customs Service in both staging and production environments. The service is deployed using [Argo CD](https://argocd.flexport.io/), which synchronizes the charts for both staging and production with their respective deployment environments. Helm chart validation is performed in the GitHub CI workflow.

## Continuous deployment to Staging

This service has a CI pipeline that validates the code (including formatting, linting, and running test suites) and builds the Docker image for all feature branches. When we merge pull requests into the main branch, the CI pipeline performs the following tasks:

- Publishes a Docker image to Artifactory.
- Creates a Git tag with the new version number.
- Updates `targetRevision` and `image.tag` in the `stage.yaml` file located in app-config/customs-product-service within the [flexport-app-config](https://github.com/flexport/flexport-app-config) repository with the new version number.
- ArgoCD synchronizes the new image and deploys the service to the staging environment.

## Deployment to Production

Deployment to production is triggered through a manual workflow. The workflow expects the image tag to be provided as input to deploy the image to production. When the workflow is triggered for production:

- It validates the GraphQL Schema.
- Updates `targetRevision` and `image.tag` in the `prod.yaml` file located in app-config/customs-product-service within the [flexport-app-config](https://github.com/flexport/flexport-app-config) repository with the new version number.
- ArgoCD synchronizes the new image and deploys the service to the production environment.
- Publishes the GraphQL Schema to Core-Production Apollo Federation Graph.

### Automatic scheduled deployment

- To enable automatic deployments in the repository, first, set the environment variable AUTODEPLOY_ENABLED to true [here](https://github.com/flexport/customs-asn-service/settings/variables/actions). This toggle signals automated deployment workflows to execute seamlessly at scheduled time.
- If you want to configure deployments schedule in the project, you can find settings [here](../.github/workflows/deploy-to-production.yaml). We recommend scheduling deployments at 08:00 UTC for European teams and 17:00 UTC for US teams.

## Rollback

To perform a rollback of the image in either the staging or production environment create a pull request (PR) to update the image tag in the respective stage.yaml or prod.yaml files
These files are located in app-confg/customs-product-service in [flexport-app-config](https://github.com/flexport/flexport-app-config) repository


### Resource Configuration

#### Setting Pod Resources

Datadog's metrics `kubernetes.cpu.usage.total` & `kubernetes.memory.usage` and
adjust the CPU and memory requests to better reflect the actual usage.

If you are not sure what values suits your service, here are some general suggestions:
- CPU: Set requests to the average or p50 for CPU
- Memory: Set requests to the max
  - Only the requested resource is guaranteed for your pods, if you can't allocate the memory you will get OOMKilled errors.

