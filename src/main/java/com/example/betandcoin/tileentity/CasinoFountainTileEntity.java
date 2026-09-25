package com.example.betandcoin.tileentity;

import com.example.betandcoin.BetAndCoin;
import com.example.betandcoin.registry.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

/**
 * Casino Fountain：
 * - 持续播放纸牌鸟/顶端悬浮方块待机动画；
 * - 服务端检测下层许愿池中的筹码 ItemEntity；
 * - 整个 ItemEntity 无论数量多少都只算一次许愿并全部消耗；
 * - 10% 概率吐出 2 个同面额筹码。
 */
public class CasinoFountainTileEntity
        extends TileEntity
        implements IAnimatable, ITickableTileEntity {

    private static final String ANIMATION_CONTROLLER =
            "casino_fountain_controller";

    private static final String ANIMATION_IDLE =
            "card_bird_fly";

    private static final ResourceLocation CHIP_TAG_ID =
            new ResourceLocation(
                    BetAndCoin.MOD_ID,
                    "chips"
            );

    /**
     * 下层水面位于模型 Y = 5.75/16 ≈ 0.36 格。
     * 检测框覆盖水面附近并避开外围占位区域。
     */
    private static final double WISH_HALF_WIDTH = 1.45D;
    private static final double WISH_MIN_Y = 0.15D;
    private static final double WISH_MAX_Y = 0.70D;

    private static final int WISH_CHECK_INTERVAL = 2;
    private static final float WIN_CHANCE = 0.10F;

    private static final String REWARD_MARKER =
            "BetAndCoinCasinoFountainReward";

    private final AnimationFactory animationFactory =
            new AnimationFactory(this);

    private final Random random = new Random();
    private int wishCheckTicker = 0;

    public CasinoFountainTileEntity() {
        this(ModTileEntities.CASINO_FOUNTAIN.get());
    }

    public CasinoFountainTileEntity(
            @Nonnull TileEntityType<?> tileEntityType
    ) {
        super(tileEntityType);
    }

    /**
     * TileEntity 默认只按主方块自身约 1x1x1 的范围做视锥剔除。
     *
     * 但 Casino Fountain 的实际模型、纸牌鸟飞行轨迹和水面都远远
     * 超出主方块，因此必须扩大 TESR 的可见包围盒。
     *
     * 这里覆盖整个喷泉和纸牌鸟动画可能到达的区域。
     * 这样即使中心主方块本身离开屏幕，只要喷泉任意可见部分仍在
     * 视锥内，Renderer 就不会把整座喷泉一起剔除。
     */
    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                this.worldPosition.getX() - 4.5D,
                this.worldPosition.getY() - 0.5D,
                this.worldPosition.getZ() - 4.5D,
                this.worldPosition.getX() + 5.5D,
                this.worldPosition.getY() + 7.5D,
                this.worldPosition.getZ() + 5.5D
        );
    }

    @Override
    public void tick() {
        if (this.level == null
                || this.level.isClientSide) {
            return;
        }

        this.wishCheckTicker++;

        if (this.wishCheckTicker < WISH_CHECK_INTERVAL) {
            return;
        }

        this.wishCheckTicker = 0;
        checkWishItems();
    }

    private void checkWishItems() {
        if (!(this.level instanceof ServerWorld)) {
            return;
        }

        double centerX = this.worldPosition.getX() + 0.5D;
        double centerZ = this.worldPosition.getZ() + 0.5D;
        double baseY = this.worldPosition.getY();

        AxisAlignedBB wishArea =
                new AxisAlignedBB(
                        centerX - WISH_HALF_WIDTH,
                        baseY + WISH_MIN_Y,
                        centerZ - WISH_HALF_WIDTH,
                        centerX + WISH_HALF_WIDTH,
                        baseY + WISH_MAX_Y,
                        centerZ + WISH_HALF_WIDTH
                );

        List<ItemEntity> itemEntities =
                this.level.getEntitiesOfClass(
                        ItemEntity.class,
                        wishArea,
                        entity ->
                                entity.isAlive()
                                        && !entity.getPersistentData()
                                                .getBoolean(REWARD_MARKER)
                                        && isValidWishChip(
                                                entity.getItem()
                                        )
                );

        if (itemEntities.isEmpty()) {
            return;
        }

        /*
         * 每个 ItemEntity 都视为一次许愿。
         * 一叠 64 个筹码与单个筹码完全等价：整叠消耗，只判定一次。
         */
        for (ItemEntity itemEntity : itemEntities) {
            if (!itemEntity.isAlive()) {
                continue;
            }

            ItemStack offeredStack =
                    itemEntity.getItem().copy();

            if (!isValidWishChip(offeredStack)) {
                continue;
            }

            Item offeredItem = offeredStack.getItem();

            itemEntity.remove();

            if (this.random.nextFloat() < WIN_CHANCE) {
                spawnWinningPayout(offeredItem);
            }
        }
    }

    private boolean isValidWishChip(
            @Nonnull ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return false;
        }

        ITag<Item> chipTag =
                ItemTags.getAllTags().getTag(CHIP_TAG_ID);

        return chipTag != null
                && chipTag.contains(stack.getItem());
    }

    private void spawnWinningPayout(
            @Nonnull Item chipItem
    ) {
        if (!(this.level instanceof ServerWorld)) {
            return;
        }

        ServerWorld serverWorld =
                (ServerWorld) this.level;

        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 0.31D;
        double z = this.worldPosition.getZ() + 0.5D;

        ItemStack payoutStack =
                new ItemStack(chipItem, 2);

        ItemEntity payout =
                new ItemEntity(
                        serverWorld,
                        x,
                        y,
                        z,
                        payoutStack
                );

        /*
         * 标记为喷泉奖励，避免刚生成的两个筹码再次被许愿检测器吞掉。
         */
        payout.getPersistentData()
                .putBoolean(REWARD_MARKER, true);

        double horizontalX =
                (this.random.nextDouble() - 0.5D) * 0.08D;

        double horizontalZ =
                (this.random.nextDouble() - 0.5D) * 0.08D;

        payout.setDeltaMovement(
                horizontalX,
                0.38D,
                horizontalZ
        );

        payout.setPickUpDelay(10);
        serverWorld.addFreshEntity(payout);

        serverWorld.sendParticles(
                ParticleTypes.SPLASH,
                x,
                this.worldPosition.getY() + 0.40D,
                z,
                12,
                0.28D,
                0.05D,
                0.28D,
                0.05D
        );

        serverWorld.sendParticles(
                ParticleTypes.BUBBLE,
                x,
                this.worldPosition.getY() + 0.32D,
                z,
                8,
                0.22D,
                0.06D,
                0.22D,
                0.02D
        );

        serverWorld.playSound(
                null,
                this.worldPosition,
                SoundEvents.GENERIC_SPLASH,
                SoundCategory.BLOCKS,
                0.75F,
                1.15F
        );
    }

    private <E extends IAnimatable> PlayState animationPredicate(
            @Nonnull AnimationEvent<E> event
    ) {
        event.getController().setAnimation(
                new AnimationBuilder()
                        .addAnimation(
                                ANIMATION_IDLE,
                                true
                        )
        );

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(
            @Nonnull AnimationData animationData
    ) {
        animationData.addAnimationController(
                new AnimationController<>(
                        this,
                        ANIMATION_CONTROLLER,
                        0,
                        this::animationPredicate
                )
        );
    }

    @Nonnull
    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }
}
