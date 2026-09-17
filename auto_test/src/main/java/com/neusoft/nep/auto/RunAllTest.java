package com.neusoft.nep.auto;

/**
 * 一键运行全部自动化用例（按 README 建议的执行顺序）。
 *
 * 依次执行 5 个用例：
 *   1. LoginTest              用户登录
 *   2. SupervisorFeedbackTest 监督员注册 + 提交反馈
 *   3. FeedbackAssignTest     管理员查询 / 指派 / 删除
 *   4. GridMeasureTest        端到端链路（反馈 → 指派 → 实测 → 确认 AQI）
 *   5. HrGridTest             网格员管理 + 请假审批 + 状态联动
 *
 * 每个用例各自打开/关闭浏览器（main 内独立管理），运行失败不影响后续用例。
 * 命令行：mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.RunAllTest
 */
public class RunAllTest {

    public static void main(String[] args) {
        String[] titles = {
                "用户登录自动化测试",
                "公众监督员注册与反馈提交自动化测试",
                "管理员监督数据查询/指派/删除自动化测试",
                "端到端业务链路自动化测试（反馈→指派→实测→确认）",
                "网格员管理与请假审批自动化测试"
        };
        String[] emptyArgs = new String[0];
        Runnable[] suites = {
                () -> invoke(() -> LoginTest.main(emptyArgs)),
                () -> invoke(() -> SupervisorFeedbackTest.main(emptyArgs)),
                () -> invoke(() -> FeedbackAssignTest.main(emptyArgs)),
                () -> invoke(() -> GridMeasureTest.main(emptyArgs)),
                () -> invoke(() -> HrGridTest.main(emptyArgs))
        };

        long totalFail = 0;
        System.out.println("================== 自动化测试套件开始（共 " + suites.length + " 个用例） ==================");
        for (int i = 0; i < suites.length; i++) {
            System.out.println("\n>>>>>> 用例 " + (i + 1) + "/" + suites.length + "：" + titles[i] + " <<<<<<");
            try {
                suites[i].run();
            } catch (Throwable t) {
                totalFail++;
                System.out.println("  [用例异常] " + t);
            }
            totalFail += BaseTest.failCount;
        }
        System.out.println("\n================== 自动化测试套件结束 ==================");
        System.out.println("全部用例累计失败项：" + totalFail);
    }

    /** 允许执行会抛出受检异常的任务（Runnable 本身不能抛受检异常） */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** 捕获受检异常，转为运行时异常，避免 lambda 与 Runnable 签名冲突 */
    private static void invoke(ThrowingRunnable action) {
        try {
            action.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
