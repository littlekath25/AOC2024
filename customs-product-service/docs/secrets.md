# Secret management

### Create custom secrets
You can set custom secret variables and add them yourself via backstage. reference this [Self-service Secrets Documentation](https://flexport.atlassian.net/wiki/spaces/IN/pages/2321353507/Self-service+Secrets+Documentation)

Currently, we have two environment staging and production, staging secrets stored in ENGDEV02(740599847676) and production secrets stored in GSSS(544618870930).
Please select the correct account when creating the secrets.

The storage path of the secret key is different for different environments.

1. Staging: `app/staging/core_staging/customs-product-service`
2. Production: `app/production/default/customs-product-service`

For example, if you want to add one secret for an api key
1. In staging environment
   * Secret Name: `app/staging/core_staging/customs-product-service/API_KEY`
   * Secret Value: api key
2. In production environment
   * Secret Name: `app/production/default/customs-product-service/API_KEY`
   * Secret Value: api key

### Load a secret via Hoplite configuration

Once you've added a secret to Backstage, you'll be able to view the secret ARN. To load it into your application you can reference it in your Hoplite `configuration.yaml` using the `awssm://` syntax. For example:
```yaml
externalApi:
  sslContext:
    javaKeyStorePassword: awssm://aws:secretsmanager:us-east-1:740599847676:secret:app/staging/core_staging/customs-product-service/JVM_KEYSTORE_PASSWORD
```

If you want to reference a subkey of a key-value AWS SM secret, you can index it using the `[KEY]` syntax. For example:
```yaml
database:
  username: awssm://arn:aws:secretsmanager:us-east-1:740599847676:secret:app/staging/core_staging/customs-product-service/POSTGRES_READWRITE[USERNAME]
```
If you want to combine secrets with other values, you can do so in your data class:

```kotlin
data class DatabaseConfiguration(
  val username: String,
  val host: String
) {
   val url = "jdbc://${username}@${host}/..."
}
```

### Deprecated: Add custom secrets in Secret Provider Class

Since the template already generates the necessary code, you only need to focus on adding your own secret key.
Let's use the example we just did.
It is enough to update the values in values.yaml.
```
spc:
  secretAccount: ""
  scopes:
    default:
      path: ""
      envVars:
        JWT_AUTH_PUBLIC_KEY: JWT_AUTH_PUBLIC_KEY
    service:
      path: ""
      envVars:
        POSTGRES_READWRITE:
          USERNAME: POSTGRES_USER
          PASSWORD: POSTGRES_PASSWORD
        POSTGRES_READONLY:
          USERNAME: POSTGRES_READONLY_USERNAME
          PASSWORD: POSTGRES_READONLY_PASSWORD
          SOME_SERVICE_API_KEY: ENVIRONMENT_SOME_SERVICE_API_KEY
    infra:
      path: ""
      envVars:
        POSTGRES_MIGRATIONS:
          USERNAME: POSTGRES_MIGRATIONS_USERNAME
          PASSWORD: POSTGRES_MIGRATIONS_PASSWORD
        POSTGRES_DBZ:
          USERNAME: POSTGRES_DBZ_USERNAME
          PASSWORD: POSTGRES_DBZ_PASSWORD
```
Just add a custom key to envVars for the service as in the above code.
PS: `SOME_SERVICE_API_KEY` means secret key, `ENVIRONMENT_SOME_SERVICE_API_KEY` means environment variable key name, so if you want to get `SOME_SERVICE_API_KEY` value, you should use  `ENVIRONMENT_SOME_SERVICE_API_KEY` as key name in your service
