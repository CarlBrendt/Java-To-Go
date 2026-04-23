package ru.mts.ip.workflow.engine.temporal;

public record SendToS3Input(String activityId, S3Connection connectionDef, S3File s3File,
    String bucket, String region) {
  public record S3File(String filePath, String contentType, String content) {
  }


  public record S3Connection(String endpoint, S3Auth authDef) {
  }


  public record S3Auth(String type, AccessKeyAuth accessKeyAuth) {
  }


  public record AccessKeyAuth(String accessKey, String secretKey) {
  }
}
