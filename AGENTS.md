# Project Rules

## Pull Requests

- **When creating a stacked PR, include `depends on #N` in the PR description** (where `#N` is the parent PR it stacks on). This makes the PR dependency gating action (`.github/workflows/check-dependencies.yml`) block the child from merging until the parent is merged. One `depends on` line per parent dependency. Keep the list up to date if the chain changes.
