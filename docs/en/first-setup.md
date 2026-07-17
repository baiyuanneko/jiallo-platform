# First Setup

[简体中文](../zh-CN/first-setup.md) | [Back to README](../../README.md)

Every user, even the administrator will **NOT** automatically get access to any LLM Model, Agent and Features.

You must assign the permission to access the resources as the following steps:

1. Goto the admin panel
2. After you added a LLM model config at "System LLM Provider Management", you can whether choose to do "User Group based authorization" or "Role based authorization", or you can use both. Detailed steps are as follows:

## User Group based authorization

1. Goto "User Group Management" in admin panel
2. Add a user group, such as "certificated_users" or any other name you like, and then add users (such as the administrator, yourself) to it.
3. Go to "Model and authorizations" and do authorization to the user group you've created one model by one model when you want the user group to access it.
4. Go to "Agent authorizations" and do the same. Notice that the "LLM Tool Call" refers to the basic conversation and task agent and usually you need to assign it if you want to use Agentic Chat UI.
5. Go to "Function and module authorizations" and do the same.

## Role based authorization

Notice that role cannot be created or edited, while user groups can.

1. Go to "Model and authorizations" and do authorization to the role one model by one model when you want the role to access it.
2. Go to "Agent authorizations" and do the same. Notice that the "LLM Tool Call" refers to the basic conversation and task agent and usually you need to assign it if you want to use Agentic Chat UI.
3. Go to "Function and module authorizations" and do the same.

## Essential setup

1. Goto "System settings" in admin panel
2. Set a "simple task model", which will be the default model for tasks like title generation.
