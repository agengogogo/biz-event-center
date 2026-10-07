# AGENTS.md

## What this repo is

Greenfield project scaffolded for **OpenSpec** (spec-driven development) plus the
OpenCode `opsx-*` workflows. There is **no application code yet**: the only
non-tooling file is a placeholder root `package.json` (no scripts, no deps,
`npm test` just exits 1). Do not infer a framework or stack from the name; nothing
has been chosen. This directory is also **not a git repository** yet.

## Workflow: use OpenSpec, not ad-hoc edits

Planning happens first, implementation second, and they are deliberately separate
workflows. The repo-local OpenCode skills/commands encode the rules:

- `/opsx-propose` (skill `openspec-propose`) — creates a change and all planning
  artifacts (proposal, specs delta, design, tasks). **Planning only**: never edit
  project code in this workflow.
- `/opsx-apply` (skill `openspec-apply-change`) — implements tasks and checks them
  off. Must be started explicitly in a new request; do not roll from propose into apply.
- `/opsx-archive`, `/opsx-sync`, `/opsx-update`, `/opsx-explore` — finalize, sync
  specs, revise artifacts, and explore respectively.

Authoritative instructions live in `.opencode/skills/openspec-*/SKILL.md` and
`.opencode/commands/opsx-*.md`; read them before running a workflow. The skills
drive the external `openspec` CLI (`openspec status|instructions|new change|...`);
the CLI's JSON output is the source of truth over any path assumptions in prose.

## Layout

- `openspec/` — planning home. `specs/` holds main specs (currently only `.gitkeep`),
  `changes/` holds active changes, `changes/archive/` archived ones. `config.yaml`
  holds project context/rules (currently all commented out).
- `.opencode/` — OpenCode tooling config. `package.json` + `node_modules` here are
  tooling, gitignored; don't hand-edit generated skill/command files.
- `AGENTS.md` — this file.

## Gotchas

- Do not create `openspec/` content by hand or run `openspec init` unless the user
  asks; a missing/`null` root is a stop-and-ask branch, not a signal to scaffold.
- OpenSpec capability paths are directory paths under `openspec/specs/`; preserve
  an existing capability's full path for new changes.
