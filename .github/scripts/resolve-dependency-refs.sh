#!/usr/bin/env bash

set -euo pipefail

if (( $# < 3 || $# % 2 == 0 )); then
  echo "Usage: $0 SOURCE_DIRECTORY OUTPUT_PREFIX DEPENDENCY_REPOSITORY [...]" >&2
  exit 2
fi

source_directory=$1
shift

source_branch=${SOURCE_BRANCH:?SOURCE_BRANCH must name the branch being tested}
integration_branch=${INTEGRATION_BRANCH:-staging}
root_branch=${ROOT_BRANCH:-master}

declare -a candidates=()
declare -A seen_candidates=()

add_candidate() {
  local branch=$1

  if [[ -n $branch && -z ${seen_candidates[$branch]+yes} ]]; then
    candidates+=("$branch")
    seen_candidates[$branch]=1
  fi
}

source_ref=HEAD
if git -C "$source_directory" show-ref --verify --quiet "refs/remotes/origin/${source_branch}"; then
  source_ref="refs/remotes/origin/${source_branch}"
elif git -C "$source_directory" show-ref --verify --quiet "refs/heads/${source_branch}"; then
  source_ref="refs/heads/${source_branch}"
fi
git -C "$source_directory" rev-parse --verify "${source_ref}^{commit}" >/dev/null

add_candidate "$source_branch"

# Branch creation has no durable metadata in Git. Branch tips that still occur
# on the source branch's first-parent history are the reliable part of the
# stack. Sort them by their distance from the source tip.
declare -A first_parent_distance=()
distance=0
root_ref=
if git -C "$source_directory" show-ref --verify --quiet "refs/remotes/origin/${root_branch}"; then
  root_ref="refs/remotes/origin/${root_branch}"
elif git -C "$source_directory" show-ref --verify --quiet "refs/heads/${root_branch}"; then
  root_ref="refs/heads/${root_branch}"
fi

if [[ -z $root_ref ]]; then
  echo "Cannot find the root branch '${root_branch}' in ${source_directory}" >&2
  exit 1
fi

reached_root=false
while IFS= read -r commit; do
  first_parent_distance[$commit]=$distance
  ((distance += 1))

  # The root branch is allowed to advance independently. Stop at the first
  # first-parent commit contained in its current history, not at its tip.
  if [[ -n $root_ref ]] &&
    git -C "$source_directory" merge-base --is-ancestor "$commit" "$root_ref"; then
    reached_root=true
    break
  fi
done < <(git -C "$source_directory" rev-list --first-parent "$source_ref")

if [[ $reached_root != true ]]; then
  echo "The first-parent history of '${source_branch}' does not reach '${root_branch}'" >&2
  exit 1
fi

ancestry_candidates=$(mktemp)
trap 'rm -f "$ancestry_candidates"' EXIT

while IFS=$'\t' read -r ref_name object_name; do
  branch=${ref_name#origin/}

  [[ $ref_name == origin || $branch == HEAD ]] && continue
  [[ $branch == "$source_branch" ]] && continue
  [[ $branch == "$integration_branch" ]] && continue
  [[ $branch == "$root_branch" ]] && continue
  [[ -n ${first_parent_distance[$object_name]+yes} ]] || continue

  printf '%s\t%s\n' "${first_parent_distance[$object_name]}" "$branch" \
    >> "$ancestry_candidates"
done < <(
  git -C "$source_directory" for-each-ref \
    --format='%(refname:short)%09%(objectname)' \
    refs/remotes/origin
)

while IFS=$'\t' read -r _ branch; do
  add_candidate "$branch"
done < <(sort -k1,1n -k2,2 -u "$ancestry_candidates")

if [[ $source_branch != "$root_branch" ]]; then
  add_candidate "$integration_branch"
fi
add_candidate "$root_branch"

printf -v candidate_chain '%s -> ' "${candidates[@]}"
candidate_chain=${candidate_chain% -> }
echo "Dependency branch candidates: ${candidate_chain}"

while (( $# )); do
  prefix=$1
  repository=$2
  shift 2

  if [[ $repository == *://* || $repository == /* ]]; then
    url=$repository
  else
    url="https://github.com/${repository}.git"
  fi

  default_branch=$(
    git ls-remote --symref "$url" HEAD |
      awk '/^ref:/ {sub("refs/heads/", "", $2); print $2; exit}'
  )
  if [[ -z $default_branch ]]; then
    echo "Cannot determine the default branch for ${repository}" >&2
    exit 1
  fi

  declare -A dependency_branches=()
  while IFS=$'\t' read -r _ ref; do
    dependency_branches[${ref#refs/heads/}]=1
  done < <(git ls-remote --heads "$url")

  selected=
  for branch in "${candidates[@]}"; do
    if [[ -n ${dependency_branches[$branch]+yes} ]]; then
      selected=$branch
      break
    fi
  done

  if [[ -z $selected ]]; then
    echo "None of the candidate branches exists in ${repository}" >&2
    exit 1
  fi

  echo "Using '${selected}' for ${repository}"
  {
    echo "${prefix}_ref=${selected}"
    echo "${prefix}_default=${default_branch}"
  } >> "${GITHUB_OUTPUT:?GITHUB_OUTPUT is not set}"
done
