package uk.gov.hmcts.cmc.integrationtests

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

  def execute(Map config) {
    execute(this.&executeTests, config)
  }

  def executeCrossBrowser(Map config) {
    execute(this.&executeCrossBrowserTests, config)
  }

  def execute(Closure runTestsFunction, Map config) {
    steps.ws(steps.pwd() + "/it-tests-${env.JOB_NAME.replaceAll("\\/", "-")}-${env.BUILD_NUMBER}") {
      steps.wrap([$class: 'VaultBuildWrapper', vaultSecrets: secrets]) {
        configure(env, config)
        loadDockerComposeFile('https://github.com/hmcts/cmc-integration-tests.git')
        updateImages()
        try {
          startTestEnvironment()
          runTestsFunction()
        } catch (e) {
          archiveDockerLogs()
          throw e
        } finally {
          steps.junit allowEmptyResults: true, testResults: './output/*result.xml'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.png'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.xml'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.html'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.pdf'

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

  private void startTestEnvironment() {
    steps.sh """
              ${dockerComposeCommand()} up --no-color -d remote-webdriver \\
                         citizen-frontend \\
                         legal-frontend
              """
  }

  private void executeTests() {
    steps.sh """
            mkdir -p output
            ${dockerComposeCommand()} ${runCommand()}
            """
    analyseTestResults()
  }

  private String runCommand() {
    String testsTag = env.TESTS_TAG
    if (testsTag == null || testsTag.trim().isEmpty()) {
      return "run --no-deps --rm integration-tests"
    } else {
      return "run --no-deps --rm integration-tests test -- --grep '${testsTag}'"
    }
  }

  private void executeCrossBrowserTests() {
    steps.sh """
            mkdir -p output
            ${dockerComposeCommand()} up --no-color -d saucelabs-connect
            ./bin/run-cross-browser-tests.sh
            """
  }

  private void analyseTestResults() {
    def testExitCode = steps.sh returnStdout: true,
      script: "${dockerComposeCommand()} ps -q integration-tests | xargs docker inspect -f '{{ .State.ExitCode }}'"

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

  private void configure(env, Map config) {
    env.COMPOSE_HTTP_TIMEOUT = 240
    env.SAUCELABS_USERNAME = 'civilmoneyclaimsT1'
    env.SAUCELABS_TUNNEL_IDENTIFIER = 'saucelabs-overnight-tunnel-cmc-T1'

    if (config != null) {
      for (Map.Entry<String, String> entry : config.entrySet()) {
        env[entry.getKey()] = entry.getValue()
      }
    }

    if (env.INTEGRATION_TESTS_BRANCH == null) {
      env.INTEGRATION_TESTS_BRANCH = 'master'
    }
  }

}
