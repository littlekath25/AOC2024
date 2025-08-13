#!/bin/bash

# The script is for creating resources via terraform in localstack
# Doc: https://docs.localstack.cloud/user-guide/integrations/terraform/#manual-configuration

# Make sure to have your localstack service running
# Make sure you create a main.tf file in terraform/local folder

cd terraform/local || exit
terraform init
echo "yes" | terraform apply

