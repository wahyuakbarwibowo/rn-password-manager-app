---
name: merge
description: Merge current branch into main and clean up
disable-model-invocation: true
---

Merge the current branch into `main`. Follow these steps:

1. Run `git branch --show-current` to check current branch
2. If on `main`, STOP and tell the user there's nothing to merge
3. Run `git status` to check for uncommitted changes — if any, STOP and tell the user to commit first using `/commit`
4. Store the current branch name
5. Run `git checkout main && git pull origin main` to update main
6. Run `git merge <branch-name> --no-ff` to merge with a merge commit
7. If there are merge conflicts, STOP and help the user resolve them
8. Run `git push origin main` to push the merged main
9. Ask the user if they want to delete the feature branch. If yes:
   - `git branch -d <branch-name>` (local)
   - `git push origin --delete <branch-name>` (remote)
10. Run `git log --oneline -5` to confirm the merge
