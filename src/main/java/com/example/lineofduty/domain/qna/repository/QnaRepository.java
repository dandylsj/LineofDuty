package com.example.lineofduty.domain.qna.repository;

import com.example.lineofduty.domain.qna.Qna;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {

     Optional<Qna> findById(Long qnaId);

    @Query("SELECT q FROM Qna q WHERE " +
            "(LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(q.questionContent) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Qna> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Qna q where q.id = :id")
    Optional<Qna> findByIdWithPessimisticLock(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select q from Qna q where q.id = :id")
    Optional<Qna> findByIdWithOptimisticLock(@Param("id") Long id);

    /**
     * 조회수 +1을 DB에서 한 문장으로 처리한다(읽고-더하고-쓰기를 애플리케이션에서 하지 않음).
     * 락 없이도 동시 요청이 유실되지 않고, 충돌로 실패하는 요청도 없다. 반환값이 0이면 없는 글.
     */
    @Modifying
    @Query("update Qna q set q.viewCount = q.viewCount + 1 where q.id = :id")
    int increaseViewCount(@Param("id") Long id);
}
