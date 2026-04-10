package com.conk.batch.common.exception;

/**
 * 배치 서비스 공통 에러 코드 계약이다.
 */
public interface ErrorCode {

    String getCode();

    String getMessage();
}
