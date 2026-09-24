#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN is required}"
: "${REPO:?REPO is required}"

publish_release() {
  local version="$1"
  local code="$2"
  local tag="$3"
  local target="$4"
  local run_id="$5"
  local artifact_name="$6"
  local source_commit="$7"
  local summary="$8"

  local dir="$RUNNER_TEMP/release-$version"
  rm -rf "$dir"
  mkdir -p "$dir"

  echo "::group::Download $tag artifact"
  gh run download "$run_id" --repo "$REPO" --name "$artifact_name" --dir "$dir"
  ls -lah "$dir"
  echo "::endgroup::"

  test -f "$dir/TrainPilot-$version.apk"
  test -f "$dir/TrainPilot-$version-source.zip"
  test -f "$dir/SHA256SUMS.txt"
  test -f "$dir/apk-badging.txt"

  (
    cd "$dir"
    sha256sum -c SHA256SUMS.txt
  )

  grep -q "versionCode='$code'" "$dir/apk-badging.txt"
  grep -q "versionName='$version'" "$dir/apk-badging.txt"

  local src_version
  local src_code
  local src_commit
  src_version="$(unzip -p "$dir/TrainPilot-$version-source.zip" SOURCE_VERSION.json | jq -r '.version')"
  src_code="$(unzip -p "$dir/TrainPilot-$version-source.zip" SOURCE_VERSION.json | jq -r '.versionCode')"
  src_commit="$(unzip -p "$dir/TrainPilot-$version-source.zip" SOURCE_VERSION.json | jq -r '.sourceCommit')"
  test "$src_version" = "$version"
  test "$src_code" = "$code"
  test "$src_commit" = "$source_commit"

  git cat-file -e "$target^{commit}"

  if gh api "repos/$REPO/git/ref/tags/$tag" >"$dir/tag-ref.json" 2>/dev/null; then
    local existing
    existing="$(jq -r '.object.sha' "$dir/tag-ref.json")"
    test "$existing" = "$target"
  else
    gh api --method POST "repos/$REPO/git/refs" -f ref="refs/tags/$tag" -f sha="$target" >/dev/null
  fi

  printf '%s\n' \
    "TrainPilot $version" \
    "versionCode: $code" \
    "tag: $tag" \
    "mainTarget: $target" \
    "validatedReleaseGateRun: $run_id" \
    "validatedSourceCommit: $source_commit" \
    "artifact: $artifact_name" \
    "provenance: preserved successful signed Release Gate artifact; not rebuilt for catalog publishing" \
    > "$dir/BUILD_INFO.txt"

  {
    printf '## TrainPilot %s\n\n' "$version"
    printf '%s\n\n' "$summary"
    printf -- '- Android versionCode: `%s`\n' "$code"
    printf -- '- Accepted main target: `%s`\n' "$target"
    printf -- '- Validated artifact source: `%s`\n' "$source_commit"
    printf -- '- Signed Release Gate: run `%s`\n' "$run_id"
    printf -- '- The APK and source bundle are the preserved outputs of the successful Release Gate; they were not rebuilt for this catalog update.\n'
  } > "$dir/RELEASE_NOTES.md"

  local files=(
    "$dir/TrainPilot-$version.apk"
    "$dir/TrainPilot-$version-source.zip"
    "$dir/SHA256SUMS.txt"
    "$dir/apk-badging.txt"
    "$dir/BUILD_INFO.txt"
  )

  if gh release view "$tag" --repo "$REPO" >/dev/null 2>&1; then
    gh release upload "$tag" "${files[@]}" --repo "$REPO" --clobber
    gh release edit "$tag" --repo "$REPO" --title "TrainPilot $version" --notes-file "$dir/RELEASE_NOTES.md"
  else
    gh release create "$tag" "${files[@]}" --repo "$REPO" --verify-tag --title "TrainPilot $version" --notes-file "$dir/RELEASE_NOTES.md"
  fi
}

publish_release "1.7.0" "2659" "v1.7.0" "0eef2a30bf12330163896bfc4727b75e99b883b9" "35900526855" "TrainPilot-35900526855-b87614e8b7b1d510d2cb047d10d35caf8336de95" "b87614e8b7b1d510d2cb047d10d35caf8336de95" "Phase 3: Journal polish and the accepted compact/expandable Journal experience, including the Phase 3 Journal and Statistics refinements."
publish_release "1.7.1" "2660" "v1.7.1" "6bb26096b155d9a5c6ce682edf8c76a21cfe0fc7" "35904134547" "TrainPilot-35904134547-38c8fe17fd1c15c9b8e34ab3810a36a8043112a3" "38c8fe17fd1c15c9b8e34ab3810a36a8043112a3" "Phase 4: Coach full exercise coverage and next-workout summary improvements."
publish_release "1.7.2" "2661" "v1.7.2" "237bf6b95e57672a516e2627a995fc7011df5065" "36008072182" "TrainPilot-36008072182-569e896bb01574a875579368c5a5c354dbba0b5d" "569e896bb01574a875579368c5a5c354dbba0b5d" "Phase 5: Health data correctness, shared Home/Health Mai állapot, global theme-aware chevrons and the phone-approved final chevron follow-up."
publish_release "1.7.3" "2662" "v1.7.3" "f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a" "36015162379" "TrainPilot-36015162379-cd968f80312d45bea42536caab7c322cc029c0fd" "cd968f80312d45bea42536caab7c322cc029c0fd" "Phase 6: native workout-camera reliability and stable inline Journal Health refresh without the transient Health Connect loading flash."

gh release edit "v1.7.3" --repo "$REPO" --latest
