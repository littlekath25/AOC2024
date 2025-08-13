## Terraform

fke_nonprod contains all terraform for the staging environment
fke_prod contain terraform for the production environment.
modules/customs-product-service contains files shared between the two

##### Terraform version

Terraform version is managed using `tfenv` and `.terraform-version`

```shell
brew uninstall terraform
brew install tfenv
tfenv install
```

### Running terraform commands locally

Request AWS admin access to either EngDev03 (staging, fke_nonprod) or EngProd03 (production, fke_prod) using FlexAccess.

Now run `source aws_login.sh` in your terminal.
Then in either `fke_prod` (prod) or `fke_nonprod` (staging) you run `terraform init`.

##### Local planning

Running `terraform plan` locally successfully is not possible because the databases are not reachable from your machine,
you can work around this by using [resource targeting](https://developer.hashicorp.com/terraform/cli/commands/plan#resource-targeting).

### Remove state lock

If a Terraform workflow is cancelled it can happen that the [state lock](https://developer.hashicorp.com/terraform/language/state/locking)
remains, blocking all terraform operations.

You can in this case use the "Terraform Force unlock" workflow (see the Actions tab of this repository) to remove the state lock.
When selecting "run workflow", you can see the parameters that are required.

### Destroy resources

Normally, when removing something from terraform, terraform will remove the related infrastructure next time it applies.

If a resource managed by terraform is a child to another resource managed by terraform, for example database roles inside
a database, they cannot be deleted all in one go. In this case, first delete the child resources, deploy terraform,
and then delete the parent resource.

In case this is not possible, you can also manually delete the resource from the terraform state by running
`terraform state rm`, for example:

```shell
terraform state rm module.customs_product_service.module.service_rds_cluster.data.aws_iam_role.cyral_readonly_role
```

See the [running terraform commands locally](#running-terraform-commands-locally) section for how set this up.


