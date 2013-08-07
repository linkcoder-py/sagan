package org.springframework.site.domain.services.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.site.domain.services.CacheService;
import org.springframework.social.github.api.GitHub;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class CachingGitHubRestClient {
	private final GitHub gitHub;
	private CacheService cacheService;

	@Autowired
	public CachingGitHubRestClient(GitHub gitHub, CacheService cacheService) {
		this.gitHub = gitHub;
		this.cacheService = cacheService;
	}

	public String sendRequestForJson(String path, Object... uriVariables) {
		return sendRequest(path, String.class, HttpMethod.GET, null, uriVariables);
	}

	public String sendRequestForHtml(String path, Object... uriVariables) {
		return sendRequest(path, MarkdownHtml.class, HttpMethod.GET, null, uriVariables);
	}

	public String sendPostRequestForHtml(String path, String body, Object... uriVariables) {
		return sendRequest(path, String.class, HttpMethod.POST, body, uriVariables);
	}

	<T> String sendRequest(String path, Class<T> clazz, HttpMethod method, String body, Object... uriVariables) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		String etag = cacheService.getEtagForPath(path);
		if (etag != null) {
			headers.add("If-None-Match", etag);
		}

		HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<T> entity = gitHub.restOperations().exchange(GitHubService.API_URL_BASE + path, method, requestEntity, clazz, uriVariables);

		if (entity.getStatusCode().equals(HttpStatus.NOT_MODIFIED)) {
			return cacheService.getContentForPath(path);
		} else {
			cacheService.cacheContent(path, entity.getHeaders().getETag(), entity.getBody().toString());
			return entity.getBody().toString();
		}
	}
}