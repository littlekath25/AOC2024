# Sentry

This project supports error monitoring using [ Sentry ](https://flexport.sentry.io/).

## Setup

To enable this feature you need to create a sentry project and fill the DSN in configuration under `sentry` key.
If the DSN is not set then sentry is not enabled.

1. Create a new Sentry project (with same name as your service name) and copy the DSN from project settings
2. If this is the initial setup from backstage you can enter this value in backstage and skip next steps
3. Set the `SENTRY_DSN` to the helm `values-{env}.yaml` file. Note that you can use same DSN for all
   environments.
4. Follow the [ Source Context ](#source-context) to enable sentry code upload.


### Environments

You can enable sentry both on staging and production by passing the same DSN. The events have a `environment` tag
attached that you can use to filter events.

## Usage

By default, Sentry captures the following events:

- Exceptions
- Logs with level error and warning

You can use the
[static API](https://docs.sentry.io/platforms/java/guides/logback/usage/)
to capture custom messages.

### Usage in Coroutines

For setting custom context inside coroutines use the
[SentryContext](https://docs.sentry.io/platforms/java/enriching-events/scopes/#kotlin-coroutines)
to make sure that it has the correct sentry context.


## Additional Config

You can add additional configuration by following
[ this page ](https://docs.sentry.io/platforms/java/guides/logback/configuration).
We use environment variables to set configurations.

### Source Context

To enable [ sentry source context feature ](https://docs.sentry.io/platforms/java/guides/logback/source-context/)
The source code must be uploaded to sentry. This is done via a Gradle plugin.

To enable this upload you need to set the `SENTRY_AUTH_TOKEN` in environment variables before running Gradle
build. This is disabled by default, if you want to use this you need to provide this value in the `Test & Build` step of
GitHub action.

## Setting up Alerts

You can set up alerts in sentry and get notifications in Slack & Pager Duty.

1. Go to https://flexport.sentry.io/alerts/rules/
2. Create a new rule for your project
3. Select the notification channel you want. For slack integration, you need to create a public channel and give the
   channel name in alert
