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

declare -a dependency_prefixes=()
declare -a dependency_repositories=()
while (( $# )); do
  dependency_prefixes+=("$1")
  dependency_repositories+=("$2")
  shift 2
done

repository_url() {
  local repository=$1
  if [[ $repository == *://* || $repository == /* ]]; then
    echo "$repository"
  else
    echo "https://github.com/${repository}.git"
  fi
}

evidence_directory=$(mktemp -d)
declare -a evidence_paths=("$source_directory")
declare -a evidence_ref_prefixes=("refs/remotes/origin/")
declare -A cloned_paths=()

clone_evidence_repository() {
  local repository=$1

  if [[ -n ${cloned_paths[$repository]+yes} ]]; then
    return
  fi

  local clone_path="${evidence_directory}/repository-${#cloned_paths[@]}.git"
  git clone --quiet --bare --filter=blob:none \
    "$(repository_url "$repository")" "$clone_path"
  cloned_paths[$repository]=$clone_path
  evidence_paths+=("$clone_path")
  evidence_ref_prefixes+=("refs/heads/")
}

for repository in "${dependency_repositories[@]}"; do
  clone_evidence_repository "$repository"
done

if [[ -n ${EVIDENCE_REPOSITORIES:-} ]]; then
  read -r -a additional_evidence <<< "$EVIDENCE_REPOSITORIES"
  for repository in "${additional_evidence[@]}"; do
    clone_evidence_repository "$repository"
  done
fi

# A moved branch is relevant only when its name is shared by repositories.
# Count each repository once so private one-off branches do not pollute the
# global candidate set.
declare -A branch_presence=()
for ((repository_index = 0; repository_index < ${#evidence_paths[@]}; repository_index++)); do
  repository_path=${evidence_paths[$repository_index]}
  ref_prefix=${evidence_ref_prefixes[$repository_index]}
  while IFS= read -r ref; do
    branch=${ref#"$ref_prefix"}
    [[ $branch == HEAD ]] && continue
    branch_presence[$branch]=$(( ${branch_presence[$branch]:-0} + 1 ))
  done < <(
    git -C "$repository_path" for-each-ref \
      --format='%(refname)' "${ref_prefix%/}"
  )
done

declare -a candidates=()
declare -A seen_candidates=()
declare -A candidate_proven=()

add_candidate() {
  local branch=$1
  local proven=${2:-true}
  if [[ -n $branch && -z ${seen_candidates[$branch]+yes} ]]; then
    seen_candidates[$branch]=${#candidates[@]}
    candidate_proven[${#candidates[@]}]=$proven
    candidates+=("$branch")
  elif [[ -n $branch && $proven == true ]]; then
    candidate_proven[${seen_candidates[$branch]}]=true
  fi
}

add_candidate "$source_branch"

collect_candidates() {
  local repository_path=$1
  local ref_prefix=$2
  local start_ref=$3
  local required=$4
  local root_ref="${ref_prefix}${root_branch}"

  if ! git -C "$repository_path" rev-parse --verify "${start_ref}^{commit}" >/dev/null 2>&1; then
    if [[ $required == true ]]; then
      echo "Cannot resolve the workflow commit in ${repository_path}" >&2
      exit 1
    fi
    return
  fi
  if ! git -C "$repository_path" rev-parse --verify "${root_ref}^{commit}" >/dev/null 2>&1; then
    echo "Cannot find root branch '${root_branch}' in ${repository_path}" >&2
    exit 1
  fi

  declare -A distance_by_commit=()
  local distance=0
  local reached_root=false
  local commit
  while IFS= read -r commit; do
    distance_by_commit[$commit]=$distance
    ((distance += 1))
    if git -C "$repository_path" merge-base --is-ancestor "$commit" "$root_ref"; then
      reached_root=true
      break
    fi
  done < <(git -C "$repository_path" rev-list --first-parent "$start_ref")

  if [[ $reached_root != true ]]; then
    if [[ $required == true ]]; then
      echo "The workflow commit's first-parent history does not reach '${root_branch}'" >&2
      exit 1
    fi
    return
  fi
  local root_distance=$((distance - 1))

  local ordered_refs="${evidence_directory}/candidate-refs-${#candidates[@]}-${distance}"
  : > "$ordered_refs"
  local ref object branch
  while IFS=$'\t' read -r ref object; do
    branch=${ref#"$ref_prefix"}
    [[ $branch == HEAD ]] && continue
    [[ $branch == "$source_branch" ]] && continue
    [[ $branch == "$integration_branch" ]] && continue
    [[ $branch == "$root_branch" ]] && continue

    if [[ -n ${distance_by_commit[$object]+yes} ]]; then
      printf '%s\t%s\ttrue\n' "${distance_by_commit[$object]}" "$branch" >> "$ordered_refs"
      continue
    fi

    # The branch may have advanced after a child was created. Its merge-base
    # still identifies the old tip on the event's stack. Requiring the name in
    # multiple repositories and a fork point newer than the root boundary
    # avoids treating every branch from master as part of this stack.
    [[ ${branch_presence[$branch]:-0} -ge 2 ]] || continue
    merge_base=$(git -C "$repository_path" merge-base "$start_ref" "$ref" 2>/dev/null || true)
    [[ -n $merge_base && -n ${distance_by_commit[$merge_base]+yes} ]] || continue
    [[ ${distance_by_commit[$merge_base]} -lt $root_distance ]] || continue
    printf '%s\t%s\tfalse\n' "${distance_by_commit[$merge_base]}" "$branch" >> "$ordered_refs"
  done < <(
    git -C "$repository_path" for-each-ref \
      --format='%(refname)%09%(objectname)' "${ref_prefix%/}"
  )

  while IFS=$'\t' read -r _ branch proven; do
    add_candidate "$branch" "$proven"
  done < <(sort -k1,1n -k2,2 -u "$ordered_refs")
}

# HEAD is the immutable event commit. Never substitute the mutable remote tip.
collect_candidates "$source_directory" "refs/remotes/origin/" HEAD true

for ((repository_index = 1; repository_index < ${#evidence_paths[@]}; repository_index++)); do
  repository_path=${evidence_paths[$repository_index]}
  source_ref="refs/heads/${source_branch}"
  if git -C "$repository_path" show-ref --verify --quiet "$source_ref"; then
    collect_candidates "$repository_path" "refs/heads/" "$source_ref" false
  fi
done

if [[ $source_branch != "$root_branch" ]]; then
  add_candidate "$integration_branch"
fi
add_candidate "$root_branch"

printf -v candidate_chain '%s -> ' "${candidates[@]}"
candidate_chain=${candidate_chain% -> }
echo "Dependency branch candidates: ${candidate_chain}"

candidate_count=${#candidates[@]}
declare -A edges=()
declare -A strict_edges=()

add_edge() {
  local newer=$1
  local older=$2
  [[ $newer == "$older" ]] || edges["${newer},${older}"]=1
}

is_first_parent_ancestor() {
  local repository_path=$1
  local older=$2
  local newer=$3
  local commit

  while IFS= read -r commit; do
    [[ $commit == "$older" ]] && return 0
  done < <(git -C "$repository_path" rev-list --first-parent "$newer")
  return 1
}

# The event branch is newest by definition. staging and master are the two
# conventional terminal levels, even when their tips have advanced.
for ((i = 0; i < candidate_count; i++)); do
  branch=${candidates[$i]}
  if [[ $branch != "$source_branch" ]]; then
    add_edge 0 "$i"
  fi
  if [[ $source_branch != "$root_branch" &&
    $branch != "$integration_branch" && $branch != "$root_branch" ]]; then
    integration_index=${seen_candidates[$integration_branch]}
    add_edge "$i" "$integration_index"
  fi
done
root_index=${seen_candidates[$root_branch]}
if [[ $source_branch != "$root_branch" ]]; then
  integration_index=${seen_candidates[$integration_branch]}
  add_edge "$integration_index" "$root_index"
fi

# Each repository contributes only strict first-parent evidence. Equal refs
# are neutral; missing or diverged refs contribute no ordering.
for ((repository_index = 0; repository_index < ${#evidence_paths[@]}; repository_index++)); do
  repository_path=${evidence_paths[$repository_index]}
  ref_prefix=${evidence_ref_prefixes[$repository_index]}

  for ((i = 0; i < candidate_count; i++)); do
    left_branch=${candidates[$i]}
    [[ $left_branch == "$source_branch" || $left_branch == "$integration_branch" || $left_branch == "$root_branch" ]] && continue
    left_ref="${ref_prefix}${left_branch}"
    left_sha=$(git -C "$repository_path" rev-parse --verify "${left_ref}^{commit}" 2>/dev/null || true)
    [[ -n $left_sha ]] || continue

    for ((j = i + 1; j < candidate_count; j++)); do
      right_branch=${candidates[$j]}
      [[ $right_branch == "$source_branch" || $right_branch == "$integration_branch" || $right_branch == "$root_branch" ]] && continue
      right_ref="${ref_prefix}${right_branch}"
      right_sha=$(git -C "$repository_path" rev-parse --verify "${right_ref}^{commit}" 2>/dev/null || true)
      [[ -n $right_sha && $left_sha != "$right_sha" ]] || continue

      if is_first_parent_ancestor "$repository_path" "$right_sha" "$left_sha"; then
        add_edge "$i" "$j"
        strict_edges["${i},${j}"]=1
      elif is_first_parent_ancestor "$repository_path" "$left_sha" "$right_sha"; then
        add_edge "$j" "$i"
        strict_edges["${j},${i}"]=1
      fi
    done
  done
done

# Strict evidence validates an uncertain candidate only when it connects to a
# branch already observed directly on a stack. Propagate that proof through a
# chain of strict first-parent relationships.
proof_changed=true
while [[ $proof_changed == true ]]; do
  proof_changed=false
  for edge in "${!strict_edges[@]}"; do
    newer_index=${edge%,*}
    older_index=${edge#*,}
    if [[ ${candidate_proven[$newer_index]:-false} == true &&
      ${candidate_proven[$older_index]:-false} != true ]]; then
      candidate_proven[$older_index]=true
      proof_changed=true
    fi
  done
done

# Compute transitive closure and reject contradictory evidence.
for ((k = 0; k < candidate_count; k++)); do
  for ((i = 0; i < candidate_count; i++)); do
    [[ -n ${edges["${i},${k}"]+yes} ]] || continue
    for ((j = 0; j < candidate_count; j++)); do
      if [[ -n ${edges["${k},${j}"]+yes} ]]; then
        edges["${i},${j}"]=1
      fi
    done
  done
done

for ((i = 0; i < candidate_count; i++)); do
  if [[ -n ${edges["${i},${i}"]+yes} ]]; then
    echo "Contradictory branch ordering evidence involves '${candidates[$i]}'" >&2
    exit 1
  fi
done

for ((dependency_index = 0; dependency_index < ${#dependency_repositories[@]}; dependency_index++)); do
  prefix=${dependency_prefixes[$dependency_index]}
  repository=${dependency_repositories[$dependency_index]}
  repository_path=${cloned_paths[$repository]}

  default_branch=$(git -C "$repository_path" symbolic-ref --short HEAD)
  default_branch=${default_branch#refs/heads/}

  declare -a available_indices=()
  declare -A available_shas=()
  for ((i = 0; i < candidate_count; i++)); do
    branch=${candidates[$i]}
    sha=$(git -C "$repository_path" rev-parse --verify "refs/heads/${branch}^{commit}" 2>/dev/null || true)
    if [[ -n $sha ]]; then
      available_indices+=("$i")
      available_shas[$i]=$sha
    fi
  done

  declare -a newest_indices=()

  # A merge-base-only branch may be a moved parent or an unrelated sibling.
  # Without strict evidence elsewhere, selecting its distinct commit would be
  # a guess. Equal target SHAs remain harmless aliases.
  for i in "${available_indices[@]}"; do
    [[ ${candidate_proven[$i]:-false} == true ]] && continue
    harmless_uncertain=false
    for j in "${available_indices[@]}"; do
      if [[ ${candidate_proven[$j]:-false} == true &&
        ( ${available_shas[$j]} == "${available_shas[$i]}" ||
          -n ${edges["${j},${i}"]+yes} ) ]]; then
        harmless_uncertain=true
        break
      fi
    done
    if [[ $harmless_uncertain != true ]]; then
      echo "Cannot place shared branch '${candidates[$i]}' in the stack for ${repository}" >&2
      exit 1
    fi
  done

  for i in "${available_indices[@]}"; do
    dominated=false
    for j in "${available_indices[@]}"; do
      if [[ $i != "$j" && -n ${edges["${j},${i}"]+yes} ]]; then
        dominated=true
        break
      fi
    done
    [[ $dominated == true ]] || newest_indices+=("$i")
  done

  if (( ${#newest_indices[@]} == 0 )); then
    echo "None of the candidate branches exists in ${repository}" >&2
    exit 1
  fi

  selected_index=${newest_indices[0]}
  selected_sha=${available_shas[$selected_index]}
  if (( ${#newest_indices[@]} > 1 )); then
    ambiguous=()
    for i in "${newest_indices[@]}"; do
      ambiguous+=("${candidates[$i]}")
      if [[ ${available_shas[$i]} != "$selected_sha" ]]; then
        printf -v ambiguous_list '%s, ' "${ambiguous[@]}"
        ambiguous_list=${ambiguous_list%, }
        echo "Ambiguous newest branches in ${repository}: ${ambiguous_list}" >&2
        exit 1
      fi
    done
  fi

  selected_branch=${candidates[$selected_index]}
  echo "Using '${selected_branch}' (${selected_sha}) for ${repository}"
  {
    echo "${prefix}_ref=${selected_sha}"
    echo "${prefix}_branch=${selected_branch}"
    echo "${prefix}_default=${default_branch}"
  } >> "${GITHUB_OUTPUT:?GITHUB_OUTPUT is not set}"
done
