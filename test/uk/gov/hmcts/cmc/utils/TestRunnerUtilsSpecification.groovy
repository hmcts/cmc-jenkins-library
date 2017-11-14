package uk.gov.hmcts.cmc.utils

import spock.lang.Specification
import spock.lang.Unroll

class TestRunnerUtilsSpecification extends Specification {

  def "should throw an exception if repository is not defined"() {
    when:
      TestRunnerUtils.convertToVersionEnvironmentVariable(null)
    then:
      def ex = thrown(RuntimeException)
      ex.message == 'Repository cannot be null'
  }

  @Unroll
  def "should return version environment variable if repository is defined"() {
    expect:
      TestRunnerUtils.convertToVersionEnvironmentVariable(repository) == variable
    where:
      repository               || variable
      'cmc/integration-tests'  || 'INTEGRATION_TESTS_VERSION'
      'cmc/citizen-frontend'   || 'CITIZEN_FRONTEND_VERSION'
      'cmc/legal-frontend'     || 'LEGAL_FRONTEND_VERSION'
      'cmc/claim-store-api'    || 'CLAIM_STORE_API_VERSION'
  }

}
