package com.example.demo.task;

import com.example.demo.service.ITaskService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 任务超时回收定时器。
 *
 * 业务规则：反馈被指派给网格员后，若在 nep.task.repool-hours（默认 24 小时）内
 * 仍未提交实测数据，视为“一直未接单”，自动回收重新进入“待指派”池，
 * 让管理员可以改派其他网格员。
 *
 * 扫描间隔由 nep.task.repool-scan-interval 控制（默认 5 分钟）。
 */
@Component
@AllArgsConstructor
@Slf4j
public class TaskRepoolScheduler {

    private final ITaskService taskService;

    @Scheduled(initialDelayString = "${nep.task.repool-scan-interval:300000}",
            fixedDelayString = "${nep.task.repool-scan-interval:300000}")
    public void repoolTimedOutTasks() {
        try {
            int n = taskService.repoolTimedOutTasks();
            if (n > 0) {
                log.info("定时扫描：已回收 {} 条超时未接单的任务", n);
            }
        } catch (Exception e) {
            log.error("定时扫描超时任务失败", e);
        }
    }
}
