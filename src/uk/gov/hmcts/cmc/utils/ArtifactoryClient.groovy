package uk.gov.hmcts.cmc.utils

import groovy.json.JsonSlurper

class ArtifactoryClient {

  private steps
  private artifactoryServerUrl

  ArtifactoryClient(steps) {
    this.steps = steps
    this.artifactoryServerUrl = steps.getArtifactoryServer(artifactoryServerID: 'artifactory.reform').getUrl()
  }

  String getLatestImageVersion(String repository, String branch) {
    def response = steps.httpRequest httpMode: 'POST', url: "${artifactoryServerUrl}/api/search/aql",
      requestBody: query(repository, branch), authentication: 'ArtifactoryDeploy'

    def results = new JsonSlurper().parseText(response.content).results

    if (results.empty) {
      return null
    }

    ArtifactoryUtils.extractImageVersionFromPath(results.first().path)
  }

  private String query(String repoName, String branchName) {
    """
    |items.find(
    |  { "repo": "docker-local" },
    |  { "@docker.repoName": "${repoName}" },
    |  { "@build.branch": "${branchName}" }
    |)
    |.include("repo", "path", "name", "created")
    |.sort({"\$desc": ["created"]})
    |.limit(1)
    """.stripMargin()
  }

}
