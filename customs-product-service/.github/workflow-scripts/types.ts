const { Octokit } = require("@octokit/rest");
export type Github = typeof Octokit;

import { RestEndpointMethodTypes } from "@octokit/plugin-rest-endpoint-methods";
export type WorkflowRun =
  RestEndpointMethodTypes["actions"]["listWorkflowRunsForRepo"]["response"]["data"]["workflow_runs"][0];
export type Job = RestEndpointMethodTypes["actions"]["listJobsForWorkflowRun"]["response"]["data"]["jobs"][0];

// Context type from https://github.com/actions/toolkit/blob/main/packages/github/src/context.ts
export type Context = { repo: { owner: string; repo: string }; issue: { number: number }; runId: number };

type FileReport = {
  lines: {
    found: number;
    hit: number;
  };
  branches: {
    found: number;
    hit: number;
  };
  missedLines: number[];
};

export type TransformedReport = {
  lines: {
    found: number;
    hit: number;
  };
  branches: {
    found: number;
    hit: number;
  };
  files: {
    [key: string]: FileReport;
  };
};

export type ChangedFile = {
  path: string;
  name: string;
  url: string;
};
