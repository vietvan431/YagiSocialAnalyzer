package com.yagi.socialanalyzer.infrastructure.persistence;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import com.yagi.socialanalyzer.domain.repositories.IProjectRepository;
import com.yagi.socialanalyzer.domain.valueobjects.ProjectStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * SQLite implementation of project repository.
 */
public class SQLiteProjectRepository implements IProjectRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLiteProjectRepository.class);
    private final DatabaseManager databaseManager;
    private final ObjectMapper objectMapper;
    
    public SQLiteProjectRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void save(DisasterProject project) throws RepositoryException {
        String sql = """
            INSERT INTO projects (id, name, disaster_name, region, start_date, end_date,
                keywords, data_sources, status, created_at, updated_at, total_posts_collected)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                disaster_name = excluded.disaster_name,
                region = excluded.region,
                start_date = excluded.start_date,
                end_date = excluded.end_date,
                keywords = excluded.keywords,
                data_sources = excluded.data_sources,
                status = excluded.status,
                updated_at = excluded.updated_at,
                total_posts_collected = excluded.total_posts_collected
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, project.getId().toString());
            stmt.setString(2, project.getName());
            stmt.setString(3, project.getDisasterName());
            stmt.setString(4, project.getRegion());
            stmt.setString(5, project.getStartDate().toString());
            stmt.setString(6, project.getEndDate().toString());
            stmt.setString(7, objectMapper.writeValueAsString(project.getKeywords()));
            stmt.setString(8, objectMapper.writeValueAsString(project.getDataSources()));
            stmt.setString(9, project.getStatus().name());
            stmt.setString(10, project.getCreatedAt().toString());
            stmt.setString(11, project.getUpdatedAt().toString());
            stmt.setLong(12, project.getTotalPostsCollected());
            
            stmt.executeUpdate();
            logger.debug("Saved project: {}", project.getId());
            
        } catch (Exception e) {
            logger.error("Failed to save project: {}", project.getId(), e);
            throw new RepositoryException("Failed to save project: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Optional<DisasterProject> findById(UUID id) throws RepositoryException {
        String sql = "SELECT * FROM projects WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProject(rs));
                }
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Failed to find project: {}", id, e);
            throw new RepositoryException("Failed to find project: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<DisasterProject> findByStatus(ProjectStatus status) throws RepositoryException {
        String sql = "SELECT * FROM projects WHERE status = ? ORDER BY created_at DESC";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            return executeQueryForList(stmt);
            
        } catch (Exception e) {
            logger.error("Failed to find projects by status: {}", status, e);
            throw new RepositoryException("Failed to find projects: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<DisasterProject> findAll() throws RepositoryException {
        String sql = "SELECT * FROM projects ORDER BY created_at DESC";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            return executeQueryForList(stmt);
            
        } catch (Exception e) {
            logger.error("Failed to find all projects", e);
            throw new RepositoryException("Failed to find projects: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void delete(UUID id) throws RepositoryException {
        String sql = "DELETE FROM projects WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id.toString());
            stmt.executeUpdate();
            logger.info("Deleted project: {}", id);
            
        } catch (Exception e) {
            logger.error("Failed to delete project: {}", id, e);
            throw new RepositoryException("Failed to delete project: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean existsByName(String name) throws RepositoryException {
        String sql = "SELECT COUNT(*) FROM projects WHERE name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
            
        } catch (Exception e) {
            logger.error("Failed to check project existence: {}", name, e);
            throw new RepositoryException("Failed to check project existence: " + e.getMessage(), e);
        }
    }
    
    @Override
    public long count() throws RepositoryException {
        String sql = "SELECT COUNT(*) FROM projects";
        
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next() ? rs.getLong(1) : 0;
            
        } catch (Exception e) {
            logger.error("Failed to count projects", e);
            throw new RepositoryException("Failed to count projects: " + e.getMessage(), e);
        }
    }
    
    private List<DisasterProject> executeQueryForList(PreparedStatement stmt) throws Exception {
        List<DisasterProject> projects = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
        }
        
        return projects;
    }
    
    @SuppressWarnings("unchecked")
    private DisasterProject mapResultSetToProject(ResultSet rs) throws Exception {
        UUID id = UUID.fromString(rs.getString("id"));
        String name = rs.getString("name");
        String disasterName = rs.getString("disaster_name");
        String region = rs.getString("region");
        LocalDate startDate = LocalDate.parse(rs.getString("start_date"));
        LocalDate endDate = LocalDate.parse(rs.getString("end_date"));
        
        Set<String> keywords = objectMapper.readValue(
            rs.getString("keywords"), 
            objectMapper.getTypeFactory().constructCollectionType(HashSet.class, String.class)
        );
        
        Set<String> dataSources = objectMapper.readValue(
            rs.getString("data_sources"),
            objectMapper.getTypeFactory().constructCollectionType(HashSet.class, String.class)
        );
        
        ProjectStatus status = ProjectStatus.valueOf(rs.getString("status"));
        Instant createdAt = Instant.parse(rs.getString("created_at"));
        Instant updatedAt = Instant.parse(rs.getString("updated_at"));
        long totalPostsCollected = rs.getLong("total_posts_collected");
        
        return new DisasterProject(id, name, disasterName, region, startDate, endDate,
            keywords, dataSources, status, createdAt, updatedAt, totalPostsCollected);
    }
}
