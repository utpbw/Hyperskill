# Syncing `main` with `utpbw-fitness-traker-api`

To make the `main` branch match the state of the `utpbw-fitness-traker-api` branch, you can follow these Git commands:

1. Ensure you have the latest commits for both branches:
   ```bash
   git fetch origin main utpbw-fitness-traker-api
   ```

2. Move your local `main` branch pointer to the other branch:
   ```bash
   git checkout main
   git reset --hard origin/utpbw-fitness-traker-api
   ```

3. Push the updated branch (force push if the remote `main` must be overwritten):
   ```bash
   git push --force origin main
   ```

Alternatively, push the other branch directly onto `main` without checking it out locally:

```bash
git push origin utpbw-fitness-traker-api:main
```

> ⚠️ Force pushing rewrites the remote history. Confirm with your team before using it.

