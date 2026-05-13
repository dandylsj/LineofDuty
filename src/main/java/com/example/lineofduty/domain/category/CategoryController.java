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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category", description = "카테고리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;


    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    @PostMapping
    public ResponseEntity<GlobalResponse> createCategory(@RequestBody CategoryCreateRequest request) {
        CategoryCreateResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(SuccessMessage.CATEGORY_CREATE_SUCCESS, response));
    }

//    @Operation(summary = "카테고리 목록 조회", description = "전체 카테고리 목록을 계층 구조로 조회합니다.")
//    @GetMapping
//    public ResponseEntity<GlobalResponse> getCategories() {
//        List<CategoryCreateResponse> response = categoryService.getAllCategories();
//        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.CATEGORY_READ_SUCCESS, response));
//    }
//
//    @Operation(summary = "특정 카테고리 조회", description = "카테고리 ID를 통해 특정 카테고리의 상세 정보를 조회합니다.")
//    @GetMapping("/{categoryId}")
//    public ResponseEntity<GlobalResponse> getCategory(@PathVariable Long categoryId) {
//        CategoryCreateResponse response = categoryService.getCategory(categoryId);
//        return ResponseEntity.ok(GlobalResponse.success(SuccessMessage.CATEGORY_READ_SUCCESS, response));
//    }



}
