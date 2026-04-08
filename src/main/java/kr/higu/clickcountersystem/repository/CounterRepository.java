package kr.higu.clickcountersystem.repository;

import kr.higu.clickcountersystem.entity.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    // step3
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update Counter c " +
            "set c.clickCount = c.clickCount + 1 " +
            "where c.id = 1"
    )
    int increaseAtomic();
}
