import { Github, Context, TransformedReport, ChangedFile } from "./types";
import parse from "lcov-parse";
import comment from "./common/comment";
import parseJacocoReport from "./common/parseJacocoReport";

type Env = {
  PR_REPORT?: string;
  MAIN_REPORT?: string;
  MIN_COVERAGE?: string;
  UPDATE_COMMENT?: string;
  SIGNIFICANT_DIFF_PERCENTAGE?: string;
  HEAD_COMMIT_SHA?: string;
  BASE_COMMIT_SHA?: string;
  TITLE?: string;
};

/**
 * This function parses given coverage reports and compares them to generate an easy to read comment.
 *
 * @param {Object} params - The parameters for the function.
 * @param {Github} params.github - The Github object, which is based on octokit/rest.js
 * @param {Context} params.context - The context object, which contains information about the repository and the workflow trigger.
 * @param {Env} params.env - The environment object.
 * @param {string} params.env.PR_REPORT - The path for the jacaco report of the pull request.
 * @param {string} params.env.MAIN_REPORT - The path for the jacaco report of the main branch.
 * @param {string} params.env.MIN_COVERAGE - The minimum coverage percentage to determine green or red indicators. Default is 80.
 * @param {string} params.env.UPDATE_COMMENT - The flag to update the comment if it already exists. Default is true.
 * @param {string} params.env.SIGNIFICANT_DIFF_PERCENTAGE - The percentage difference to determine if the coverage difference should be displayed in the comment. Default is 0.5.
 * @param {string} params.env.HEAD_COMMIT_SHA - The head commit sha of the pull request.
 * @param {string} params.env.BASE_COMMIT_SHA - The base commit sha of the pull request or the head commit sha of the main branch.
 * @param {string} params.env.TITLE - The title of the comment. Default is '📋 **Compared with Main Code Coverage Report**\n'.
 *
 * @async
 */
const testCoverageReport = async ({ github, context, env }: { github: Github; context: Context; env: Env }) => {
  const {
    PR_REPORT,
    MAIN_REPORT,
    MIN_COVERAGE,
    UPDATE_COMMENT,
    SIGNIFICANT_DIFF_PERCENTAGE,
    HEAD_COMMIT_SHA,
    BASE_COMMIT_SHA,
    TITLE,
  } = env;

  if (!HEAD_COMMIT_SHA) {
    console.log("Unable to find last commit sha, please add HEAD_COMMIT_SHA environment variable");
    return;
  }

  if (!BASE_COMMIT_SHA) {
    console.log("Unable to find last commit sha, please add BASE_COMMIT_SHA environment variable");
    return;
  }

  const title = TITLE || "📋 **Compared with Main Code Coverage Report**\n";

  const minCoverageStr = MIN_COVERAGE || "80";
  const minCoverage = parseInt(minCoverageStr);

  const significantDiffStr = SIGNIFICANT_DIFF_PERCENTAGE || "0.5";
  const significantDiff = parseFloat(significantDiffStr);

  const updateComment = UPDATE_COMMENT == undefined ? true : UPDATE_COMMENT == "true";

  const reports = await Promise.all([parseJacocoReport(PR_REPORT), parseJacocoReport(MAIN_REPORT)]);
  const prReport = reports[0];
  const mainReport = reports[1];

  const transformReport = (report: parse.LcovFile[]): TransformedReport => {
    return report.reduce(
      (totalReport, report) => {
        totalReport.lines.found += report.lines.found;
        totalReport.lines.hit += report.lines.hit;
        totalReport.branches.found += report.branches.found;
        totalReport.branches.hit += report.branches.hit;

        const lines = report.lines.details.filter((l) => l.hit == 0).map((l) => l.line);

        totalReport.files[report.file] = {
          lines: {
            found: report.lines.found,
            hit: report.lines.hit,
          },
          branches: {
            found: report.branches.found,
            hit: report.branches.hit,
          },
          missedLines: lines,
        };

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
        files: {},
      }
    );
  };

  const formatDiff = (diff: number): string => {
    if (diff < 0 && -diff > significantDiff) {
      return ` **\`${diff.toFixed(2)}%\`** `;
    }
    if (diff > significantDiff) {
      return ` **\`+${diff.toFixed(2)}%\`** `;
    }
    return "";
  };

  const changedFilesResponse = await github.rest.repos.compareCommits({
    base: BASE_COMMIT_SHA,
    head: HEAD_COMMIT_SHA,
    owner: context.repo.owner,
    repo: context.repo.repo,
  });
  const changedFiles: ChangedFile[] = changedFilesResponse.data.files.map((file) => ({
    path: file.filename,
    name: file.filename.split("/").pop(),
    url: file.blob_url,
  }));

  const prCoverage = transformReport(prReport);
  const mainCoverage = transformReport(mainReport);

  const prCoveragePercentage = (prCoverage.lines.hit / prCoverage.lines.found) * 100;
  const mainCoveragePercentage = (mainCoverage.lines.hit / mainCoverage.lines.found) * 100;
  const diff = prCoveragePercentage - mainCoveragePercentage;

  const structure = "|:-|:-|:-:|";
  const overallReport = `| Overall | ${prCoveragePercentage.toFixed(2)}% ${formatDiff(diff)} | ${
    prCoveragePercentage < minCoverage ? "❌" : "✅"
  } |\n${structure}`;

  const filesLine = (file: ChangedFile): string => {
    const reportFileName = file.path.replace("server/src/main/kotlin/", "").replace("server/src/test/kotlin/", "");
    if (prCoverage.files[reportFileName] == null) {
      return null;
    }
    const prCoveragePercentage =
      (prCoverage.files[reportFileName].lines.hit / prCoverage.files[reportFileName].lines.found) * 100;
    var diff = 0;
    if (mainCoverage.files[reportFileName] != null) {
      const mainCoveragePercentage =
        (mainCoverage.files[reportFileName].lines.hit / mainCoverage.files[reportFileName].lines.found) * 100;
      diff = prCoveragePercentage - mainCoveragePercentage;
    }

    return `| [${file.name}](${file.url}) | ${prCoveragePercentage.toFixed(2)}%${formatDiff(diff)}| ${
      prCoveragePercentage < minCoverage ? "❌" : "✅"
    } |`;
  };
  const filesHeader = `| File | Coverage | Status |`;
  var filesContent = changedFiles.map(filesLine);
  filesContent = filesContent.filter((f) => f != null);
  var filesTable = "";
  if (filesContent.length > 0) {
    filesTable = `<details><summary>Files</summary>\n\n${filesHeader}\n${structure}\n${filesContent.join(
      "\n"
    )}\n\n</details>`;
  }

  await comment(
    github,
    context.repo.owner,
    context.repo.repo,
    context.issue.number,
    title,
    `${overallReport}\n${filesTable}`,
    updateComment
  );
};

export = testCoverageReport;
