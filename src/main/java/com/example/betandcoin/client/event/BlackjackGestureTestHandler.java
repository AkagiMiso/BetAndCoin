package com.example.betandcoin.client.event;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.client.BlackjackClientSession;
import com.example.betandcoin.network.BlackjackGesturePacket;
import com.example.betandcoin.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blackjack 点头与摇头手势的合并测试器。
 *
 * 点头：
 * 中心 -> 上 -> 下 -> 再次向上
 *
 * 摇头：
 * 中心 -> 任意一侧 -> 相反一侧 -> 返回第一侧
 *
 * 哪个方向先达到启动阈值，就锁定识别该手势。
 * 暂时不连接牌桌、音效、网络包或实际游戏逻辑。
 */
@Mod.EventBusSubscriber(
        modid = BetAndCoin.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class BlackjackGestureTestHandler {

    private static final float VERTICAL_THRESHOLD = 18.0F;
    private static final float HORIZONTAL_THRESHOLD = 24.0F;

    private static final int MAX_GESTURE_TICKS = 36;
    private static final int SUCCESS_MESSAGE_TICKS = 30;
    private static final int COOLDOWN_TICKS = 20;

    private static GestureType activeGesture =
            GestureType.NONE;

    private static NodStage nodStage =
            NodStage.IDLE;

    private static ShakeStage shakeStage =
            ShakeStage.IDLE;

    private static float basePitch;
    private static float baseYaw;

    private static int firstSideDirection;
    private static int gestureTicks;
    private static int successMessageTicks;
    private static int cooldownTicks;


    private BlackjackGestureTestHandler() {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        resetAll();
    }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.RespawnEvent event) {
        resetAll();
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;

        if (player == null || minecraft.level == null) {
            resetAll();
            return;
        }

        if (minecraft.screen != null) {
            resetGesture(
                    player.xRot,
                    player.yRot
            );
            return;
        }

        float currentPitch = player.xRot;
        float currentYaw = player.yRot;

        /*
         * 只有服务器已经同步了有效牌桌会话时，
         * 才运行手势识别。
         */
        if (!BlackjackClientSession.isActive()) {
            resetGesture(
                    currentPitch,
                    currentYaw
            );
            return;
        }

        if (successMessageTicks > 0) {
            successMessageTicks--;

            if (successMessageTicks == 0) {
                resetGesture(
                        currentPitch,
                        currentYaw
                );
            }
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        /*
         * 尚未锁定手势时，让基准视角缓慢跟随玩家。
         * 这样普通、缓慢的观察动作不容易触发识别。
         */
        if (activeGesture == GestureType.NONE) {
            basePitch +=
                    (currentPitch - basePitch) * 0.12F;

            float yawDifference =
                    MathHelper.wrapDegrees(
                            currentYaw - baseYaw
                    );

            baseYaw = MathHelper.wrapDegrees(
                    baseYaw + yawDifference * 0.12F
            );
        }

        float pitchOffset =
                currentPitch - basePitch;

        float yawOffset =
                MathHelper.wrapDegrees(
                        currentYaw - baseYaw
                );

        /*
         * 尚未锁定手势时，比较两个方向相对于阈值的强度。
         * 哪个方向更明显，就优先锁定哪个手势。
         */
        if (activeGesture == GestureType.NONE) {
            float verticalStrength =
                    Math.abs(pitchOffset)
                            / VERTICAL_THRESHOLD;

            float horizontalStrength =
                    Math.abs(yawOffset)
                            / HORIZONTAL_THRESHOLD;

            if (verticalStrength >= 1.0F
                    || horizontalStrength >= 1.0F) {

                if (verticalStrength
                        >= horizontalStrength) {

                    /*
                     * 点头必须先向上。
                     * 单纯向下看不会立即开始点头测试。
                     */
                    if (pitchOffset
                            <= -VERTICAL_THRESHOLD) {
                        activeGesture =
                                GestureType.NOD;

                        nodStage =
                                NodStage.UP_1;

                        gestureTicks = 0;
                    }
                } else {
                    activeGesture =
                            GestureType.SHAKE;

                    firstSideDirection =
                            yawOffset < 0.0F
                                    ? -1
                                    : 1;

                    shakeStage =
                            ShakeStage.FIRST_SIDE;

                    gestureTicks = 0;
                }
            }
        }

        if (activeGesture == GestureType.NOD) {
            updateNod(
                    pitchOffset,
                    currentPitch,
                    currentYaw
            );
        } else if (
                activeGesture == GestureType.SHAKE
        ) {
            updateShake(
                    yawOffset,
                    currentPitch,
                    currentYaw
            );
        }
    }

    private static void updateNod(
            float pitchOffset,
            float currentPitch,
            float currentYaw
    ) {
        gestureTicks++;

        switch (nodStage) {
            case UP_1:
                if (pitchOffset
                        >= VERTICAL_THRESHOLD) {
                    nodStage = NodStage.DOWN;
                }
                break;

            case DOWN:
                if (pitchOffset
                        <= -VERTICAL_THRESHOLD) {
                    completeGesture();
                }
                break;

            case IDLE:
            case SUCCESS:
                break;
        }

        if (gestureTicks > MAX_GESTURE_TICKS) {
            resetGesture(
                    currentPitch,
                    currentYaw
            );
        }
    }

    private static void updateShake(
            float yawOffset,
            float currentPitch,
            float currentYaw
    ) {
        gestureTicks++;

        switch (shakeStage) {
            case FIRST_SIDE:
                if (yawOffset
                        * firstSideDirection
                        <= -HORIZONTAL_THRESHOLD) {

                    shakeStage =
                            ShakeStage.OPPOSITE_SIDE;
                }
                break;

            case OPPOSITE_SIDE:
                if (yawOffset
                        * firstSideDirection
                        >= HORIZONTAL_THRESHOLD) {

                    completeGesture();
                }
                break;

            case IDLE:
            case SUCCESS:
                break;
        }

        if (gestureTicks > MAX_GESTURE_TICKS) {
            resetGesture(
                    currentPitch,
                    currentYaw
            );
        }
    }

    private static void completeGesture() {
        successMessageTicks =
                SUCCESS_MESSAGE_TICKS;
        cooldownTicks =
                COOLDOWN_TICKS;

        if (activeGesture
                == GestureType.NOD) {
            nodStage = NodStage.SUCCESS;

            ModNetwork.CHANNEL.sendToServer(
                    new BlackjackGesturePacket(
                            BlackjackClientSession
                                    .getTablePos(),
                            BlackjackClientSession
                                    .getSlotIndex(),
                            BlackjackGesturePacket
                                    .Gesture
                                    .NOD
                    )
            );
        } else if (
                activeGesture
                        == GestureType.SHAKE
        ) {
            shakeStage = ShakeStage.SUCCESS;

            ModNetwork.CHANNEL.sendToServer(
                    new BlackjackGesturePacket(
                            BlackjackClientSession
                                    .getTablePos(),
                            BlackjackClientSession
                                    .getSlotIndex(),
                            BlackjackGesturePacket
                                    .Gesture
                                    .SHAKE
                    )
            );
        }
    }

    private static void resetGesture(
            float currentPitch,
            float currentYaw
    ) {
        activeGesture =
                GestureType.NONE;

        nodStage =
                NodStage.IDLE;

        shakeStage =
                ShakeStage.IDLE;

        basePitch = currentPitch;
        baseYaw = currentYaw;

        firstSideDirection = 0;
        gestureTicks = 0;
    }

    private static void resetAll() {
        BlackjackClientSession.clear();
        activeGesture =
                GestureType.NONE;

        nodStage =
                NodStage.IDLE;

        shakeStage =
                ShakeStage.IDLE;

        basePitch = 0.0F;
        baseYaw = 0.0F;

        firstSideDirection = 0;
        gestureTicks = 0;
        successMessageTicks = 0;
        cooldownTicks = 0;
    }

    private enum GestureType {
        NONE,
        NOD,
        SHAKE
    }

    private enum NodStage {
        IDLE,
        UP_1,
        DOWN,
        SUCCESS
    }

    private enum ShakeStage {
        IDLE,
        FIRST_SIDE,
        OPPOSITE_SIDE,
        SUCCESS
    }
}
