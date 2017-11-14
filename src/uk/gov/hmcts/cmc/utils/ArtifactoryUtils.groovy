package uk.gov.hmcts.cmc.utils

class ArtifactoryUtils {

  static String extractImageVersionFromPath(String path) {
    if (path == null) {
      throw new RuntimeException('Path cannot be null')
    }

    path.substring(path.lastIndexOf('/') + 1)
  }

}
