package com.Minyou.MINYOU.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseTimeEntity {

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}