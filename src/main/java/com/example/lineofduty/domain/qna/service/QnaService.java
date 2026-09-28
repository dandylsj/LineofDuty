package com.example.lineofduty.domain.qna.service;

import com.example.lineofduty.common.exception.CustomException;
import com.example.lineofduty.common.exception.ErrorMessage;
import com.example.lineofduty.domain.qna.Qna;
import com.example.lineofduty.domain.qna.QnaDto;
import com.example.lineofduty.domain.qna.QnaStatus;
import com.example.lineofduty.domain.qna.repository.QnaRepository;
import com.example.lineofduty.domain.qna.dto.request.QnaResisterRequest;
import com.example.lineofduty.domain.qna.dto.request.QnaUpdateRequest;
import com.example.lineofduty.domain.qna.dto.response.QnaInquiryListResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaInquiryResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaResisterResponse;
import com.example.lineofduty.domain.qna.dto.response.QnaUpdateResponse;
import com.example.lineofduty.domain.user.dto.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final ProfanityFilterService profanityFilterService;
    private final RateLimitService rateLimitService;

    // 질문 등록
    @Transactional
    public QnaResisterResponse qnaRegistration(UserDetail userDetails, QnaResisterRequest request) {

        if (userDetails.getUser().isDeleted()) {
            throw new CustomException(ErrorMessage.USER_DELETED_NOT_FOUND);
        }

        Qna qna = new Qna(request.getTitle(), request.getQuestionContent(), userDetails.getUser());

        profanityFilterService.validateNoProfanity(qna.getTitle());

        profanityFilterService.validateNoProfanity(qna.getQuestionContent());

        rateLimitService.checkPostLimit(userDetails.getUser().getId().toString());

        qnaRepository.save(qna);

        return QnaResisterResponse.from(qna);
    }
    //질문 단건 조회
    @Transactional
    public QnaInquiryResponse qnaInquiry(Long qnaId) {

        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorMessage.QUESTION_NOT_FOUND));

        qna.increaseViewCount();

        return QnaInquiryResponse.from(qna);
    }

    // 질문 단건 조회 (비관적 락)
    @Transactional
    public QnaInquiryResponse qnaInquiryWithPessimisticLock(Long qnaId) {
        Qna qna = qnaRepository.findByIdWithPessimisticLock(qnaId)
                .orElseThrow(() -> new CustomException(ErrorMessage.QUESTION_NOT_FOUND));

        qna.increaseViewCount();

        return QnaInquiryResponse.from(qna);
    }

    // 질문 단건 조회 (낙관적 락)
    @Transactional
    public QnaInquiryResponse qnaInquiryWithOptimisticLock(Long qnaId) {
        Qna qna = qnaRepository.findByIdWithOptimisticLock(qnaId)
                .orElseThrow(() -> new CustomException(ErrorMessage.QUESTION_NOT_FOUND));

        qna.increaseViewCount();

        return QnaInquiryResponse.from(qna);
    }

    // 질문 단건 조회 (원자적 UPDATE) - 실제 API가 쓰는 방식
    // 조회수 +1을 "UPDATE ... SET view_count = view_count + 1" 한 문장으로 처리한다. 락을 잡지 않아서 동시 조회가
    // 서로를 기다리지 않고(비관적 락 대비), 충돌로 실패하는 요청도 없다(낙관적 락 대비). 성능 비교는
    // performance/QnaViewCountPerformanceTest 참고.
    @Transactional
    public QnaInquiryResponse qnaInquiryWithAtomicUpdate(Long qnaId) {
        if (qnaRepository.increaseViewCount(qnaId) == 0) {
            throw new CustomException(ErrorMessage.QUESTION_NOT_FOUND);
        }

        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorMessage.QUESTION_NOT_FOUND));

        return QnaInquiryResponse.from(qna);
    }

    //질문 목록 조회
    @Transactional(readOnly = true)
    public QnaInquiryListResponse qnaInquiryListResponse(int page, int size, String sort, String keyword) {

        String sortProperty = "createdAt";
        Sort.Direction sortDirection = Sort.Direction.DESC;

        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            sortProperty = sortParams[0];
            if (sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1])) {
                sortDirection = Sort.Direction.ASC;
            }
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Page<Qna> qnaPage;

        if (keyword == null || keyword.trim().isEmpty()) {
            qnaPage = qnaRepository.findAll(pageable);
        } else {
            qnaPage = qnaRepository.searchByKeyword(keyword, pageable);
        }

        Page<QnaDto> qnaDtoPage = qnaPage.map(QnaDto::from);

        return QnaInquiryListResponse.from(qnaDtoPage);
    }

    //질문 수정
    @Transactional
    public QnaUpdateResponse qnaUpdate(UserDetail userDetails, Long qnaId, QnaUpdateRequest request) {

        if (userDetails.getUser().isDeleted()) {
            throw new CustomException(ErrorMessage.USER_DELETED_NOT_FOUND);
        }

        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(()-> new CustomException(ErrorMessage.QUESTION_NOT_FOUND));

        if (!userDetails.getUser().getId().equals(qna.getUser().getId())) {
            throw new CustomException(ErrorMessage.NO_MODIFY_PERMISSION);
        }

        if (qna.getStatus() == QnaStatus.RESOLVED) {
            throw new CustomException(ErrorMessage.ALREADY_ANSWERED_CANNOT_MODIFY);
        }

        profanityFilterService.validateNoProfanity(qna.getTitle());

        profanityFilterService.validateNoProfanity(qna.getQuestionContent());

        qna.update(request.getTitle(), request.getQuestionContent());

        return QnaUpdateResponse.from(qna);
    }

    //질문 삭제
    @Transactional
    public void qnaDelete(UserDetail userDetails, Long qnaId) {

        if (userDetails.getUser().isDeleted()) {
            throw new CustomException(ErrorMessage.USER_DELETED_NOT_FOUND);
        }

        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(()-> new CustomException(ErrorMessage.QUESTION_NOT_FOUND));

        if (!userDetails.getUser().getId().equals(qna.getUser().getId())) {
            throw new CustomException(ErrorMessage.NO_MODIFY_PERMISSION);
        }

        qnaRepository.delete(qna);

    }

}
