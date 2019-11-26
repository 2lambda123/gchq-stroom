```sh
git pull
git clean -dfx
mvn versions:set -DnewVersion=5.x.y -DgenerateBackupPoms=false
git commit -a -m "Updated version to v5.x.y"
gp
mvn clean install -U
```
Manually create a release in GitHub, attaching the distribution zip, tagging with the new version
