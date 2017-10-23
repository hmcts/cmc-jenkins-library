package uk.gov.hmcts.cmc.smoketests

class SmokeTests implements Serializable {
  def steps

  SmokeTests(steps) {
    this.steps = steps
  }

  def executeAgainst(applicationUrl) {
    executeAgainst(applicationUrl, 'latest')
  }

  def executeAgainst(applicationUrl, imageVersion) {
    loadDockerComposeFile()
    try {
      startWebdriver()
      executeTestsAgainst(applicationUrl, imageVersion)
      analyseTestResults(applicationUrl)
    } finally {
      stopWebdriver()
    }
  }

  private void loadDockerComposeFile() {
    def file = steps.libraryResource 'uk/gov/hmcts/cmc/smoketests/docker-compose.yml'
    steps.writeFile file: 'docker-compose.yml', text: file
  }

  private void startWebdriver() {
    steps.sh 'docker-compose up -d remote-webdriver'
  }

  private void executeTestsAgainst(applicationUrl, imageVersion) {
    steps.sh """
            mkdir -p output
            export APP_URL=${applicationUrl}
            export SMOKE_TESTS_VERSION=${imageVersion} 
            docker-compose pull smoke-tests
            docker-compose up --no-build --no-deps --no-color smoke-tests
        """
  }

  private void analyseTestResults(applicationUrl) {
    def testExitCode = steps.sh returnStdout: true,
      script: "docker-compose ps -q smoke-tests | xargs docker inspect -f '{{ .State.ExitCode }}'"

    steps.junit allowEmptyResults: true, testResults: 'output/smoke-*-result.xml'

    steps.sh "docker-compose logs --no-color > output/logs.txt"
    steps.archiveArtifacts 'output/logs.txt'

    if (testExitCode.toInteger() > 0) {
      steps.archiveArtifacts 'output/*.png'
      steps.error("Smoke tests failed against ${applicationUrl}")
    }
  }

  private void stopWebdriver() {
    steps.sh 'docker-compose down'
  }
}
