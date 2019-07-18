# AWS assume role plugin for Bamboo

Automatically assume role and inject AWS credentials to all your tasks in given job.

## How it works

Define your custom capability of your node with AWS role ARN and credentials which should be used for role assumption.
Plugin integrates with AWS Credentials configuration in Shared credentials and standard AWS credential providers 
(environment variables, EC2 roles etc).

AWS credentials are injected as task context variables:
- aws.accessKey.password
- aws.secretAccessKey.password
- aws.sessionToken.password

Additionally to simplify integration with other tools there is a combined variable:
`aws.env.password` in format of `AWS_ACCESS_KEY_ID=${aws.accessKey.password} AWS_SECRET_ACCESS_KEY=${aws.secretAccessKey.password} AWS_SESSION_TOKEN=${aws.sessionToken.password}`

That variable can be useful in custom tasks as an environment variable configuration.
Unfortunately it's not possible to inject AWS_* variables directly.  

## Screenshots
