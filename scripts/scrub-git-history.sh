#!/usr/bin/env bash
# =============================================================================
# scrub-git-history.sh — GitLab #42 git-history scrub (P1 SECURITY)
# =============================================================================
# Companion to #41 (rotate-secrets.sh). Rotation stops the credentials from
# being useful; history scrub stops the old commit blobs from being readable.
#
# Even after rotation, any git forensic pull can inspect the old blobs and
# fingerprint internal product/feature flags. #42 removes them entirely.
#
# This is DESTRUCTIVE — rewrites every commit's hash. After running + force-
# pushing, every developer MUST re-clone. Coordinate before running.
#
# Usage:
#   ./scripts/scrub-git-history.sh                 # dry-run, prints plan
#   ./scripts/scrub-git-history.sh --plan          # same as no-flag
#   ./scripts/scrub-git-history.sh --execute       # actually scrub a mirror
#                                                   # (still does NOT push)
#   ./scripts/scrub-git-history.sh --execute --push=CONFIRM
#                                                   # mirror force-push
#
# =============================================================================

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

MODE=plan
PUSH=""
for arg in "$@"; do
    case "$arg" in
        --plan)        MODE=plan ;;
        --execute)     MODE=execute ;;
        --push=CONFIRM) PUSH=CONFIRM ;;
        *)             echo "unknown arg: $arg" >&2; exit 1 ;;
    esac
done

# Files known to have ever been tracked with secrets.
# Extend this list if #41 rotation surfaces any more.
PATHS_TO_SCRUB=(
    '.env.bak-1775819071'
    '.env.bak-1776096159'
    'banca/Core/config.json.bak-before-prod-run'
    'banca/BanCaLiteNet/publish/config.json.bak-before-prod-run'
    'deploy.sh.bak-before-prod-run'
    'docker-compose.web.yml.bak-before-prod-run'
    'api-xsmb-today-main/.env'
)
# Globs to also scrub if any future .bak-* files were committed on a branch.
GLOBS_TO_SCRUB=(
    '*.env.bak-*'
    '*.bak-before-prod-run'
)

echo "========================================================================="
echo "  scrub-git-history.sh  —  GitLab #42 ($MODE)"
echo "========================================================================="
echo

echo "## Paths that will be purged from every commit"
for p in "${PATHS_TO_SCRUB[@]}"; do echo "    $p"; done
echo
echo "## Globs that will also be purged"
for g in "${GLOBS_TO_SCRUB[@]}"; do echo "    $g"; done
echo

if [[ $MODE == plan ]]; then
    cat <<'PLAN'
## Plan (read-only mode)

The actual scrub happens on a fresh mirror clone, not on the working tree.
Run with --execute to do the scrub locally; add --push=CONFIRM to force-push
the scrubbed history back to origin. Force-push is destructive:

    - Every commit hash on every branch changes.
    - All open MRs need to be re-rebased on the new history (they may auto-
      reclose; GitLab usually handles this gracefully).
    - Every developer MUST:
        git fetch origin
        git checkout <their-branch>
        git reset --hard origin/<their-branch>
      Any uncommitted work on their fork should be stashed/saved first.

## Pre-flight checklist — all MUST be checked before running with --push

  [ ] #41 rotation complete — every secret in .env.bak-* has been rotated
      at the source (GSC, MoMo, CoinPayments, DB, etc). Without rotation,
      an attacker who already cloned the old repo keeps working secrets.
  [ ] Team has scheduled a short downtime / coordination window — all
      devs know to pause pushes for ~15 minutes while the scrub runs.
  [ ] All currently-open MRs listed + tracked. Each will need a rebase
      after the scrub.
  [ ] A backup of the pre-scrub repo exists off-host:
        git clone --mirror git@gitlab.com:vinhtv6789/sunwinkr.git \
          sunwinkr-PRE-SCRUB-backup-$(date +%Y%m%d)
  [ ] git-filter-repo installed:  pip install git-filter-repo
      (Not the deprecated `git filter-branch`.)

## When you're ready

  ./scripts/scrub-git-history.sh --execute                    # local scrub
  ./scripts/scrub-git-history.sh --execute --push=CONFIRM     # + force-push
PLAN
    exit 0
fi

# --execute path
if ! command -v git-filter-repo >/dev/null 2>&1; then
    echo "ERROR: git-filter-repo not installed. Install with: pip install git-filter-repo" >&2
    exit 1
fi

WORK=/tmp/sunwinkr-scrub-$(date +%s)
echo "## Execute mode — working dir: $WORK"
mkdir -p "$WORK"
cd "$WORK"

echo "  [1/4] Mirror-cloning origin..."
git clone --mirror git@gitlab.com:vinhtv6789/sunwinkr.git sunwinkr-scrub
cd sunwinkr-scrub

PATHS_FILE="$WORK/paths.txt"
: > "$PATHS_FILE"
for p in "${PATHS_TO_SCRUB[@]}"; do echo "$p" >> "$PATHS_FILE"; done
for g in "${GLOBS_TO_SCRUB[@]}"; do echo "glob:$g" >> "$PATHS_FILE"; done

echo "  [2/4] Running git filter-repo..."
git filter-repo --invert-paths --paths-from-file "$PATHS_FILE" --force

echo "  [3/4] Summary of the scrubbed mirror:"
git log --all --pretty=oneline | wc -l | xargs printf '         commits: %s\n'
git log --all --pretty=format:'%H' | head -1 | xargs printf '         new HEAD: %s\n'

if [[ $PUSH != CONFIRM ]]; then
    echo "  [4/4] --push=CONFIRM not set — stopping before force-push."
    echo "         Inspect mirror at: $WORK/sunwinkr-scrub"
    echo "         Then re-run with --execute --push=CONFIRM to push."
    exit 0
fi

echo "  [4/4] Force-pushing scrubbed history..."
git remote add scrubbed git@gitlab.com:vinhtv6789/sunwinkr.git 2>/dev/null || true
git push scrubbed --mirror --force
echo
echo "DONE. Team re-clone required:"
echo "    git fetch --all && git reset --hard origin/<branch>"
