package uk.gov.hmcts.cmc.utils

class TestRunnerUtils {

  static String convertToVersionEnvironmentVariable(String repository) {
    if (repository == null) {
      throw new RuntimeException('Repository cannot be null')
    }

    repository.substring(repository.lastIndexOf('/') + 1)
      .replace('-', '_')
      .toUpperCase()
      .concat("_VERSION")
  }

}
