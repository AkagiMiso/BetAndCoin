package com.example.betandcoin.block;

import com.example.betandcoin.registry.ModTileEntities;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;


public class CasinoFarkleTableBlock extends Block {

    public CasinoFarkleTableBlock(Properties properties) {
        super(properties);
    }


    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }


    @Nullable
    @Override
    public TileEntity createTileEntity(
            BlockState state,
            IBlockReader world
    ) {
        return new CasinoFarkleTableTileEntity();
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
}