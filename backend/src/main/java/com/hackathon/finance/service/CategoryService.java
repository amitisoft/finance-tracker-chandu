package com.hackathon.finance.service;

import com.hackathon.finance.dto.category.CategoryRequest;
import com.hackathon.finance.dto.category.CategoryResponse;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.exception.ConflictException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserContextService userContextService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        UserEntity user = userContextService.getCurrentUser();
        return categoryRepository.findAllByUserAndArchivedFalseOrderByNameAsc(user).stream().map(mapper::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        UserEntity user = userContextService.getCurrentUser();
        if (categoryRepository.existsByUserAndNameIgnoreCaseAndType(user, request.name(), request.type())) {
            throw new ConflictException("Category with the same name already exists.");
        }
        CategoryEntity category = new CategoryEntity();
        category.setUser(user);
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setColor(request.color());
        category.setIcon(request.icon());
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        CategoryEntity category = findOwned(id);
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setColor(request.color());
        category.setIcon(request.icon());
        return mapper.toCategoryResponse(category);
    }

    @Transactional
    public void delete(UUID id) {
        findOwned(id).setArchived(true);
    }

    @Transactional(readOnly = true)
    public CategoryEntity findOwned(UUID id) {
        return categoryRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Category not found."));
    }

    @Transactional(readOnly = true)
    public Optional<CategoryEntity> findByUserAndName(UserEntity user, String name) {
        return categoryRepository.findByUserAndNameIgnoreCase(user, name.trim());
    }
}
