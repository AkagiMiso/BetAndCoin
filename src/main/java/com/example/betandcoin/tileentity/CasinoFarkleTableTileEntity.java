package com.example.betandcoin.tileentity;


import com.example.betandcoin.registry.ModTileEntities;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;


public class CasinoFarkleTableTileEntity
        extends TileEntity
        implements IAnimatable, ITickableTileEntity {


    private final AnimationFactory factory =
            new AnimationFactory(this);


    /**
     * 游戏状态
     *
     * 0 = 未开始
     * 1 = 玩家A回合
     * 2 = 玩家B回合
     */
    private int gameState = 0;



    public CasinoFarkleTableTileEntity() {

        super(
                ModTileEntities.CASINO_FARKLE_TABLE.get()
        );

    }



    public int getGameState() {

        return gameState;

    }



    public void setGameState(int state) {

        this.gameState = state;

        setChanged();

    }



    @Override
    public void tick() {

        // 后续加入：
        // 回合检测
        // 动画状态同步
        // 骰子状态更新

    }



    @Override
    public CompoundNBT save(
            CompoundNBT tag
    ) {

        tag.putInt(
                "GameState",
                gameState
        );

        return super.save(tag);

    }



    @Override
    public void load(
            BlockState state,
            CompoundNBT tag
    ) {

        super.load(
                state,
                tag
        );


        gameState =
                tag.getInt(
                        "GameState"
                );

    }



    @Override
    public void registerControllers(
            AnimationData data
    ) {

        data.addAnimationController(
                new AnimationController<>(
                        this,
                        "controller",
                        0,
                        event -> PlayState.CONTINUE
                )
        );

    }



    @Override
    public AnimationFactory getFactory() {

        return factory;

    }

}