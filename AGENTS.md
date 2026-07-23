# Project Rules

## Pull Requests

- **When creating a stacked PR, include `depends on #N` in the PR description** (where `#N` is the parent PR it stacks on). This makes the PR dependency gating action (`.github/workflows/check-dependencies.yml`) block the child from merging until the parent is merged. One `depends on` line per parent dependency. Keep the list up to date if the chain changes.
- **Label PRs for release notes.** Release notes are drafted by release-drafter from PR titles grouped by label (`feature`/`enhancement`, `fix`/`bug`, `chore`, `build`, `breaking`). Any PR that changes published Maven coordinates or otherwise breaks consumers must be labelled `breaking` and state the migration in its body.

## Releasing

- Published to Maven Central under group id `bz.stub.truth`. The version in the root `pom.xml` is the source of truth: merging a non-`-SNAPSHOT` version to `master` publishes a release. See [RELEASING.md](RELEASING.md) for the full process and one-time setup.
