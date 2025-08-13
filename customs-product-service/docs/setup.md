# Setup
When setting up a new service, There are a couple of additional steps to complete the setup of your service. See below

### Configure GitHub repository

we recommend you make the following changes to the GitHub repository settings.

In [General](https://github.com/flexport/customs-product-service/settings):
* Disable Wikis, Issues, Discussions and Projects
* Under "Pull Requests" only allow squash merging
* Check "Always suggest updating pull request branches"
* Check "Allow auto-merge"
* Check "Automatically delete head branches"

In [Branches](https://github.com/flexport/customs-product-service/settings/branches):
* Add or edit a branch production rule for "main"
* Check "Require a pull request before merging", with 1 approval from a code owner
* Check "Require status checks to pass before merging" and select the following checks:
  - `CI Dispatch`
  - `Lint - super-linter`
  - `gradle-build / Build and Publish Docker Image`
* Don't forget to save changes at the bottom

### Choose which environments and resources to create
#### Staging

By default, a newly created app is set not to create staging resources to save AWS costs

At any point of time, you can have above resources created in staging by adjusting `enable_staging_env: true` in `.templates.yaml`.
and starting a template upgrade action (can be to the same version of the template you are on).

Note: setting this variable to false will not delete the environment, there is no runbook for deleting an apps resources at the moment.

#### Prod

The production environment will be created when you first run the "Deploy the application to production" action and
updated everytime you deploy again.

### Create Java Cert Store Secrets
Go to [Backstage's Secrets page](https://backstage.gess.flexport.internal/secrets) and create two new secrets using the
following configurations.

Staging:
```
Secret name: app/staging/core_staging/customs-product-service/JVM_KEYSTORE_PASSWORD
Secret value: <<Leave empty>>
Generate a random secret: YES
AWS Account: ENGDEV02 NonProd Secrets (740599847676)
Description: The JVM Keystore password for the customs-product-service
```
![Staging Java Keystore Secret](images/staging-jvm-keystore-secret.png)

Production:
```
Secret name: app/production/default/customs-product-service/JVM_KEYSTORE_PASSWORD
Secret value: <<Leave empty>>
Generate a random secret: YES
AWS Account: GSSS Production Secrets/FKE (544618870930)
Description: The JVM Keystore password for the customs-product-service
```
![Production Java Keystore Secret](images/production-jvm-keystore-secret.png)
