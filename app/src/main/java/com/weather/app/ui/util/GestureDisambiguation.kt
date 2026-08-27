package com.weather.app.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 手势滑动方向锁定枚举
 *
 * 用于在多轴向复合滑动场景中（如左右切页与上下滚动卡片）仲裁并锁定当前手势的主方向，
 * 避免双向手势同时触发导致的界面抖动与误触。
 */
enum class ScrollDirectionLock {
    /**
     * 未决状态（刚按下触摸屏幕或位移尚未达到判定门槛）
     */
    UNDETERMINED,

    /**
     * 水平方向锁定（左右滑动手势占主导，例如左右切换城市）
     */
    HORIZONTAL,

    /**
     * 垂直方向锁定（上下滑动手势占主导，例如上下翻阅天气详情卡片或下拉刷新）
     */
    VERTICAL
}

/**
 * 手势方向判定与锁定修饰符
 *
 * 在 [PointerEventPass.Initial] 阶段捕获触摸流并追踪手指位移向量：
 * 当位移超过容错门槛（Touch Slop）时，通过水平与垂直位移的比例倾角快速仲裁出当前手势意图，
 * 并通过 [onDirectionLocked] 回调通知外部容器锁定或挂起对应的滚动方向，在手势抬起时自动复位。
 *
 * @param verticalRatio 判定为垂直滑动所需的垂直分量与水平分量最小比例阈值（默认 0.7f，倾角约 35° 即判为纵向）
 * @param horizontalRatio 判定为水平滑动所需的水平分量与垂直分量最小比例阈值（默认 1.2f）
 * @param onDirectionLocked 手势主方向变更时的回调函数，参数为当前的锁定状态 [ScrollDirectionLock]
 * @return 附加了手势方向仲裁逻辑的 [Modifier]
 */
fun Modifier.directionLockDetector(
    verticalRatio: Float = 0.7f,
    horizontalRatio: Float = 1.2f,
    onDirectionLocked: (ScrollDirectionLock) -> Unit
): Modifier = composed {
    val currentOnDirectionLocked by rememberUpdatedState(onDirectionLocked)
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop

    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                // 等待手指按下
                val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                val firstDown = downEvent.changes.firstOrNull { it.pressed } ?: continue
                val downPosition = firstDown.position
                var currentLock = ScrollDirectionLock.UNDETERMINED
                currentOnDirectionLocked(ScrollDirectionLock.UNDETERMINED)

                // 在手势移动过程中持续追踪位移
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val activeChanges = event.changes.filter { it.pressed }

                    // 如果所有手指均已离开屏幕或被取消，结束本次手势生命周期
                    if (activeChanges.isEmpty()) {
                        if (currentLock != ScrollDirectionLock.UNDETERMINED) {
                            currentOnDirectionLocked(ScrollDirectionLock.UNDETERMINED)
                        }
                        break
                    }

                    // 追踪主触控点的位移向量
                    val primaryChange = activeChanges.firstOrNull() ?: break
                    val currentPos = primaryChange.position
                    val dx = currentPos.x - downPosition.x
                    val dy = currentPos.y - downPosition.y
                    val distance = hypot(dx, dy)

                    // 尚未判定方向时，进行斜率倾角仲裁
                    if (currentLock == ScrollDirectionLock.UNDETERMINED && distance >= touchSlop) {
                        val absDx = abs(dx)
                        val absDy = abs(dy)

                        if (absDy >= absDx * verticalRatio) {
                            // 垂直分量明显占优，锁定为垂直滚动（上下翻阅卡片或下拉刷新）
                            currentLock = ScrollDirectionLock.VERTICAL
                            currentOnDirectionLocked(ScrollDirectionLock.VERTICAL)
                        } else if (absDx > absDy * horizontalRatio) {
                            // 水平分量明显占优，锁定为水平切页（左右切换城市）
                            currentLock = ScrollDirectionLock.HORIZONTAL
                            currentOnDirectionLocked(ScrollDirectionLock.HORIZONTAL)
                        }
                    }
                }
            }
        }
    }
}
