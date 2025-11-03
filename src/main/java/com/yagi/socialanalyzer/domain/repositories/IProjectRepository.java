package com.yagi.socialanalyzer.domain.repositories;

import com.yagi.socialanalyzer.domain.entities.DisasterProject;
import com.yagi.socialanalyzer.domain.exceptions.RepositoryException;
import com.yagi.socialanalyzer.domain.valueobjects.ProjectStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DisasterProject persistence.
 */
public interface IProjectRepository {
    
    /**
     * Save a project (insert or update).
     */
    void save(DisasterProject project) throws RepositoryException;
    
    /**
     * Find project by ID.
     */
    Optional<DisasterProject> findById(UUID id) throws RepositoryException;
    
    /**
     * Find projects by status.
     */
    List<DisasterProject> findByStatus(ProjectStatus status) throws RepositoryException;
    
    /**
     * Find all projects.
     */
    List<DisasterProject> findAll() throws RepositoryException;
    
    /**
     * Delete a project by ID.
     */
    void delete(UUID id) throws RepositoryException;
    
    /**
     * Check if project with name exists.
     */
    boolean existsByName(String name) throws RepositoryException;
    
    /**
     * Count total projects.
     */
    long count() throws RepositoryException;
}
