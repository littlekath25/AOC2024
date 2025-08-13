import { Github, Context, WorkflowRun } from "./types";

import findLastRanWorkflow from "./common/findLastRanWorkflow";
import duration from "./common/duration";

type Env = {
  TIMEOUT_SECONDS?: string;
  MIN_RUNTIME_SECONDS?: string;
  LOG_PERIOD_DURING_WAITING_SECONDS?: string;
  LAST_COMMIT_SHA?: string;
  GITHUB_HEAD_REF?: string;
  GITHUB_REF?: string;
};

type DataDogMetric = {
  type: string;
  name: string;
  value: string;
  host: string;
  tags: string[];
};

/**
 * This function finds the workflows runs in the current branch and generates the duration metrics compatible with masci/datadog@v1.
 * @param {Object} params - The parameters for the function.
 * @param {Github} params.github - The Github object, which is based on octokit/rest.js
 * @param {Context} params.context - The context object, which contains information about the repository and the workflow trigger.
 * @param {Env} params.env - The environment object.
 * @param {string} params.env.TIMEOUT_SECONDS - The time in seconds to wait for the workflows to be completed. Default is 1200.
 * @param {string} params.env.MIN_RUNTIME_SECONDS - The minimum runtime in seconds to log the duration. Default is 3.
 * @param {string} params.env.LOG_PERIOD_DURING_WAITING_SECONDS - The time in seconds to log the waiting message. Default is 180.
 * @param {string} params.env.LAST_COMMIT_SHA - The commit SHA to find the workflow runs. Default is the last commit SHA.
 * @param {string} params.env.GITHUB_HEAD_REF - Current branch name.
 * @param {string} params.env.GITHUB_REF - The branch name that triggered the workflow. Only used if GITHUB_HEAD_REF is not provided.
 *
 * @returns {DataDogMetric[]} - The list of metrics that is compatible with DataDog APIs
 *
 * @async
 */
const pipelineDurationMetrics = async ({ github, context, env }: { github: Github; context: Context; env: Env }) => {
  const {
    TIMEOUT_SECONDS,
    MIN_RUNTIME_SECONDS,
    LOG_PERIOD_DURING_WAITING_SECONDS,
    LAST_COMMIT_SHA,
    GITHUB_HEAD_REF,
    GITHUB_REF,
  } = env;

  const timeoutStr = TIMEOUT_SECONDS || "1200";
  const timeout = parseInt(timeoutStr) * 1000;
  const minRuntimeStr = MIN_RUNTIME_SECONDS || "3";
  const minRuntime = parseInt(minRuntimeStr);
  const logPeriodStr = LOG_PERIOD_DURING_WAITING_SECONDS || "180";
  const logPeriod = parseInt(logPeriodStr) * 1000;

  const commit = LAST_COMMIT_SHA;
  const head_ref = GITHUB_HEAD_REF || GITHUB_REF;
  const branch = head_ref.replace("refs/heads/", "");

  console.log("Waiting for workflows to be completed");
  const startedPolling = Date.now();
  let workflowRuns: WorkflowRun[] = [];
  while (startedPolling + timeout > Date.now()) {
    const workflowRunsResponse = await github.rest.actions.listWorkflowRunsForRepo({
      owner: context.repo.owner,
      repo: context.repo.repo,
      head_sha: commit,
    });
    workflowRuns = workflowRunsResponse.data.workflow_runs;
    if (
      workflowRuns.every(
        (workflowRun) => workflowRun.id == context.runId || ["queued", "completed"].includes(workflowRun.status)
      )
    ) {
      break;
    }
    await new Promise((resolve) => setTimeout(resolve, 1000));
    if ((Date.now() - startedPolling) % logPeriod == 0) {
      console.log("Waiting for workflows to be completed");
      console.log(workflowRuns);
    }
  }
  if (workflowRuns.length == 0) {
    console.log("Timed out while waiting for workflow to be completed");
    return;
  }
  let metrics = (
    await Promise.all(
      workflowRuns.flatMap(async (workflowRun) => {
        let workflowMetric = {
          workflowName: workflowRun.name,
          jobName: null,
          stepName: null,
          conclusion: workflowRun.conclusion,
          status: workflowRun.status,
          runtime: duration(workflowRun.created_at, workflowRun.updated_at),
          labels: [],
        };
        const jobsResponse = await github.rest.actions.listJobsForWorkflowRun({
          owner: context.repo.owner,
          repo: context.repo.repo,
          run_id: workflowRun.id,
        });
        const jobs = jobsResponse.data.jobs;
        return [workflowMetric].concat(
          jobs
            .flatMap((job) =>
              [
                {
                  workflowName: workflowRun.name,
                  jobName: job.name,
                  stepName: null,
                  conclusion: job.conclusion,
                  status: job.status,
                  runtime: duration(job.started_at, job.completed_at),
                  labels: job.labels,
                },
              ].concat(
                job.steps.map((step) => ({
                  workflowName: workflowRun.name,
                  jobName: job.name,
                  stepName: step.name,
                  conclusion: step.conclusion,
                  status: step.status,
                  runtime: duration(step.started_at, step.completed_at),
                  labels: job.labels,
                }))
              )
            )
            .flat()
        );
      })
    )
  ).flat();
  // Only log significant runtimes to save costs
  metrics = metrics.filter((m) => m.runtime > minRuntime);

  return metrics.map((metric) => ({
    type: "gauge",
    name: "github.workflow.duration",
    value: `${metric.runtime}`,
    host: context.repo.owner,
    tags: [
      `repository:${context.repo.repo}`,
      `branch:${branch}`,
      `workflow_name:${metric.workflowName}`,
      metric.status && `status:${metric.status}`,
      metric.jobName && `job_name:${metric.jobName}`,
      metric.stepName && `step_name:${metric.stepName}`,
    ].filter((tag) => !!tag),
  }));
};

export = pipelineDurationMetrics;
