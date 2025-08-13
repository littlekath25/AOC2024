import { WorkflowRun, Github, Context } from "./types";
import findLastRanWorkflow from "./common/findLastRanWorkflow";

type Env = { TARGET_WORKFLOW_NAME?: string; GITHUB_WORKFLOW?: string; BRANCH_NAME?: string; GITHUB_HEAD_REF?: string };

/**
 * This function finds the ID of the last workflow run of a specific workflow or the workflow that triggered this script
 * in github actions
 *
 * @param {Object} params - The parameters for the function.
 * @param {Github} params.github - The Github object, which is based on octokit/rest.js
 * @param {Context} params.context - The context object, which contains information about the repository and the workflow trigger.
 * @param {Env} params.env - The environment object.
 * @param {string} params.env.TARGET_WORKFLOW_NAME - The name of the target workflow. If this is not provided, the function will find the last run of the current workflow.
 * @param {string} params.env.GITHUB_WORKFLOW - The name of the current workflow. Only used if TARGET_WORKFLOW_NAME is not provided.
 * @param {string} params.env.BRANCH_NAME - The name of the branch that workflow ran. If this is not provided, the function will default to current branch (GITHUB_HEAD_REF) and if that is also not found it will default to 'main'.
 * @param {string} params.env.GITHUB_HEAD_REF - Current branch name. Only used if BRANCH_NAME is not provided. If both are not provided, the function will default to 'main'.
 *
 * @returns {number | null} The ID of the last run of the target workflow or null if not found.
 *
 * @async
 */
const findLastRunId = async ({
  github,
  context,
  env,
}: {
  github: Github;
  context: Context;
  env: Env;
}): Promise<number | null> => {
  const { TARGET_WORKFLOW_NAME, BRANCH_NAME, GITHUB_WORKFLOW, GITHUB_HEAD_REF } = env;

  const workflowName = TARGET_WORKFLOW_NAME || GITHUB_WORKFLOW;

  if (workflowName == null) {
    console.log("No workflow name or current workflow found");
    return null;
  }

  const prevWorkflowRun = await findLastRanWorkflow(
    github,
    context.repo.owner,
    context.repo.repo,
    BRANCH_NAME || GITHUB_HEAD_REF || "main",
    workflowName
  );

  if (prevWorkflowRun == null) {
    console.log(`No previous run found for ${workflowName}`);
    return null;
  }
  return prevWorkflowRun.id;
};

export = findLastRunId;
