# Slack notification

## GitHub Workflow
If you're looking to send notifications to a Slack channel, such as a [PR review](https://github.com/flexport/customs-eu-doc-receiver/actions/workflows/slack-notify.yml), this guide will help you set it up. We'll be using [Slack apps](https://api.slack.com/docs).

### 1. Create a Slack app
If you don't already have a Slack app, you can create one [here](https://api.slack.com/apps) following [these](https://flexport.atlassian.net/wiki/spaces/IS/pages/2267775319/Slack+App+-+Security+Review+Guidelines) guidelines. Your request will be reviewed by security ops. Remember, a single Slack app can post messages to multiple channels using bot tokens or webhooks, there is no need to create an app for each channel.

![](images/slack-app-create-new-app.png)

### 2. Bot scopes and permissions
After your app is approved, you can view it [here](https://api.slack.com/apps).

![](images/slack-app-overview.png)

The bot requires certain scopes to post messages to a channel. You can add these scopes in the `OAuth & Permissions` section. The most common ones are `chat:write` and `incoming-webhooks`. **Remember to reinstall the app after updating the permissions/scopes.**

![](images/slack-app-permissions.png)

![](images/slack-app-reinstall.png)

### 3. Send messages with token or webhook
You can send messages to a channel either by a bot token or a webhook. The bot token is more secure and recommended. You can find the bot token in the `OAuth & Permissions` -> `Bot User OAuth Token` section.

![](images/slack-app-bot-token.png)

If you prefer to use a webhook, you can create one in the `Incoming Webhooks` section. Here, you can add a new webhook and select the channel for posting messages.

![](images/slack-app-webhooks.png)

![](images/slack-app-webhooks-channel.png)

### 4. Store the token or url in GitHub secrets
To allow your yaml to access your bot token or webhook URL, save it in your GitHub repository secrets. Navigate to your repository -> `Settings` -> `Secrets and variables` -> `New repository secret`.

![](images/github-repo-new-repository-secret.png)

### 5. Invite the bot to the channel
The last step is to invite the bot to the channel where you want to post messages. You can do this by going to the channel and typing `/invite` and select the app.

![](images/slack-app-invite-bot.png)

### Example Slack Notification in GitHub Actions
Here's an example of how to post a message to a Slack channel every time a PR is opened using [Slack Notify](https://github.com/marketplace/actions/slack-notify). Copy the yaml into your GitHub Actions workflow folder (.github/workflows) and replace `SLACK_WEBHOOK` with your webhook URL.

If you're using the bot token, remove `SLACK_WEBHOOK` and add `SLACK_BOT_TOKEN` and `SLACK_CHANNEL` instead. Remember, both the Slack Channel and Token need to be set for the token to work.

```yaml
on:
  pull_request:
    types: [opened, ready_for_review, reopened]

name: Slack Notification
jobs:
  slackNotification:
    name: Slack Notification
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Slack Notification
        uses: rtCamp/action-slack-notify@v2
        env:
          SLACK_WEBHOOK:
          SLACK_COLOR:
          SLACK_TITLE: 'Review requested :wave:'
          SLACK_MESSAGE: '[](//pull/)'
          SLACK_FOOTER: ''
          MSG_MINIMAL: true
          SLACKIFY_MARKDOWN: true
```

