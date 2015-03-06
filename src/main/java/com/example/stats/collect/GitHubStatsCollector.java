package com.example.stats.collect;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.beans.Tags;
import com.example.repository.TagRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class GitHubStatsCollector implements StatsCollector {

	private static final String URL_ELEMENT = "url";

	private static final String ITEMS_ELEMENT = "items";

	private static String GITHUB_SEARCH_URL = "https://api.github.com/search/repositories?q={tagName}&per_page=100&page=1&access_token={token}";

	@Value("${github.access.token}")
	private String token;

	@Autowired
	private TagRepository tagRepository;

	@Override
	public void collect() throws Exception {

		Boolean hasMore = Boolean.TRUE;
		List<Tags> tagsList = tagRepository.findBytagName("hadoop");
		Map<String, List<RepoDetails>> tagToRepoMap = new HashMap<String, List<RepoDetails>>();
		if (null != tagsList && tagsList.size() > 0) {
			for (Tags tags : tagsList) {
				List<RepoDetails> masterList = new ArrayList<RepoDetails>();
				String tagName = tags.getTagName();
				if (null != tagToRepoMap.get(tagName))
					continue;
				tagToRepoMap.put(tagName, masterList);
				hasMore = Boolean.TRUE;
				String URL = GITHUB_SEARCH_URL;
				while (hasMore) {
					ResponseEntity<byte[]> responseEntity = callGitHubSearchAPI(
							tagName, URL);
					if (null != responseEntity) {
						List<RepoDetails> repoDetails = getRepoDetails(responseEntity);
						masterList.addAll(repoDetails);
					}
					String nextPageURL = getNextPageLink(responseEntity);
					if (null != nextPageURL)
						URL = nextPageURL
								.substring(1, nextPageURL.length() - 1);
					else {
						URL = null;
						hasMore = Boolean.FALSE;
					}
				}

				Long commitActivity = calculateCommitActivity(tagName,
						tagToRepoMap.get(tagName));
				tags.setCommitCount(commitActivity);
				tagRepository.save(tags);
			}
		}

	}

	private Long calculateCommitActivity(String tagName,
			List<RepoDetails> repoDetails) {
		Long totalCommits = 0l;
		InputStreamReader reader = null;
		BufferedReader in = null;
		try {
			if (null != repoDetails && repoDetails.size() > 0) {
				for (RepoDetails repoDetail : repoDetails) {
					String commitActivityURL = new StringBuffer(
							repoDetail.getURL())
							.append("/stats/commit_activity?access_token=")
							.append(token).toString();
					ResponseEntity<byte[]> responseEntity = callCommitActivityAPI(commitActivityURL);
					if (null != responseEntity) {
						byte[] body = responseEntity.getBody();
						GZIPInputStream gzipInputStream = new GZIPInputStream(
								new ByteArrayInputStream(body));

						reader = new InputStreamReader(gzipInputStream);
						in = new BufferedReader(reader);
						String json = in.readLine();
						JsonArray jsonArray = (new JsonParser()).parse(json)
								.getAsJsonArray();

						if (null != jsonArray) {
							JsonObject commitsObject = jsonArray.get(
									jsonArray.size() - 1).getAsJsonObject();
							Integer commits = commitsObject.get("total")
									.getAsInt();
							if (null != commits)
								totalCommits += commits;
						}
					}

				}
			}

		} catch (Exception e) {

		} finally {

			try {
				if (null != reader)
					reader.close();
				if (null != in)
					in.close();
			}

			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return totalCommits;

	}

	private ResponseEntity<byte[]> callCommitActivityAPI(String URL) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");

		HttpEntity<String> httpEntity = new HttpEntity<String>("parameters",
				headers);

		ResponseEntity<byte[]> responseEntity = restTemplate.exchange(URL,
				HttpMethod.GET, httpEntity, byte[].class);

		if (responseEntity.getStatusCode() == HttpStatus.OK) {

			return responseEntity;
		} else if (responseEntity.getStatusCode() == HttpStatus.ACCEPTED) {
			try {
				Thread.sleep(30 * 1000);
			} catch (InterruptedException e) {

			}
			return callCommitActivityAPI(URL);
		}
		return null;
	}

	private String getNextPageLink(ResponseEntity<byte[]> responseEntity) {
		List<String> links = responseEntity.getHeaders().get(HttpHeaders.LINK);
		if (null != links && links.size() > 0) {
			for (String link : links) {
				String[] splits = link.split(",");
				if (null != splits && splits.length > 0) {
					for (String split : splits) {
						String[] linkSplits = split.split(";");
						if (null != linkSplits && linkSplits.length > 0) {
							if (" rel=\"next\"".equals(linkSplits[1]))
								return linkSplits[0];
						}
					}
				}
			}
		}
		return null;
	}

	private List<RepoDetails> getRepoDetails(
			ResponseEntity<byte[]> responseEntity) {
		InputStreamReader reader = null;
		BufferedReader in = null;
		try {
			List<RepoDetails> repoDetailsList = new ArrayList<GitHubStatsCollector.RepoDetails>();
			byte[] body = responseEntity.getBody();
			GZIPInputStream gzipInputStream = new GZIPInputStream(
					new ByteArrayInputStream(body));

			reader = new InputStreamReader(gzipInputStream);
			in = new BufferedReader(reader);

			String json = in.readLine();

			JsonObject jsonObject = (new JsonParser()).parse(json)
					.getAsJsonObject();
			if (null != jsonObject) {
				JsonElement itemsElement = jsonObject.get(ITEMS_ELEMENT);
				if (null != itemsElement) {
					JsonArray itemsArray = itemsElement.getAsJsonArray();
					if (null != itemsArray && itemsArray.size() > 0) {
						for (JsonElement items : itemsArray) {
							JsonObject itemObject = items.getAsJsonObject();
							String repoURL = itemObject.get(URL_ELEMENT)
									.getAsString();
							RepoDetails repoDetails = new RepoDetails();
							repoDetails.setURL(repoURL);
							repoDetailsList.add(repoDetails);
						}
					}
				}
			}
			return repoDetailsList;
		} catch (Exception e) {

		} finally {

			try {
				if (null != reader)
					reader.close();
				if (null != in)
					in.close();
			}

			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return null;
	}

	private ResponseEntity<byte[]> callGitHubSearchAPI(String tagName,
			String URL) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");

		HttpEntity<String> httpEntity = new HttpEntity<String>("parameters",
				headers);

		ResponseEntity<byte[]> responseEntity = restTemplate.exchange(URL,
				HttpMethod.GET, httpEntity, byte[].class, tagName, token);

		if (responseEntity.getStatusCode() == HttpStatus.OK) {

			return responseEntity;
		}

		return null;
	}

	static class RepoDetails {

		private String loginName;

		private String repoName;

		private String URL;

		public String getLoginName() {
			return loginName;
		}

		public void setLoginName(String loginName) {
			this.loginName = loginName;
		}

		public String getRepoName() {
			return repoName;
		}

		public void setRepoName(String repoName) {
			this.repoName = repoName;
		}

		public String getURL() {
			return URL;
		}

		public void setURL(String uRL) {
			URL = uRL;
		}
	}

}
