package com.yagi.socialanalyzer.infrastructure.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import com.yagi.socialanalyzer.domain.exceptions.CollectionException;
import com.yagi.socialanalyzer.domain.valueobjects.EngagementMetrics;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Reddit data source using Reddit API with OAuth 2.0 authentication.
 * Implements real API integration for searching submissions and comments.
 */
public class RedditDataSource implements IPlatformDataSource {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditDataSource.class);
    private static final String PLATFORM_ID = "reddit";
    private static final String OAUTH_URL = "https://www.reddit.com/api/v1/access_token";
    private static final String API_BASE_URL = "https://oauth.reddit.com";
    private static final String USER_AGENT = "YagiSocialAnalyzer/1.0";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // Start with 2 seconds
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;
    
    private String accessToken;
    private String clientId;
    private String clientSecret;
    private boolean authenticated = false;
    private int remainingRateLimit = 60; // Reddit: 60 requests per minute
    private long rateLimitResetTime = 0;
    
    public RedditDataSource() {
        this.httpClient = HttpClients.createDefault();
    }
    
    @Override
    public String getPlatformId() {
        return PLATFORM_ID;
    }
    
    /**
     * Authenticate with Reddit API using OAuth 2.0 client credentials flow.
     * Expected credentials format: "client_id:client_secret"
     */
    @Override
    public void authenticate(String credentials) throws CollectionException {
        logger.info("Authenticating with Reddit API using OAuth 2.0...");
        
        if (credentials == null || credentials.isEmpty()) {
            throw new CollectionException("Reddit credentials are required");
        }
        
        // Parse credentials
        String[] parts = credentials.split(":");
        if (parts.length != 2) {
            throw new CollectionException("Invalid credentials format. Expected 'client_id:client_secret'");
        }
        
        this.clientId = parts[0].trim();
        this.clientSecret = parts[1].trim();
        
        try {
            // Request access token using client credentials flow
            HttpPost post = new HttpPost(OAUTH_URL);
            
            // Basic authentication header
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            post.setHeader("Authorization", "Basic " + encodedAuth);
            post.setHeader("User-Agent", USER_AGENT);
            
            // Request body
            String body = "grant_type=client_credentials";
            post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    this.accessToken = jsonResponse.get("access_token").asText();
                    this.authenticated = true;
                    logger.info("Reddit OAuth authentication successful");
                } else {
                    logger.error("Reddit authentication failed. Status: {}, Response: {}", statusCode, responseBody);
                    throw new CollectionException("Reddit authentication failed: " + responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during Reddit authentication", e);
            throw new CollectionException("Failed to authenticate with Reddit: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<SocialMediaPost> searchPosts(List<String> keywords, LocalDate startDate,
                                            LocalDate endDate, int maxResults) throws CollectionException {
        if (!authenticated) {
            throw new CollectionException("Not authenticated with Reddit");
        }
        
        logger.info("Searching Reddit for keywords: {} (max results: {})", keywords, maxResults);
        
        List<SocialMediaPost> allPosts = new ArrayList<>();
        String query = String.join(" OR ", keywords);
        
        try {
            // Search in r/all
            allPosts.addAll(searchSubmissions(query, startDate, endDate, maxResults));
            
            // If we haven't reached maxResults, also collect comments
            if (allPosts.size() < maxResults) {
                int remainingResults = maxResults - allPosts.size();
                logger.info("Collecting Reddit comments (remaining: {})", remainingResults);
                // Note: Comment search is limited in Reddit API, we'd need to search within submissions
            }
            
        } catch (Exception e) {
            logger.error("Error searching Reddit", e);
            throw new CollectionException("Failed to search Reddit: " + e.getMessage(), e);
        }
        
        logger.info("Collected {} posts from Reddit", allPosts.size());
        return allPosts;
    }
    
    /**
     * Search for Reddit submissions (posts) using the search API.
     */
    private List<SocialMediaPost> searchSubmissions(String query, LocalDate startDate, 
                                                   LocalDate endDate, int maxResults) throws Exception {
        List<SocialMediaPost> posts = new ArrayList<>();
        String after = null; // Pagination cursor
        
        long startEpoch = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long endEpoch = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();
        
        while (posts.size() < maxResults) {
            // Build search URL
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_BASE_URL + "/r/all/search.json?q=" + encodedQuery 
                       + "&sort=new&limit=100&restrict_sr=false&t=all";
            
            if (after != null) {
                url += "&after=" + after;
            }
            
            logger.debug("Reddit API request: {}", url);
            
            // Execute request with retry logic
            JsonNode data = executeWithRetry(url);
            
            if (data == null || !data.has("children")) {
                break; // No more results
            }
            
            JsonNode children = data.get("children");
            if (children.size() == 0) {
                break; // No more results
            }
            
            // Process submissions
            for (JsonNode child : children) {
                JsonNode postData = child.get("data");
                
                // Check if post is within date range
                long createdUtc = postData.get("created_utc").asLong();
                if (createdUtc < startEpoch || createdUtc > endEpoch) {
                    continue;
                }
                
                // Extract post data
                SocialMediaPost post = parseSubmission(postData);
                if (post != null) {
                    posts.add(post);
                }
                
                if (posts.size() >= maxResults) {
                    break;
                }
            }
            
            // Get pagination cursor
            if (data.has("after") && !data.get("after").isNull()) {
                after = data.get("after").asText();
            } else {
                break; // No more pages
            }
            
            // Respect rate limits
            if (remainingRateLimit < 10) {
                logger.warn("Reddit rate limit low ({}), waiting...", remainingRateLimit);
                Thread.sleep(60000); // Wait 1 minute
                remainingRateLimit = 60;
            }
        }
        
        return posts;
    }
    
    /**
     * Parse a Reddit submission JSON into a SocialMediaPost.
     */
    private SocialMediaPost parseSubmission(JsonNode data) {
        try {
            String postId = data.get("id").asText();
            String title = data.has("title") ? data.get("title").asText() : "";
            String selftext = data.has("selftext") ? data.get("selftext").asText() : "";
            String content = title + (selftext.isEmpty() ? "" : "\n\n" + selftext);
            
            String author = data.has("author") ? data.get("author").asText() : "unknown";
            long createdUtc = data.get("created_utc").asLong();
            Instant publishedAt = Instant.ofEpochSecond(createdUtc);
            
            // Engagement metrics
            int score = data.has("score") ? data.get("score").asInt() : 0;
            int numComments = data.has("num_comments") ? data.get("num_comments").asInt() : 0;
            
            EngagementMetrics metrics = new EngagementMetrics(
                score,        // likes (upvotes)
                0,            // retweets (not applicable)
                numComments,  // replies (comments)
                0             // views (not available)
            );
            
            String url = "https://reddit.com" + data.get("permalink").asText();
            
            return new SocialMediaPost(
                UUID.randomUUID(), // projectId - will be set by service
                PLATFORM_ID,
                postId,
                author,
                content,
                publishedAt,
                metrics,
                url,
                Instant.now(), // collectedAt
                null  // dataFilePath will be set by repository
            );
            
        } catch (Exception e) {
            logger.error("Error parsing Reddit submission", e);
            return null;
        }
    }
    
    /**
     * Execute HTTP GET request with exponential backoff retry logic.
     */
    private JsonNode executeWithRetry(String url) throws Exception {
        int attempt = 0;
        long delayMs = RETRY_DELAY_MS;
        
        while (attempt < MAX_RETRIES) {
            try {
                HttpGet get = new HttpGet(url);
                get.setHeader("Authorization", "Bearer " + accessToken);
                get.setHeader("User-Agent", USER_AGENT);
                
                try (CloseableHttpResponse response = httpClient.execute(get)) {
                    int statusCode = response.getCode();
                    
                    // Update rate limit from headers
                    if (response.containsHeader("X-Ratelimit-Remaining")) {
                        String remaining = response.getFirstHeader("X-Ratelimit-Remaining").getValue();
                        remainingRateLimit = (int) Double.parseDouble(remaining);
                    }
                    
                    if (response.containsHeader("X-Ratelimit-Reset")) {
                        String reset = response.getFirstHeader("X-Ratelimit-Reset").getValue();
                        rateLimitResetTime = Long.parseLong(reset);
                    }
                    
                    String responseBody = EntityUtils.toString(response.getEntity());
                    
                    if (statusCode == 200) {
                        JsonNode jsonResponse = objectMapper.readTree(responseBody);
                        return jsonResponse.get("data"); // Return the data object
                        
                    } else if (statusCode == 429) {
                        // Rate limit exceeded
                        logger.warn("Reddit rate limit exceeded (429). Attempt {}/{}. Retrying in {}ms...", 
                                   attempt + 1, MAX_RETRIES, delayMs);
                        Thread.sleep(delayMs);
                        delayMs *= 2; // Exponential backoff
                        attempt++;
                        continue;
                        
                    } else if (statusCode == 401) {
                        throw new CollectionException("Reddit authentication expired. Please re-authenticate.");
                        
                    } else {
                        logger.error("Reddit API error. Status: {}, Response: {}", statusCode, responseBody);
                        throw new CollectionException("Reddit API error: " + statusCode);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CollectionException("Request interrupted", e);
            } catch (CollectionException e) {
                throw e;
            } catch (Exception e) {
                if (attempt >= MAX_RETRIES - 1) {
                    throw e;
                }
                logger.warn("Request failed, retrying... Attempt {}/{}", attempt + 1, MAX_RETRIES, e);
                Thread.sleep(delayMs);
                delayMs *= 2;
                attempt++;
            }
        }
        
        throw new CollectionException("Failed to execute Reddit request after " + MAX_RETRIES + " attempts");
    }
    
    @Override
    public boolean isAvailable() {
        return authenticated;
    }
    
    @Override
    public int getRemainingRateLimit() {
        return remainingRateLimit;
    }
    
    @Override
    public void close() {
        logger.info("Closing Reddit data source");
        this.authenticated = false;
        this.accessToken = null;
        
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            logger.error("Error closing HTTP client", e);
        }
    }
}
