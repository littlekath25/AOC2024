import { Github, Context } from "./types";

import comment from "./common/comment";
import findLastRanWorkflow from "./common/findLastRanWorkflow";
import duration from "./common/duration";
import parseJacocoReport from "./common/parseJacocoReport";

type Env = {
  TIMEOUT_SECONDS?: string;
  MIN_RUNTIME_SECONDS?: string;
  LOG_PERIOD_DURING_WAITING_SECONDS?: string;
  LAST_COMMIT_SHA?: string;
  GITHUB_HEAD_REF?: string;
  GITHUB_REF?: string;
};

type TotalReport = {
  repository: string;
  branch: string;
  coverage_branch: number;
  coverage_line: number;
};

/**
 * This function parses given coverage report and sets environment variables for the coverage metrics.
 *
 * @param {Object} params - The parameters for the function.
 * @param {Github} params.github - The Github object, which has methods to list workflow runs.
 * @param {Context} params.context - The context object, which contains information about the repository.
 * @param {Env} params.env - The env object, which contains the target workflow name and branch name.
 * @param {string} params.env.REPORT - The path to test coverage report
 *
 * @returns {string} Sets environment variables REPOSITORY, BRANCH, COVERAGE_BRANCH and COVERAGE_LINE as percentages (ex: 0.8)
 *
 * @async
 */
const testCoverageMetrics = async ({
  github,
  context,
  env,
}: {
  github: Github;
  context: Context;
  env: Env;
}): Promise<TotalReport> => {
  const reports = await parseJacocoReport(process.env.REPORT);
  const totalReport = reports.reduce(
    (totalReport, report) => {
      totalReport.lines.found += report.lines.found;
      totalReport.lines.hit += report.lines.hit;
      totalReport.branches.found += report.branches.found;
      totalReport.branches.hit += report.branches.hit;
      return totalReport;
    },
    {
      lines: {
        found: 0,
        hit: 0,
      },
      branches: {
        found: 0,
        hit: 0,
      },
    }
  );

  const head_ref = process.env.GITHUB_HEAD_REF || process.env.GITHUB_REF;
  const branch = head_ref.replace("refs/heads/", "");

  return {
    repository: context.repo.repo,
    branch: branch,
    coverage_branch: totalReport.branches.hit / totalReport.branches.found,
    coverage_line: totalReport.lines.hit / totalReport.lines.found,
  };
};

export = testCoverageMetrics;
