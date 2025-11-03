package com.yagi.socialanalyzer.infrastructure.persistence;

import com.yagi.socialanalyzer.domain.entities.SocialMediaPost;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import com.yagi.socialanalyzer.domain.repositories.IPostRepository;
import com.yagi.socialanalyzer.domain.valueobjects.EngagementMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * SQLite implementation of post repository with JSON file integration.
 */
public class SQLitePostRepository implements IPostRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLitePostRepository.class);
    private final DatabaseManager databaseManager;
    private final JsonFileManager jsonFileManager;
    
    public SQLitePostRepository(DatabaseManager databaseManager, JsonFileManager jsonFileManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.jsonFileManager = Objects.requireNonNull(jsonFileManager);
    }
    
    @Override
    public void save(SocialMediaPost post) throws RepositoryException {
        String sql = """
            INSERT INTO posts_metadata (id, project_id, platform, post_id, author, content,
                published_at, likes, shares, comments, views, url, collected_at, data_file_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(project_id, platform, post_id) DO UPDATE SET
                author = excluded.author,
                content = excluded.content,
                published_at = excluded.published_at,
                likes = excluded.likes,
                shares = excluded.shares,
                comments = excluded.comments,
                views = excluded.views,
                url = excluded.url,
                data_file_path = excluded.data_file_path
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String compositeId = post.getCompositeId();
            stmt.setString(1, compositeId);
            stmt.setString(2, post.getProjectId().toString());
            stmt.setString(3, post.getPlatform());
            stmt.setString(4, post.getPostId());
            stmt.setString(5, post.author());
            stmt.setString(6, post.getContent());
            stmt.setString(7, post.getPublishedAt().toString());
            stmt.setInt(8, post.getEngagementMetrics().likes());
            stmt.setInt(9, post.getEngagementMetrics().shares());
            stmt.setInt(10, post.getEngagementMetrics().comments());
            stmt.setLong(11, post.getEngagementMetrics().views());
            stmt.setString(12, post.getUrl());
            stmt.setString(13, post.getCollectedAt().toString());
            stmt.setString(14, post.getDataFilePath());
            
            stmt.executeUpdate();
            logger.debug("Saved post: {}", compositeId);
            
        } catch (Exception e) {
            logger.error("Failed to save post: {}", post.getCompositeId(), e);
            throw new RepositoryException("Failed to save post: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void saveBatch(List<SocialMediaPost> posts) throws RepositoryException {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO posts_metadata (id, project_id, platform, post_id, author, content,
                published_at, likes, shares, comments, views, url, collected_at, data_file_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(project_id, platform, post_id) DO NOTHING
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (SocialMediaPost post : posts) {
                stmt.setString(1, post.getCompositeId());
                stmt.setString(2, post.getProjectId().toString());
                stmt.setString(3, post.getPlatform());
                stmt.setString(4, post.getPostId());
                stmt.setString(5, post.author());
                stmt.setString(6, post.getContent());
                stmt.setString(7, post.getPublishedAt().toString());
                stmt.setInt(8, post.getEngagementMetrics().likes());
                stmt.setInt(9, post.getEngagementMetrics().shares());
                stmt.setInt(10, post.getEngagementMetrics().comments());
                stmt.setLong(11, post.getEngagementMetrics().views());
                stmt.setString(12, post.getUrl());
                stmt.setString(13, post.getCollectedAt().toString());
                stmt.setString(14, post.getDataFilePath());
                
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            
            logger.info("Saved batch of {} posts", posts.size());
            
        } catch (Exception e) {
            logger.error("Failed to save post batch", e);
            throw new RepositoryException("Failed to save post batch: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<SocialMediaPost> findById(UUID projectId, String platform, String postId) 
            throws RepositoryException {
        String sql = "SELECT * FROM posts_metadata WHERE project_id = ? AND platform = ? AND post_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId.toString());
            stmt.setString(2, platform);
            stmt.setString(3, postId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPost(rs));
                }
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Failed to find post: {}:{}:{}", projectId, platform, postId, e);
            throw new RepositoryException("Failed to find post: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<SocialMediaPost> findByProjectAndDateRange(UUID projectId, LocalDate startDate, 
            LocalDate endDate) throws RepositoryException {
        String sql = """
            SELECT * FROM posts_metadata 
            WHERE project_id = ? AND published_at >= ? AND published_at <= ?
            ORDER BY published_at DESC
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId.toString());
            stmt.setString(2, startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toString());
            stmt.setString(3, endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString());
            
            return executeQueryForList(stmt);
            
        } catch (Exception e) {
            logger.error("Failed to find posts by date range", e);
            throw new RepositoryException("Failed to find posts: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<SocialMediaPost> findByProjectAndPlatform(UUID projectId, String platform) 
            throws RepositoryException {
        String sql = "SELECT * FROM posts_metadata WHERE project_id = ? AND platform = ? ORDER BY published_at DESC";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId.toString());
            stmt.setString(2, platform);
            
            return executeQueryForList(stmt);
            
        } catch (Exception e) {
            logger.error("Failed to find posts by platform", e);
            throw new RepositoryException("Failed to find posts: " + e.getMessage(), e);
        }
    }
    
    @Override
    public long countByProject(UUID projectId) throws RepositoryException {
        String sql = "SELECT COUNT(*) FROM posts_metadata WHERE project_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
            
        } catch (Exception e) {
            logger.error("Failed to count posts", e);
            throw new RepositoryException("Failed to count posts: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteByProject(UUID projectId) throws RepositoryException {
        String sql = "DELETE FROM posts_metadata WHERE project_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId.toString());
            int deleted = stmt.executeUpdate();
            
            // Also delete JSON files
            jsonFileManager.deleteProjectFiles(projectId);
            
            logger.info("Deleted {} posts for project {}", deleted, projectId);
            
        } catch (Exception e) {
            logger.error("Failed to delete posts", e);
            throw new RepositoryException("Failed to delete posts: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean exists(UUID projectId, String platform, String postId) throws RepositoryException {
        String sql = "SELECT COUNT(*) FROM posts_metadata WHERE project_id = ? AND platform = ? AND post_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId.toString());
            stmt.setString(2, platform);
            stmt.setString(3, postId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (Exception e) {
            logger.error("Failed to check post existence", e);
            throw new RepositoryException("Failed to check post existence: " + e.getMessage(), e);
        }
    }
    
    private List<SocialMediaPost> executeQueryForList(PreparedStatement stmt) throws Exception {
        List<SocialMediaPost> posts = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        
        return posts;
    }
    
    private SocialMediaPost mapResultSetToPost(ResultSet rs) throws SQLException {
        UUID projectId = UUID.fromString(rs.getString("project_id"));
        String platform = rs.getString("platform");
        String postId = rs.getString("post_id");
        String author = rs.getString("author");
        String content = rs.getString("content");
        Instant publishedAt = Instant.parse(rs.getString("published_at"));
        
        EngagementMetrics metrics = new EngagementMetrics(
            rs.getInt("likes"),
            rs.getInt("shares"),
            rs.getInt("comments"),
            rs.getLong("views")
        );
        
        String url = rs.getString("url");
        Instant collectedAt = Instant.parse(rs.getString("collected_at"));
        String dataFilePath = rs.getString("data_file_path");
        
        return new SocialMediaPost(projectId, platform, postId, author, content,
            publishedAt, metrics, url, collectedAt, dataFilePath);
    }
}
