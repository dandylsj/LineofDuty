package com.example.lineofduty.domain.category;

import com.example.lineofduty.common.model.enums.SuccessMessage;
import com.example.lineofduty.common.model.response.GlobalResponse;
import com.example.lineofduty.domain.category.dto.CategoryCreateRequest;
import com.example.lineofduty.domain.category.dto.CategoryCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category", description = "카테고리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // 관리자 API - 카테고리 생성
    @Operation(summary = "카테고리 생성 (관리자)", description = "새로운 카테고리를 생성합니다. parentId를 넣으면 하위 카테고리가 됩니다.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> createCategory(@RequestBody CategoryCreateRequest request) {
        CategoryCreateResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(SuccessMessage.CATEGORY_CREATE_SUCCESS, response));
    }

    // 공개 API - 전체 카테고리 계층 구조 조회
    @Operation(summary = "카테고리 목록 조회", description = "최상위 카테고리와 하위 카테고리를 계층 구조로 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getCategories() {
        List<CategoryCreateResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.CATEGORY_READ_SUCCESS, response));
    }

    // 공개 API - 특정 카테고리 단건 조회
    @Operation(summary = "카테고리 단건 조회", description = "카테고리 ID로 특정 카테고리를 조회합니다.")
    @GetMapping("/{categoryId}")
    public ResponseEntity<GlobalResponse> getCategory(@PathVariable Long categoryId) {
        CategoryCreateResponse response = categoryService.getCategory(categoryId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.CATEGORY_READ_SUCCESS, response));
    }

    // 관리자 API - 카테고리 삭제
    @Operation(summary = "카테고리 삭제 (관리자)", description = "카테고리를 삭제합니다. 하위 카테고리가 있으면 삭제할 수 없습니다.")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.CATEGORY_DELETE_SUCCESS, null));
    }
}
