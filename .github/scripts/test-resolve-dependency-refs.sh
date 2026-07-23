#!/usr/bin/env bash

set -euo pipefail

script_directory=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
resolver=${1:-"${script_directory}/resolve-dependency-refs.sh"}
test_directory=$(mktemp -d)
source_repository="${test_directory}/source"

git init --quiet --initial-branch=master "$source_repository"
git -C "$source_repository" config user.name "CI Resolver Test"
git -C "$source_repository" config user.email "ci-resolver@example.invalid"

commit() {
  local message=$1
  git -C "$source_repository" commit --quiet --allow-empty -m "$message"
}

commit "master base"

git -C "$source_repository" switch --quiet -c staging
commit "staging base"

git -C "$source_repository" switch --quiet -c lmsousa
commit "branch A"
lmsousa_tip=$(git -C "$source_repository" rev-parse HEAD)

git -C "$source_repository" switch --quiet -c vitest
commit "branch B"
vitest_tip=$(git -C "$source_repository" rev-parse HEAD)

git -C "$source_repository" switch --quiet -c java-deprecation
commit "branch C"
java_deprecation_tip=$(git -C "$source_repository" rev-parse HEAD)

git -C "$source_repository" switch --quiet -c workflow-fix
commit "current branch"
workflow_fix_tip=$(git -C "$source_repository" rev-parse HEAD)

# Both long-lived branches advance after the stack was created.
git -C "$source_repository" switch --quiet staging
commit "advanced staging"
staging_tip=$(git -C "$source_repository" rev-parse HEAD)

git -C "$source_repository" switch --quiet master
commit "advanced master"
master_tip=$(git -C "$source_repository" rev-parse HEAD)

# Simulate a later push after the workflow event was created. Resolution must
# use the detached event commit, never the newer remote source branch tip.
git -C "$source_repository" switch --quiet workflow-fix
commit "post-event parent branch"
later_parent_tip=$(git -C "$source_repository" rev-parse HEAD)
git -C "$source_repository" branch later-parent
commit "post-event source update"
post_event_source_tip=$(git -C "$source_repository" rev-parse HEAD)
git -C "$source_repository" switch --quiet --detach "$workflow_fix_tip"

for branch in workflow-fix java-deprecation vitest lmsousa staging master; do
  branch_variable=${branch//-/_}_tip
  git -C "$source_repository" update-ref \
    "refs/remotes/origin/${branch}" "${!branch_variable}"
done
git -C "$source_repository" update-ref \
  refs/remotes/origin/workflow-fix "$post_event_source_tip"
git -C "$source_repository" update-ref \
  refs/remotes/origin/later-parent "$later_parent_tip"

make_dependency() {
  local name=$1
  shift
  local repository="${test_directory}/${name}.git"

  git clone --quiet --bare "$source_repository" "$repository"
  while IFS= read -r ref; do
    git -C "$repository" update-ref -d "$ref"
  done < <(git -C "$repository" for-each-ref --format='%(refname)' refs/heads)

  git -C "$repository" update-ref refs/heads/master "$master_tip"
  for branch in "$@"; do
    branch_variable=${branch//-/_}_tip
    git -C "$repository" update-ref "refs/heads/${branch}" "${!branch_variable}"
  done
  git -C "$repository" symbolic-ref HEAD refs/heads/master
  echo "$repository"
}

current_dependency=$(make_dependency current workflow-fix lmsousa)
middle_dependency=$(make_dependency middle java-deprecation lmsousa)
older_dependency=$(make_dependency older vitest lmsousa)
integration_dependency=$(make_dependency integration staging)
root_dependency=$(make_dependency root)

output_file="${test_directory}/github-output"
log_file="${test_directory}/resolver.log"
SOURCE_BRANCH=workflow-fix GITHUB_OUTPUT="$output_file" \
  bash "$resolver" "$source_repository" \
    current "$current_dependency" \
    middle "$middle_dependency" \
    older "$older_dependency" \
    integration "$integration_dependency" \
    root "$root_dependency" |
  tee "$log_file"

grep -Fqx \
  "Dependency branch candidates: workflow-fix -> java-deprecation -> vitest -> lmsousa -> staging -> master" \
  "$log_file"
if grep -Fq "later-parent" "$log_file"; then
  echo "Resolver used a branch created after the workflow event" >&2
  exit 1
fi
grep -Fqx "current_ref=workflow-fix" "$output_file"
grep -Fqx "middle_ref=java-deprecation" "$output_file"
grep -Fqx "older_ref=vitest" "$output_file"
grep -Fqx "integration_ref=staging" "$output_file"
grep -Fqx "root_ref=master" "$output_file"

echo "All dependency ref resolver tests passed"
