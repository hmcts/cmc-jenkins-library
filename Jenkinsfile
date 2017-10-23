#!groovy
properties(
  [[$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/hmcts/cmc-jenkins-library'],
   pipelineTriggers([
     [$class: 'GitHubPushTrigger'],
     [$class: 'hudson.triggers.TimerTrigger', spec: 'H 1 * * *']
   ])]
)

def channel = '#cmc-tech-notification'

@Library('Reform') _

node {
  try {
    stage('Checkout') {
      deleteDir()
      checkout scm
    }

    stage('Build') {
      sh "./gradlew clean build -x test"
    }

    stage('Test') {
      sh "./gradlew test"
    }
  } catch (err) {
    notifyBuildFailure channel: channel
    throw err
  }
  notifyBuildFixed channel: channel
}

