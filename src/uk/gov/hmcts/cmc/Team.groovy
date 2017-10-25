package uk.gov.hmcts.cmc

public enum Team {
  LEGAL(
    'git@github.com:hmcts/cmc-integration-tests.git',
    'legal-frontend',
    'legal-integration-tests',
    '!!! NOT AVAILABLE YET !!!',
    '!!! NOT AVAILABLE YET !!!'
  ),
  CITIZEN(
    'git@github.com:hmcts/cmc-integration-tests.git',
    'citizen-frontend',
    'integration-tests',
    'civilmoneyclaimsT1',
    'saucelabs-overnight-tunnel-cmc-T1'
  )

  private String gitUrl
  private String application
  private String testsContainerName
  private String saucelabsUserName
  private String saucelabsTunnelIdentifier

  Team(gitUrl, application, testsContainerName, saucelabsUserName, saucelabsTunnelIdentifier) {
    this.application = application
    this.gitUrl = gitUrl
    this.testsContainerName = testsContainerName
    this.saucelabsUserName = saucelabsUserName
    this.saucelabsTunnelIdentifier = saucelabsTunnelIdentifier
  }

  String getGitUrl() {
    return gitUrl
  }

  String getApplication() {
    return application
  }

  String getTestsContainerName() {
    return testsContainerName
  }

  String getSaucelabsUserName() {
    return saucelabsUserName
  }

  String getSaucelabsTunnelIdentifier() {
    return saucelabsTunnelIdentifier
  }
}
