package com.example.betandcoin.client.renderer;


import com.example.betandcoin.client.model.CasinoFarkleTableModel;
import com.example.betandcoin.tileentity.CasinoFarkleTableTileEntity;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;



public class CasinoFarkleTableRenderer
        extends GeoBlockRenderer<CasinoFarkleTableTileEntity> {


    public CasinoFarkleTableRenderer(
            TileEntityRendererDispatcher dispatcher
    ) {

        super(
                dispatcher,
                new CasinoFarkleTableModel()
        );

    }

}