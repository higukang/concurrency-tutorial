package kr.higu.clickcountersystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "counter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Counter {

    @Id
    private Long id;

    private Long clickCount;

    public Counter(Long id, Long clickCount) {
        this.id = id;
        this.clickCount = clickCount;
    }

    public void increase() {
        this.clickCount += 1;
    }
}