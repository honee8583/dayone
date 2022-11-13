package com.dayone.sample.persist;

import com.dayone.sample.persist.entity.DividendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DividendRepository extends JpaRepository<DividendEntity, Long> {
    List<DividendEntity> findAllByCompanyId(long companyId);

    @Transactional
    void deleteAllByCompanyId(long companyId);

    boolean existsByCompanyIdAndDate(Long companyId, LocalDateTime date);

}
