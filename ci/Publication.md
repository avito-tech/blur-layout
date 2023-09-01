To publish a new version of the library:
1. Update `projectVersion` in root `gradle.properties` on the main branch
2. Create tag by `git tag <version>` and push it with `git push --tags`
3. Run [publish configuration](http://links.k.avito.ru/cJA) from a newly created tag
4. Wait 15m-1h for artifacts to become available in Maven Central
