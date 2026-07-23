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

git -C "$source_repository" switch --quiet lmsousa
commit "advanced parent branch"
advanced_lmsousa_tip=$(git -C "$source_repository" rev-parse HEAD)

git -C "$source_repository" switch --quiet staging
git -C "$source_repository" switch --quiet -c rebased-parent-fixture
commit "rebased parent branch"
rebased_lmsousa_tip=$(git -C "$source_repository" rev-parse HEAD)

git -C "$source_repository" switch --quiet --detach "$lmsousa_tip"
git -C "$source_repository" switch --quiet -c unrelated-sibling
commit "unrelated sibling branch"
unrelated_sibling_tip=$(git -C "$source_repository" rev-parse HEAD)
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
# Clava's vitest and java-deprecation refs are intentionally equal. Another
# repository must provide their strict ordering.
git -C "$source_repository" update-ref \
  refs/remotes/origin/vitest "$java_deprecation_tip"

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
middle_dependency=$(make_dependency middle java-deprecation vitest lmsousa)
older_dependency=$(make_dependency older vitest lmsousa)
integration_dependency=$(make_dependency integration staging)
root_dependency=$(make_dependency root)
equal_dependency=$(make_dependency equal java-deprecation vitest lmsousa)
git -C "$equal_dependency" update-ref \
  refs/heads/vitest "$java_deprecation_tip"
ambiguous_dependency=$(make_dependency ambiguous java-deprecation vitest lmsousa)
git -C "$ambiguous_dependency" update-ref \
  refs/heads/vitest "$staging_tip"
reverse_dependency=$(make_dependency reverse java-deprecation vitest lmsousa)
git -C "$reverse_dependency" update-ref \
  refs/heads/java-deprecation "$vitest_tip"
git -C "$reverse_dependency" update-ref \
  refs/heads/vitest "$java_deprecation_tip"
advanced_dependency=$(make_dependency advanced lmsousa staging)
git -C "$advanced_dependency" update-ref \
  refs/heads/lmsousa "$advanced_lmsousa_tip"
rebased_dependency=$(make_dependency rebased lmsousa staging)
git -C "$rebased_dependency" update-ref \
  refs/heads/lmsousa "$rebased_lmsousa_tip"
sibling_dependency=$(make_dependency sibling unrelated-sibling staging)

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
grep -Fqx "current_branch=workflow-fix" "$output_file"
grep -Fqx "current_ref=${workflow_fix_tip}" "$output_file"
grep -Fqx "middle_branch=java-deprecation" "$output_file"
grep -Fqx "middle_ref=${java_deprecation_tip}" "$output_file"
grep -Fqx "older_branch=vitest" "$output_file"
grep -Fqx "older_ref=${vitest_tip}" "$output_file"
grep -Fqx "integration_branch=staging" "$output_file"
grep -Fqx "integration_ref=${staging_tip}" "$output_file"
grep -Fqx "root_branch=master" "$output_file"
grep -Fqx "root_ref=${master_tip}" "$output_file"

# Equal, unordered names are safe when they resolve to the same target commit.
equal_output="${test_directory}/equal-output"
SOURCE_BRANCH=workflow-fix GITHUB_OUTPUT="$equal_output" \
  bash "$resolver" "$source_repository" \
    equal "$equal_dependency" >/dev/null
grep -Fqx "equal_ref=${java_deprecation_tip}" "$equal_output"

# Diverged candidates with no ordering evidence must fail rather than guess.
if SOURCE_BRANCH=workflow-fix GITHUB_OUTPUT="${test_directory}/ambiguous-output" \
  bash "$resolver" "$source_repository" \
    ambiguous "$ambiguous_dependency" \
    >"${test_directory}/ambiguous.log" 2>&1; then
  echo "Resolver accepted ambiguous, different dependency refs" >&2
  exit 1
fi
grep -Fq "Ambiguous newest branches" "${test_directory}/ambiguous.log"

# Opposite strict orderings from two repositories are contradictory.
if SOURCE_BRANCH=workflow-fix GITHUB_OUTPUT="${test_directory}/contradictory-output" \
  bash "$resolver" "$source_repository" \
    forward "$middle_dependency" \
    reverse "$reverse_dependency" \
    >"${test_directory}/contradictory.log" 2>&1; then
  echo "Resolver accepted contradictory repository evidence" >&2
  exit 1
fi
grep -Fq "Contradictory branch ordering evidence" \
  "${test_directory}/contradictory.log"

# Terminal branch workflows must not create a staging/master ordering cycle.
git -C "$source_repository" switch --quiet --detach "$staging_tip"
SOURCE_BRANCH=staging GITHUB_OUTPUT="${test_directory}/staging-output" \
  bash "$resolver" "$source_repository" \
    integration "$integration_dependency" >/dev/null
grep -Fqx "integration_ref=${staging_tip}" \
  "${test_directory}/staging-output"

git -C "$source_repository" switch --quiet --detach "$master_tip"
SOURCE_BRANCH=master GITHUB_OUTPUT="${test_directory}/master-output" \
  bash "$resolver" "$source_repository" \
    root "$root_dependency" >/dev/null
grep -Fqx "root_ref=${master_tip}" "${test_directory}/master-output"

# A shared parent remains a candidate when its tip advances or is rebased,
# provided its merge-base still identifies this post-master stack.
git -C "$source_repository" switch --quiet --detach "$workflow_fix_tip"
git -C "$source_repository" update-ref \
  refs/remotes/origin/lmsousa "$advanced_lmsousa_tip"
SOURCE_BRANCH=workflow-fix EVIDENCE_REPOSITORIES="$middle_dependency" \
  GITHUB_OUTPUT="${test_directory}/advanced-output" \
  bash "$resolver" "$source_repository" \
    advanced "$advanced_dependency" >/dev/null
grep -Fqx "advanced_ref=${advanced_lmsousa_tip}" \
  "${test_directory}/advanced-output"

git -C "$source_repository" update-ref \
  refs/remotes/origin/lmsousa "$rebased_lmsousa_tip"
SOURCE_BRANCH=workflow-fix EVIDENCE_REPOSITORIES="$middle_dependency" \
  GITHUB_OUTPUT="${test_directory}/rebased-output" \
  bash "$resolver" "$source_repository" \
    rebased "$rebased_dependency" >/dev/null
grep -Fqx "rebased_ref=${rebased_lmsousa_tip}" \
  "${test_directory}/rebased-output"

# A shared sibling with the same fork point is not a proven stack member.
git -C "$source_repository" update-ref \
  refs/remotes/origin/unrelated-sibling "$unrelated_sibling_tip"
if SOURCE_BRANCH=workflow-fix GITHUB_OUTPUT="${test_directory}/sibling-output" \
  bash "$resolver" "$source_repository" \
    sibling "$sibling_dependency" \
    >"${test_directory}/sibling.log" 2>&1; then
  echo "Resolver selected an unproven sibling branch" >&2
  exit 1
fi
grep -Fq "Cannot place shared branch 'unrelated-sibling'" \
  "${test_directory}/sibling.log"

echo "All dependency ref resolver tests passed"
