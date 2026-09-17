package com.example.demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 业务规则配置。
 *
 * 独立成 bean 而不是把 @Value 字段直接放进服务实现类：服务实现类使用 Lombok
 * {@code @AllArgsConstructor}，任何实例字段都会成为构造参数，@Value 字段会被
 * Spring 当成“需要一个 long bean”而导致启动失败。
 */
@Component
@Getter
public class NepTaskProperties {

    /** 已指派任务在该小时数内未提交实测数据，则自动回收重新进入“待指派”池 */
    @Value("${nep.task.repool-hours:24}")
    private long repoolHours = 24;

    /**
     * 单个网格员可同时承担的在办任务数上限（用于决策看板推算“是否需要增员”）。
     * 当待指派任务数超过「空闲网格员数 × 该上限」时，说明人力不足。
     */
    @Value("${nep.task.worker-capacity:3}")
    private int workerCapacity = 3;
}
