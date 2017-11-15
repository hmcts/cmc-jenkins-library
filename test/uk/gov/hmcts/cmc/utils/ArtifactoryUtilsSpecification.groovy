package uk.gov.hmcts.cmc.utils

import spock.lang.Specification

class ArtifactoryUtilsSpecification extends Specification {

  def "should throw an exception if path is not defined"() {
    when:
      ArtifactoryUtils.extractImageVersionFromPath(null)
    then:
      def ex = thrown(RuntimeException)
      ex.message == 'Path cannot be null'
  }

  def "should return version if path is defined"() {
    when:
      def result = ArtifactoryUtils.extractImageVersionFromPath('cmc/citizen-frontend/b6f205d3cefcf2b1e911a3cf89e88f6ddd8ffa3b')
    then:
      result == 'b6f205d3cefcf2b1e911a3cf89e88f6ddd8ffa3b'
  }
}
