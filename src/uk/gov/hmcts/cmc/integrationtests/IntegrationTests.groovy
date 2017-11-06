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
    doExecute(this.&executeTests, config)
  }

  def executeCrossBrowser(Map config) {
    doExecute(this.&executeCrossBrowserTests, config)
  }

  private void doExecute(Closure runTestsFunction, Map config) {
    steps.ws(steps.pwd() + "/it-tests-${env.JOB_NAME.replaceAll("\\/", "-")}-${env.BUILD_NUMBER}") {
      steps.wrap([$class: 'VaultBuildWrapper', vaultSecrets: secrets]) {
        configure(env, config)
        loadDockerComposeFile('https://github.com/hmcts/cmc-integration-tests.git')
        updateImages()
        try {
          startTestEnvironment()
          runTestsFunction()
        } finally {
          steps.junit allowEmptyResults: true, testResults: './output/*result.xml'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.png'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.xml'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.html'
          steps.archiveArtifacts allowEmptyArchive: true, artifacts: 'output/*.pdf'

          archiveDockerStatus()
          archiveDockerLogs()
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
    def exitCode = steps.sh returnStatus: true, script: """
            mkdir -p output
            ${dockerComposeCommand()} ${runCommand()}
            """

    if (exitCode > 0) {
      steps.archiveArtifacts 'output/*.png'
      steps.error("Integration tests failed")
    }
  }

  private String runCommand() {
    String testsTag = env.TESTS_TAG
    if (testsTag == null || testsTag.trim().isEmpty()) {
      return "run --no-deps integration-tests"
    } else {
      return "run --no-deps integration-tests test -- --grep '${testsTag}'"
    }
  }

  private void executeCrossBrowserTests() {
    steps.sh """
            mkdir -p output
            ${dockerComposeCommand()} up --no-color -d saucelabs-connect
            ./bin/cross-browser/run-tests.sh
            """
  }

  private void archiveDockerStatus() {
    steps.sh "mkdir -p output && ${dockerComposeCommand()} ps > output/docker-status.txt"
    steps.archiveArtifacts 'output/docker-status.txt'
  }

  private void archiveDockerLogs() {
    steps.sh """
             mkdir -p output
             ${dockerComposeCommand()} logs --no-color > output/docker-logs.txt
             for service in \$(docker-compose config --services); do docker-compose logs --no-color \$service > output/docker-log-\$service.txt; done
             """

    steps.archiveArtifacts 'output/docker-log*.txt'
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
