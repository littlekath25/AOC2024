# Shellcheck does not have a zsh shell (which the devs will likely use), so use bash as an approximation
# We cannot use a shebang because this script needs to be run as "source ..."
# shellcheck shell=bash

select profile in "EngProd03" "EngDev03"; do
  PROFILE="$profile"
  break;
done

echo "Logging in and setting AWS profile to $PROFILE"

if ! aws configure list-profiles | grep -q "$PROFILE"; then
    echo "Profile $PROFILE does not exist. Configuring SSO"
    echo
    echo "Use the following values:"
    cat << INSTRUCTIONS
SSO session name:           $PROFILE
SSO start URL:              https://d-9067771c12.awsapps.com/start
SSO region:                 us-east-1
SSO registration scopes:    sso:account:access
AWS Account:                Matching $PROFILE
Role:                       AdministratorAccess
CLI default client Region:  us-east-1
CLI default output format:  leave empty
CLI profile name:           $PROFILE
INSTRUCTIONS

    aws configure sso \
      --endpoint-url "https://d-9067771c12.awsapps.com/start" \
      --profile "$PROFILE" \
      --region "us-east-1"
fi

aws sso login --profile "$PROFILE"
export AWS_PROFILE="$PROFILE"
