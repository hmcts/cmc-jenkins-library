package uk.gov.hmcts.cmc.integrationtests

import uk.gov.hmcts.cmc.Team

class IntegrationTests implements Serializable {
  def steps
  def env
  String[] additionalComposeFiles

  def secrets = [
    [$class: 'VaultSecret', path: 'secret/test/cc/payment/api/gov-pay-keys/cmc', secretValues:
      [
        [$class: 'VaultSecretValue', envVar: 'GOV_PAY_AUTH_KEY_CMC', vaultKey: 'value']
      ]
    ],
    [$class: 'VaultSecret', path: 'secret/dev/cmc/notify/integration-tests/test_mode_api_key', secretValues:
      [
        [$class: 'VaultSecretValue', envVar: 'GOV_NOTIFY_API_KEY', vaultKey: 'value']
      ]
    ],
    [$class: 'VaultSecret', path: 'secret/test/cmc/sauce-labs/api-key', secretValues:
      [
        [$class: 'VaultSecretValue', envVar: 'SAUCELABS_ACCESS_KEY', vaultKey: 'value']
      ]
    ]
  ]

  IntegrationTests(env, steps, String... additionalComposeFiles) {
    this.env = env
    this.steps = steps
    this.additionalComposeFiles = additionalComposeFiles
  }

  def execute(Map config, Team team = Team.CITIZEN) {
    execute(this.&executeTests, config, team)
  }

  def executeCrossBrowser(Map config, Team team = Team.CITIZEN) {
    execute(this.&executeCrossBrowserTests, config, team)
  }

  def execute(Closure runTestsFunction, Map config, Team team) {
    steps.ws(steps.pwd() + "/it-tests-${env.JOB_NAME.replaceAll("\\/", "-")}-${env.BUILD_NUMBER}") {
      steps.wrap([$class: 'VaultBuildWrapper', vaultSecrets: secrets]) {
        configure(env, config, team)
        loadDockerComposeFile(team.gitUrl)
        updateImages()
        try {
          startTestEnvironment(team.application)
          runTestsFunction(team)
        } catch (e) {
          archiveDockerLogs()
          throw e
        } finally {
          stopTestEnvironment()
        }
      }
    }
  }

  private String dockerComposeCommand() {
    String composeCommand = "docker-compose -f docker-compose.yml"
    for (String additionalComposeFile: additionalComposeFiles) {
      composeCommand += " -f " + additionalComposeFile
    }
    return composeCommand
  }

  private void loadDockerComposeFile(String gitHubUrl) {
    // Generic checkout used to allow checking out of commit hashes
    // http://stackoverflow.com/a/43613408/4951015
    steps.checkout([$class           : 'GitSCM', branches: [[name: env.INTEGRATION_TESTS_BRANCH]],
                    userRemoteConfigs: [[url: gitHubUrl]]])
  }

  private void updateImages() {
    steps.sh "${dockerComposeCommand()} pull"
  }

  private void startTestEnvironment(String application) {
    steps.sh """
              ${dockerComposeCommand()} up --no-color -d remote-webdriver \\
                         ${application}          
              """
  }

  private void executeTests(Team team) {
    steps.sh """
            mkdir -p output
            ${dockerComposeCommand()} up --no-deps --no-color ${team.testsContainerName}
            """
    analyseTestResults(team)
  }

  private void executeCrossBrowserTests(Team team) {
    try {
      steps.sh """
              mkdir -p output
              ${dockerComposeCommand()} up --no-color -d saucelabs-connect
              ./bin/run-cross-browser-tests.sh
              """
    } finally {
      steps.junit allowEmptyResults: true, testResults: './output/*result.xml'
      steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.png'
      steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.xml'
      steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.html'
    }
  }

  private void analyseTestResults(Team team) {
    def testExitCode = steps.sh returnStdout: true,
      script: "${dockerComposeCommand()} ps -q ${team.testsContainerName} | xargs docker inspect -f '{{ .State.ExitCode }}'"


    def testResultFilePath = 'output/integration-result.xml'
    if (steps.fileExists(testResultFilePath)) {
      steps.junit testResultFilePath
    }

    if (team == Team.LEGAL) {
      def mochaAwesomeReport = 'output/CMCT2-End2End-Test-Report.html'
      if (steps.fileExists(mochaAwesomeReport)) {
        steps.archiveArtifacts 'output/CMCT2-End2End-Test-Report.html'
      }
      def downloadedPdf = 'download/000LR001.pdf'
      if(steps.fileExists(downloadedPdf)) {
      steps.archiveArtifacts 'download/*.pdf'
      }
    }

    archiveDockerLogs()

    if (testExitCode.toInteger() > 0) {
      steps.archiveArtifacts 'output/*.png'
      steps.error("Integration tests failed")
    }
  }

  private void archiveDockerLogs() {
    steps.sh "mkdir -p output && ${dockerComposeCommand()} logs --no-color > output/logs.txt"
    steps.archiveArtifacts 'output/logs.txt'
  }

  private void stopTestEnvironment() {
    steps.sh "${dockerComposeCommand()} down"
  }

  private void configure(env, Map versions, Team team) {
    env.COMPOSE_HTTP_TIMEOUT = 240
    env.SAUCELABS_USERNAME = team.saucelabsUserName
    env.SAUCELABS_TUNNEL_IDENTIFIER = team.saucelabsTunnelIdentifier

    if (versions != null) {
      for (Map.Entry<String, String> entry : versions.entrySet()) {
        env[entry.getKey()] = entry.getValue()
      }
    }

    if (env.INTEGRATION_TESTS_BRANCH == null) {
      env.INTEGRATION_TESTS_BRANCH = 'master'
    }
  }

}
