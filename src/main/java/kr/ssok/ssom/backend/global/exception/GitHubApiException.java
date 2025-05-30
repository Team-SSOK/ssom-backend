package kr.ssok.ssom.backend.global.exception;

/**
 * GitHub API 호출 시 발생하는 예외
 */
public class GitHubApiException extends RuntimeException {
    
    private final int statusCode;
    private final String responseBody;
    
    public GitHubApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
}
